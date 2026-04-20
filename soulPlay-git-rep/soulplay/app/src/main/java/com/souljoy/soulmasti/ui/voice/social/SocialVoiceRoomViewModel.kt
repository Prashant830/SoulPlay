package com.souljoy.soulmasti.ui.voice.social

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.souljoy.soulmasti.BuildConfig
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.google.firebase.auth.FirebaseAuth
import com.souljoy.soulmasti.domain.gift.GiftCatalog
import com.souljoy.soulmasti.domain.gift.GiftEvent
import com.souljoy.soulmasti.domain.gift.GiftSendContext
import com.souljoy.soulmasti.domain.model.SocialSeatInvite
import com.souljoy.soulmasti.domain.model.SocialVoiceChatMessage
import com.souljoy.soulmasti.domain.model.SocialVoiceRoomSnapshot
import com.souljoy.soulmasti.domain.model.VoiceConnectionState
import com.souljoy.soulmasti.domain.repository.GiftRepository
import com.souljoy.soulmasti.domain.repository.GiftSendResult
import com.souljoy.soulmasti.domain.repository.SocialVoiceRoomRepository
import com.souljoy.soulmasti.domain.repository.VoiceRoomRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SocialVoiceRoomViewModel(
    application: Application,
    private val repository: SocialVoiceRoomRepository,
    private val voiceRoomRepository: VoiceRoomRepository,
    private val database: FirebaseDatabase,
    private val giftRepository: GiftRepository,
    private val roomId: String,
) : AndroidViewModel(application) {
    private val roomSessionStartMs = System.currentTimeMillis()

    private val _room = MutableStateFlow<SocialVoiceRoomSnapshot?>(null)
    val room: StateFlow<SocialVoiceRoomSnapshot?> = _room.asStateFlow()

    private val _messages = MutableStateFlow<List<SocialVoiceChatMessage>>(emptyList())
    val messages: StateFlow<List<SocialVoiceChatMessage>> = _messages.asStateFlow()
    private val _giftEvents = MutableStateFlow<List<GiftEvent>>(emptyList())
    val giftEvents: StateFlow<List<GiftEvent>> = _giftEvents.asStateFlow()
    private val _giftFxEvents = MutableSharedFlow<GiftEvent>(extraBufferCapacity = 32)
    val giftFxEvents: SharedFlow<GiftEvent> = _giftFxEvents.asSharedFlow()
    private val _userProfiles = MutableStateFlow<Map<String, SeatUserProfile>>(emptyMap())
    val userProfiles: StateFlow<Map<String, SeatUserProfile>> = _userProfiles.asStateFlow()
    private val _coinBalance = MutableStateFlow<Long?>(null)
    val coinBalance: StateFlow<Long?> = _coinBalance.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()
    private val handledInviteStatus = mutableSetOf<String>()
    private val _incomingInvite = MutableStateFlow<SocialSeatInvite?>(null)
    val incomingInvite: StateFlow<SocialSeatInvite?> = _incomingInvite.asStateFlow()
    private val _joinTicker = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val joinTicker: SharedFlow<String> = _joinTicker.asSharedFlow()
    private var previousOnlineUids: Set<String> = emptySet()
    private val _chatVisibleAfter = MutableStateFlow<Long?>(null)
    private var forcedByNoSeat = false
    private var forcedBySeatMute = false

    val myUid: String? get() = FirebaseAuth.getInstance().currentUser?.uid
    val currentRoomId: String get() = roomId
    val participants: StateFlow<List<Int>> = voiceRoomRepository.participants
    val connectionState: StateFlow<VoiceConnectionState> = voiceRoomRepository.connectionState
    val isMuted: StateFlow<Boolean> = voiceRoomRepository.isMuted
    val audioLevelsByUid: StateFlow<Map<Int, Float>> = voiceRoomRepository.audioLevelsByUid

    init {
        viewModelScope.launch {
            repository.observeRoom(roomId).collect { snap ->
                _room.value = snap
                val onlineNow = snap?.onlineUids.orEmpty()
                val joined = onlineNow - previousOnlineUids
                previousOnlineUids = onlineNow
                joined.firstOrNull { it != myUid }?.let { joinedUid ->
                    _joinTicker.tryEmit("${displayName(joinedUid)} joined")
                }
                preloadProfiles(
                    (
                        snap?.seats?.mapNotNull { it.occupantUid }.orEmpty() +
                            onlineNow +
                            listOfNotNull(snap?.ownerUid) +
                            snap?.contributionDaily?.keys.orEmpty() +
                            snap?.contributionWeekly?.keys.orEmpty() +
                            snap?.contributionTotal?.keys.orEmpty() +
                            snap?.contributionDailySoul?.keys.orEmpty() +
                            snap?.contributionWeeklySoul?.keys.orEmpty() +
                            snap?.contributionTotalSoul?.keys.orEmpty()
                        ).toSet(),
                )
                val uid = myUid
                if (uid != null) {
                    val mySeat = snap?.seats?.firstOrNull { it.occupantUid == uid }
                    val seatForcedMuted = mySeat?.muted == true
                    val noSeat = mySeat == null

                    if (noSeat && !isMuted.value) {
                        voiceRoomRepository.toggleMute()
                        forcedByNoSeat = true
                    } else if (!noSeat && forcedByNoSeat && isMuted.value && !seatForcedMuted) {
                        voiceRoomRepository.toggleMute()
                        forcedByNoSeat = false
                    }

                    if (seatForcedMuted && !isMuted.value) {
                        voiceRoomRepository.toggleMute()
                        forcedBySeatMute = true
                    } else if (!seatForcedMuted && forcedBySeatMute && isMuted.value && !forcedByNoSeat) {
                        voiceRoomRepository.toggleMute()
                        forcedBySeatMute = false
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.observeMyChatVisibleAfter(roomId).collect { ts ->
                _chatVisibleAfter.value = ts
            }
        }
        viewModelScope.launch {
            repository.observeRoomChat(roomId).collect { list ->
                val visibleAfter = _chatVisibleAfter.value ?: 0L
                _messages.value = if (visibleAfter <= 0L) list else {
                    list.filter { (it.createdAt ?: 0L) >= visibleAfter }
                }
            }
        }
        viewModelScope.launch {
            repository.observeSeatInvites(roomId).collect { invites ->
                val uid = myUid
                if (uid != null) {
                    _incomingInvite.value = invites.firstOrNull {
                        it.toUid == uid && it.status == "pending" && (it.createdAt ?: 0L) >= roomSessionStartMs
                    }
                    val myDeclined = invites.filter { it.fromUid == uid && it.status == "declined" }
                    for (invite in myDeclined) {
                        val key = "${invite.inviteId}:${invite.status}"
                        if (handledInviteStatus.add(key)) {
                            _toastEvents.tryEmit("User refused your invite")
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            giftRepository.observeGiftEvents(GiftSendContext.VoiceRoom(roomId)).collect { event ->
                val createdAt = event.createdAt ?: 0L
                if (createdAt in 1 until roomSessionStartMs) return@collect
                _giftEvents.value = (_giftEvents.value + event)
                    .sortedBy { it.createdAt ?: 0L }
                    .takeLast(100)
                _giftFxEvents.tryEmit(event)
                preloadProfiles(setOfNotNull(event.fromUserId, event.toUserId))
            }
        }
        refreshCoinBalance()
        setPresence(true)
    }

    fun dismissError() {
        _error.value = null
    }

    fun showInfo(message: String) {
        _toastEvents.tryEmit(message)
    }

    fun setPresence(online: Boolean) {
        viewModelScope.launch {
            repository.setRoomPresence(roomId, online).onFailure {
                _error.value = it.message ?: "Presence update failed"
            }
        }
    }

    fun onSeatClick(seatNo: Int, occupiedBySelf: Boolean) {
        viewModelScope.launch {
            val result = if (occupiedBySelf) {
                repository.leaveSeat(roomId, seatNo)
            } else {
                repository.takeSeat(roomId, seatNo)
            }
            result.onFailure { _error.value = it.message ?: "Seat action failed" }
        }
    }

    fun setSeatMuted(seatNo: Int, muted: Boolean) {
        viewModelScope.launch {
            repository.setSeatMuted(roomId, seatNo, muted).onFailure {
                _error.value = it.message ?: "Could not change mute state"
            }
        }
    }

    fun setSeatLocked(seatNo: Int, locked: Boolean) {
        viewModelScope.launch {
            repository.setSeatLocked(roomId, seatNo, locked).onFailure {
                _error.value = it.message ?: "Could not lock seat"
            }
        }
    }

    fun removeSeatOccupant(seatNo: Int) {
        viewModelScope.launch {
            repository.removeSeatOccupant(roomId, seatNo).onFailure {
                _error.value = it.message ?: "Could not remove user"
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch {
            repository.sendRoomChatMessage(roomId, text).onFailure {
                _error.value = it.message ?: "Could not send message"
            }
        }
    }

    suspend fun sendGift(toUid: String, giftId: String, selectedCount: Int): Result<GiftSendResult> {
        if (toUid.isBlank()) return Result.failure(IllegalArgumentException("Invalid recipient"))
        val result = giftRepository.sendGift(
            context = GiftSendContext.VoiceRoom(roomId),
            giftId = giftId,
            recipientUserId = toUid,
            selectedCount = selectedCount,
        )
        result.onSuccess { sent ->
            _coinBalance.value = sent.newBalance
        }
        return result
    }

    fun joinAgoraVoice() {
        if (BuildConfig.AGORA_APP_ID.isBlank()) return
        val uid = myUid ?: return
        val agoraUid = FirebaseUidMapping.agoraUidFromFirebaseUid(uid)
        voiceRoomRepository.join(channelName = "social_$roomId", token = null, uid = agoraUid)
    }

    fun toggleMute() {
        val uid = myUid
        val hasSeat = uid != null && (
            _room.value?.seats?.any { it.occupantUid == uid } == true ||
                _room.value?.ownerUid == uid
            )
        if (!hasSeat) {
            _toastEvents.tryEmit("Take a seat to speak")
            if (!isMuted.value) {
                voiceRoomRepository.toggleMute()
                forcedByNoSeat = true
            }
            return
        }
        voiceRoomRepository.toggleMute()
    }






    fun sendSeatInvite(seatNo: Int, toUid: String) {
        viewModelScope.launch {
            repository.sendSeatInvite(roomId, toUid, seatNo).onFailure {
                _error.value = it.message ?: "Could not send invite"
            }
        }
    }

    fun respondToIncomingInvite(accept: Boolean) {
        val invite = _incomingInvite.value ?: return
        viewModelScope.launch {
            repository.respondSeatInvite(roomId, invite.inviteId, accept).onFailure {
                _error.value = it.message ?: "Could not respond to invite"
            }
            _incomingInvite.value = null
        }
    }

    fun displayName(uid: String): String =
        _userProfiles.value[uid]?.name?.takeIf { it.isNotBlank() } ?: FirebaseUidMapping.shortLabel(uid)

    fun profileImageUrl(uid: String?): String? {
        val safe = uid?.takeIf { it.isNotBlank() } ?: return null
        return _userProfiles.value[safe]?.photoUrl?.takeIf { it.isNotBlank() }
    }

    fun userSoul(uid: String?): Long {
        val safe = uid?.takeIf { it.isNotBlank() } ?: return 0L
        return _userProfiles.value[safe]?.soul ?: 0L
    }

    fun updateRoomName(name: String) {
        viewModelScope.launch {
            repository.updateRoomName(roomId, name).onFailure {
                _error.value = it.message ?: "Could not update room name"
            }.onSuccess {
                _toastEvents.tryEmit("Room name updated")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        setPresence(false)
        voiceRoomRepository.leave()
    }

    fun leaveRoom(onDone: () -> Unit) {
        viewModelScope.launch {
            val uid = myUid
            val mySeat = _room.value?.seats?.firstOrNull { it.occupantUid == uid }
            val isOwner = uid != null && uid == _room.value?.ownerUid
            if (mySeat != null && mySeat.seatNo != 1) {
                repository.leaveSeat(roomId, mySeat.seatNo)
            }
            if (isOwner) {
                repository.collapseRoomIfOwnerLeft(roomId)
            }
            repository.markMyChatCleared(roomId)
            repository.dismissMyPendingSeatInvites(roomId)
            clearLocalRoomData()
            voiceRoomRepository.leave()
            setPresence(false)
            onDone()
        }
    }

    private fun clearLocalRoomData() {
        _messages.value = emptyList()
        _giftEvents.value = emptyList()
        _incomingInvite.value = null
        _room.value = _room.value?.copy(
            seats = _room.value?.seats.orEmpty().map {
                if (it.occupantUid == myUid) it.copy(occupantUid = null, occupantName = null, muted = false) else it
            },
        )
    }

    private fun preloadProfiles(uids: Set<String>) {
        if (uids.isEmpty()) return
        val known = _userProfiles.value
        val missing = uids.filter { it.isNotBlank() && !known.containsKey(it) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val loaded = mutableMapOf<String, SeatUserProfile>()
            for (uid in missing) {
                val snap = runCatching {
                    database.reference.child("users").child(uid).get().await()
                }.getOrNull()
                loaded[uid] = SeatUserProfile(
                    name = snap?.child("username")?.getValue(String::class.java)?.trim().orEmpty(),
                    photoUrl = snap?.child("profilePictureUrl")?.getValue(String::class.java)?.trim().orEmpty(),
                    soul = snap?.child("soul")?.getValue(Long::class.java) ?: 0L,
                )
            }
            _userProfiles.value = known + loaded
        }
    }

    private fun refreshCoinBalance() {
        val uid = myUid ?: return
        viewModelScope.launch {
            val value = runCatching {
                database.reference.child("users").child(uid).child("totalWinnings").get().await()
            }.getOrNull()
            _coinBalance.value = parseLong(value?.value) ?: _coinBalance.value
        }
    }

    fun giftSummaryText(event: GiftEvent): String {
        val giftName = GiftCatalog.displayLabel(event.giftId)
        val sender = displayName(event.fromUserId)
        val target = event.toUserId?.let { displayName(it) }.orEmpty()
        return if (target.isBlank()) {
            "$sender sent $giftName x${event.selectedCount}"
        } else {
            "$sender sent $target $giftName x${event.selectedCount}"
        }
    }

    private fun parseLong(value: Any?): Long? {
        if (value == null) return null
        if (value is String) return value.trim().toLongOrNull()
        if (value is Long) return value
        if (value is Int) return value.toLong()
        if (value is Double) return value.toLong()
        if (value is Float) return value.toLong()
        if (value is Number) return value.toLong()
        (value as? java.lang.Number)?.let { return it.longValue() }
        return null
    }
}

private fun setOfNotNull(vararg ids: String?): Set<String> =
    ids.filterNotNull().filter { it.isNotBlank() }.toSet()

data class SeatUserProfile(
    val name: String,
    val photoUrl: String,
    val soul: Long,
)

