package com.souljoy.soulmasti.ui.voiceroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.souljoy.soulmasti.BuildConfig
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.domain.gift.GiftCatalog
import com.souljoy.soulmasti.domain.gift.GiftSendContext
import com.souljoy.soulmasti.domain.model.GameRoomSnapshot
import com.souljoy.soulmasti.domain.model.RoomEmojiEvent
import com.souljoy.soulmasti.domain.model.VoiceConnectionState
import com.souljoy.soulmasti.domain.repository.GameSessionRepository
import com.souljoy.soulmasti.domain.repository.GiftRepository
import com.souljoy.soulmasti.domain.repository.GiftSendResult
import com.souljoy.soulmasti.domain.repository.SocialRepository
import com.souljoy.soulmasti.domain.repository.VoiceRoomRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VoiceRoomViewModel(
    application: Application,
    private val voice: VoiceRoomRepository,
    private val game: GameSessionRepository,
    private val database: FirebaseDatabase,
    private val giftRepository: GiftRepository,
    private val socialRepository: SocialRepository,
    private val roomId: String,
) : AndroidViewModel(application) {

    val displayedRoomId: String get() = roomId

    val participants: StateFlow<List<Int>> = voice.participants
    val connectionState: StateFlow<VoiceConnectionState> = voice.connectionState
    val isMuted: StateFlow<Boolean> = voice.isMuted
    val audioLevelsByUid: StateFlow<Map<Int, Float>> = voice.audioLevelsByUid

    val myFirebaseUid: StateFlow<String?> = game.firebaseUid

    val coinBalance: StateFlow<Long?> = game.observeUserTotalWinnings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _roomSnapshot = MutableStateFlow<GameRoomSnapshot?>(null)
    val roomSnapshot: StateFlow<GameRoomSnapshot?> = _roomSnapshot.asStateFlow()
    private val _userProfiles = MutableStateFlow<Map<String, VoiceUserProfile>>(emptyMap())
    val userProfiles: StateFlow<Map<String, VoiceUserProfile>> = _userProfiles.asStateFlow()

    private val _giftBannerEvents = MutableSharedFlow<GiftBannerUi>(extraBufferCapacity = 32)
    val giftBannerEvents: SharedFlow<GiftBannerUi> = _giftBannerEvents.asSharedFlow()

    private val _roomEmojiEvents = MutableSharedFlow<RoomEmojiEvent>(extraBufferCapacity = 64)
    val roomEmojiEvents: SharedFlow<RoomEmojiEvent> = _roomEmojiEvents.asSharedFlow()

    /** Ignore emoji pushes that predate this screen session (avoids replay floods from RTDB). */
    private val roomEmojiSessionStartMs = System.currentTimeMillis()

    init {
        viewModelScope.launch {
            game.observeRoom(roomId).collect { snap ->
                _roomSnapshot.value = snap
                preloadProfiles(snap?.players?.keys.orEmpty())
            }
        }
        viewModelScope.launch {
            game.observeRoomEmojiEvents(roomId).collect { event ->
                if (event.sentAt < roomEmojiSessionStartMs - 5_000L) return@collect
                _roomEmojiEvents.emit(event)
            }
        }
        viewModelScope.launch {
            giftRepository.observeGiftEvents(GiftSendContext.RajaRaniGame(roomId)).collect { event ->
                preloadProfiles(setOfNotNull(event.fromUserId, event.toUserId))
                val senderDisplay = profileDisplayName(event.fromUserId)
                val recipientDisplay = event.toUserId?.let { profileDisplayName(it) }
                _giftBannerEvents.emit(
                    GiftBannerUi(
                        eventId = event.eventId,
                        giftId = event.giftId,
                        senderDisplay = senderDisplay,
                        isSentByMe = event.fromUserId == game.firebaseUid.value,
                        giftDisplayName = GiftCatalog.displayLabel(event.giftId),
                        selectedCount = event.selectedCount,
                        recipientDisplay = recipientDisplay,
                        coins = event.coins,
                        receiverCoins = event.receiverCoins,
                        receiverSoul = event.receiverSoul,
                    )
                )
            }
        }
    }

    private fun profileDisplayName(uid: String): String =
        _userProfiles.value[uid]?.username?.takeIf { it.isNotBlank() }
            ?: FirebaseUidMapping.shortLabel(uid)

    private suspend fun preloadProfiles(uids: Set<String>) {
        if (uids.isEmpty()) return
        val known = _userProfiles.value
        val missing = uids.filterNot { known.containsKey(it) }
        if (missing.isEmpty()) return

        val loaded = mutableMapOf<String, VoiceUserProfile>()
        for (uid in missing) {
            val profile = runCatching {
                val userSnap = database.reference.child("users").child(uid).get().await()
                VoiceUserProfile(
                    username = userSnap.child("username").getValue(String::class.java)?.trim().orEmpty(),
                    profilePictureUrl = userSnap.child("profilePictureUrl").getValue(String::class.java)?.trim().orEmpty(),
                )
            }.getOrElse {
                VoiceUserProfile(username = "", profilePictureUrl = "")
            }
            loaded[uid] = profile
        }
        _userProfiles.value = known + loaded
    }

    suspend fun sendGift(recipientUid: String, giftId: String, selectedCount: Int): Result<GiftSendResult> {
        return giftRepository.sendGift(
            GiftSendContext.RajaRaniGame(roomId),
            giftId,
            recipientUid,
            selectedCount = selectedCount,
        )
    }

    fun sendFriendRequest(toUid: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = try {
                socialRepository.sendFriendRequest(toUid)
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: "Could not send request", e))
            }
            onResult(result)
        }
    }

    fun joinVoiceChannel() {
        if (BuildConfig.AGORA_APP_ID.isBlank()) return
        val fbUid = game.firebaseUid.value ?: return
        val agoraUid = FirebaseUidMapping.agoraUidFromFirebaseUid(fbUid)
        voice.join(roomId, token = null, uid = agoraUid)
    }

    fun submitMantriGuess(suspectFirebaseUid: String) {
        viewModelScope.launch {
            game.setSelectedSuspect(roomId, suspectFirebaseUid)
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            voice.toggleMute()
        }
    }

    fun sendRoomEmoji(emoji: String) {
        viewModelScope.launch {
            runCatching { game.sendRoomEmoji(roomId, emoji) }
        }
    }

    fun leaveRoom(onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            game.setLocalPlayerOnline(false)
            voice.leave()
            onFinished()
        }
    }

    override fun onCleared() {
        super.onCleared()
        voice.release()
    }
}

data class VoiceUserProfile(
    val username: String,
    val profilePictureUrl: String,
)

private fun setOfNotNull(vararg ids: String?): Set<String> =
    ids.filterNotNull().filter { it.isNotBlank() }.toSet()
