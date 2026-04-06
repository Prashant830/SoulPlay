package com.masti.soulplay.ui.voiceroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.masti.soulplay.BuildConfig
import com.masti.soulplay.data.firebase.FirebaseUidMapping
import com.masti.soulplay.domain.gift.GiftCatalog
import com.masti.soulplay.domain.gift.GiftSendContext
import com.masti.soulplay.domain.model.GameRoomSnapshot
import com.masti.soulplay.domain.model.VoiceConnectionState
import com.masti.soulplay.domain.repository.GameSessionRepository
import com.masti.soulplay.domain.repository.GiftRepository
import com.masti.soulplay.domain.repository.GiftSendResult
import com.masti.soulplay.domain.repository.VoiceRoomRepository
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

    init {
        viewModelScope.launch {
            game.observeRoom(roomId).collect { snap ->
                _roomSnapshot.value = snap
                preloadProfiles(snap?.players?.keys.orEmpty())
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
                        senderDisplay = senderDisplay,
                        giftDisplayName = GiftCatalog.displayLabel(event.giftId),
                        recipientDisplay = recipientDisplay,
                        coins = event.coins,
                        receiverCoins = event.receiverCoins,
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

    suspend fun sendGift(recipientUid: String, giftId: String): Result<GiftSendResult> =
        giftRepository.sendGift(
            GiftSendContext.RajaRaniGame(roomId),
            giftId,
            recipientUid,
        )

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
