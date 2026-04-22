package com.souljoy.soulmasti.ui.voice.social

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.domain.model.SocialVoiceRoomSnapshot
import com.souljoy.soulmasti.domain.model.VoiceEntitlementState
import com.souljoy.soulmasti.domain.repository.SocialRepository
import com.souljoy.soulmasti.domain.repository.SocialVoiceRoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SocialVoiceRoomsViewModel(
    application: Application,
    private val socialRepository: SocialRepository,
    private val socialVoiceRoomRepository: SocialVoiceRoomRepository,
    private val database: FirebaseDatabase,
) : AndroidViewModel(application) {

    val friends: StateFlow<Set<String>> = socialRepository.friends
    val entitlement: StateFlow<VoiceEntitlementState> =
        socialVoiceRoomRepository.observeMyEntitlement().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            VoiceEntitlementState(false, null),
        )

    private val _myRoom = MutableStateFlow<SocialVoiceRoomSnapshot?>(null)
    val myRoom: StateFlow<SocialVoiceRoomSnapshot?> = _myRoom.asStateFlow()

    private val _friendRooms = MutableStateFlow<List<FriendRoomCardUi>>(emptyList())
    val friendRooms: StateFlow<List<FriendRoomCardUi>> = _friendRooms.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _joinConfirm = MutableStateFlow<JoinRoomConfirmUi?>(null)
    val joinConfirm: StateFlow<JoinRoomConfirmUi?> = _joinConfirm.asStateFlow()
    private val trackedFriendUids = mutableSetOf<String>()
    private val userRefs = mutableMapOf<String, com.google.firebase.database.DatabaseReference>()
    private val roomRefs = mutableMapOf<String, com.google.firebase.database.DatabaseReference>()
    private val userListeners = mutableMapOf<String, ValueEventListener>()
    private val roomListeners = mutableMapOf<String, ValueEventListener>()
    private val userSnapshots = mutableMapOf<String, DataSnapshot>()
    private val roomSnapshots = mutableMapOf<String, DataSnapshot>()

    init {
        viewModelScope.launch {
            socialVoiceRoomRepository.observeRoom(requireUid()).collect { _myRoom.value = it }
        }
        viewModelScope.launch {
            entitlement.collect { state ->
                if (state.hasPermanentRoom) {
                    socialVoiceRoomRepository.ensureOwnRoomCreated()
                } else {
                    _myRoom.value = null
                }
            }
        }
        viewModelScope.launch {
            friends.collect { ids -> syncFriendObservers(ids) }
        }
    }

    fun dismissError() {
        _error.value = null
    }

    fun purchaseOwnRoom(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = socialVoiceRoomRepository.purchaseOwnRoom()
            result.onFailure { _error.value = it.message ?: "Could not purchase room" }
            result.onSuccess { onSuccess() }
        }
    }

    fun purchaseVip(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = socialVoiceRoomRepository.purchaseVipMonthly()
            result.onFailure { _error.value = it.message ?: "Could not purchase VIP" }
            result.onSuccess { onSuccess() }
        }
    }

    fun openFriendRoom(friendUid: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val uid = requireUid()
            val currentRoomId = runCatching {
                database.reference.child("users").child(uid).child("currentSocialRoomId").get().await()
                    .getValue(String::class.java).orEmpty()
            }.getOrDefault("")
            if (currentRoomId.isNotBlank() && currentRoomId != friendUid) {
                _joinConfirm.value = JoinRoomConfirmUi(
                    targetRoomId = friendUid,
                    oldRoomId = currentRoomId,
                    enterTargetViaRepository = true,
                )
                return@launch
            }
            val result = socialVoiceRoomRepository.enterFriendRoom(friendUid)
            result.onFailure { _error.value = it.message ?: "Could not enter friend room" }
            result.onSuccess { onSuccess() }
        }
    }

    fun openMyRoom(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val uid = requireUid()
            val targetRoomId = myRoom.value?.roomId?.takeIf { it.isNotBlank() } ?: uid
            val currentRoomId = runCatching {
                database.reference.child("users").child(uid).child("currentSocialRoomId").get().await()
                    .getValue(String::class.java).orEmpty()
            }.getOrDefault("")
            if (currentRoomId.isNotBlank() && currentRoomId != targetRoomId) {
                _joinConfirm.value = JoinRoomConfirmUi(
                    targetRoomId = targetRoomId,
                    oldRoomId = currentRoomId,
                    enterTargetViaRepository = false,
                )
                return@launch
            }
            onSuccess(targetRoomId)
        }
    }

    fun dismissJoinConfirm() {
        _joinConfirm.value = null
    }

    fun confirmSwitchAndJoin(onSuccess: (String) -> Unit) {
        val prompt = _joinConfirm.value ?: return
        viewModelScope.launch {
            val uid = requireUid()
            if (prompt.oldRoomId.isNotBlank() && prompt.oldRoomId != prompt.targetRoomId) {
                runCatching { socialVoiceRoomRepository.setRoomPresence(prompt.oldRoomId, false) }
            }
            if (prompt.enterTargetViaRepository) {
                val result = socialVoiceRoomRepository.enterFriendRoom(prompt.targetRoomId)
                result.onFailure { _error.value = it.message ?: "Could not switch room" }
                result.onSuccess {
                    _joinConfirm.value = null
                    onSuccess(prompt.targetRoomId)
                    runCatching {
                        database.reference.child("users").child(uid).child("currentSocialRoomId")
                            .setValue(prompt.targetRoomId).await()
                    }
                }
            } else {
                _joinConfirm.value = null
                onSuccess(prompt.targetRoomId)
            }
        }
    }

    private fun syncFriendObservers(friendIds: Set<String>) {
        val normalized = friendIds.take(30).toSet()
        val toRemove = trackedFriendUids - normalized
        val toAdd = normalized - trackedFriendUids

        toRemove.forEach { uid ->
            userRefs[uid]?.removeEventListener(userListeners.remove(uid) ?: return@forEach)
            roomRefs[uid]?.removeEventListener(roomListeners.remove(uid) ?: return@forEach)
            userRefs.remove(uid)
            roomRefs.remove(uid)
            userSnapshots.remove(uid)
            roomSnapshots.remove(uid)
            trackedFriendUids.remove(uid)
        }

        toAdd.forEach { uid ->
            val userRef = database.reference.child("users").child(uid)
            val roomRef = database.reference.child("voiceRooms").child(uid)
            val userListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userSnapshots[uid] = snapshot
                    rebuildFriendRooms()
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
            val roomListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    roomSnapshots[uid] = snapshot
                    rebuildFriendRooms()
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
            userRefs[uid] = userRef
            roomRefs[uid] = roomRef
            userListeners[uid] = userListener
            roomListeners[uid] = roomListener
            userRef.addValueEventListener(userListener)
            roomRef.addValueEventListener(roomListener)
            trackedFriendUids.add(uid)
        }
        rebuildFriendRooms()
    }

    private fun rebuildFriendRooms() {
        val cards = trackedFriendUids.map { uid ->
            val userSnap = userSnapshots[uid]
            val roomSnap = roomSnapshots[uid]
            val hasRoomEntitlement = userSnap?.child("voiceRoomOwned")?.getValue(Boolean::class.java) == true ||
                userSnap?.child("voiceRoomPurchaseAt")?.exists() == true
            FriendRoomCardUi(
                ownerUid = uid,
                ownerName = userSnap?.child("username")?.getValue(String::class.java)?.takeIf { it.isNotBlank() }
                    ?: FirebaseUidMapping.shortLabel(uid),
                ownerPhoto = userSnap?.child("profilePictureUrl")?.getValue(String::class.java)?.takeIf { it.isNotBlank() },
                onlineCount = roomSnap?.child("presence")?.childrenCount?.toInt() ?: 0,
                exists = hasRoomEntitlement && roomSnap?.exists() == true,
            )
        }.sortedBy { it.ownerName.lowercase() }
        _friendRooms.value = cards
    }

    override fun onCleared() {
        super.onCleared()
        trackedFriendUids.toList().forEach { uid ->
            userRefs[uid]?.removeEventListener(userListeners[uid] ?: return@forEach)
            roomRefs[uid]?.removeEventListener(roomListeners[uid] ?: return@forEach)
        }
        trackedFriendUids.clear()
        userRefs.clear()
        roomRefs.clear()
        userListeners.clear()
        roomListeners.clear()
        userSnapshots.clear()
        roomSnapshots.clear()
    }

    private fun requireUid(): String =
        com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            ?.takeIf { it.isNotBlank() } ?: error("Not signed in")
}

data class FriendRoomCardUi(
    val ownerUid: String,
    val ownerName: String,
    val ownerPhoto: String?,
    val onlineCount: Int,
    val exists: Boolean,
)

data class JoinRoomConfirmUi(
    val targetRoomId: String,
    val oldRoomId: String,
    val enterTargetViaRepository: Boolean = true,
)

