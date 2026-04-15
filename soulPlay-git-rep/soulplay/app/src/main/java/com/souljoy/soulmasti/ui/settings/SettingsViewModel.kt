package com.souljoy.soulmasti.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.souljoy.soulmasti.data.firebase.ProfilePhotoStorage
import com.souljoy.soulmasti.data.firebase.profilePhotoUploadMessage
import com.souljoy.soulmasti.domain.model.GameHistoryEntry
import com.souljoy.soulmasti.domain.repository.GiftRepository
import com.souljoy.soulmasti.domain.repository.GameSessionRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel(
    application: Application,
    private val game: GameSessionRepository,
    private val giftRepository: GiftRepository,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage,
) : AndroidViewModel(application) {

    val totalWinnings: StateFlow<Long?> =
        game.observeUserTotalWinnings().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val gameHistory: StateFlow<List<GameHistoryEntry>> =
        game.observeUserGameHistory().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _username = MutableStateFlow<String?>(null)
    val username: StateFlow<String?> = _username

    private val _profilePictureUrl = MutableStateFlow<String?>(null)
    val profilePictureUrl: StateFlow<String?> = _profilePictureUrl

    private val _gender = MutableStateFlow<String?>(null)
    val gender: StateFlow<String?> = _gender
    private val _signature = MutableStateFlow<String?>(null)
    val signature: StateFlow<String?> = _signature
    private val _soul = MutableStateFlow<Long?>(null)
    val soul: StateFlow<Long?> = _soul

    private val _historyUsernames = MutableStateFlow<Map<String, String>>(emptyMap())
    val historyUsernames: StateFlow<Map<String, String>> = _historyUsernames
    private val _userProfilePhotos = MutableStateFlow<Map<String, String>>(emptyMap())
    val userProfilePhotos: StateFlow<Map<String, String>> = _userProfilePhotos

    private val _giftReceivedCoins = MutableStateFlow<Long?>(null)
    val giftReceivedCoins: StateFlow<Long?> = _giftReceivedCoins

    private val _giftReceivedCount = MutableStateFlow<Long?>(null)
    val giftReceivedCount: StateFlow<Long?> = _giftReceivedCount

    private val _receivedGiftHistory = MutableStateFlow<List<ReceivedGiftSummary>>(emptyList())
    val receivedGiftHistory: StateFlow<List<ReceivedGiftSummary>> = _receivedGiftHistory

    init {
        viewModelScope.launch {
            runCatching { game.ensureSignedInAndPresence() }
                .onSuccess {
                    loadProfile()
                    loadReceivedGifts()
                }
        }
        viewModelScope.launch {
            game.observeUserGameHistory().collectLatest { entries ->
                preloadHistoryUsernames(entries)
            }
        }
    }

    private suspend fun loadProfile() {
        val uid = game.firebaseUid.value ?: return
        val snap = database.reference.child("users").child(uid).get().await()
        _username.value = snap.child("username").getValue(String::class.java)
        _profilePictureUrl.value = snap.child("profilePictureUrl").getValue(String::class.java)
        _profilePictureUrl.value?.takeIf { it.isNotBlank() }?.let { url ->
            _userProfilePhotos.value = _userProfilePhotos.value + (uid to url)
        }
        _gender.value = snap.child("gender").getValue(String::class.java)
        _signature.value =
            snap.child("signature").getValue(String::class.java)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: snap.child("bio").getValue(String::class.java)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                ?: snap.child("status").getValue(String::class.java)
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
        _giftReceivedCoins.value = snap.child("giftReceivedCoins").getValue(Long::class.java)
        _giftReceivedCount.value = snap.child("giftReceivedCount").getValue(Long::class.java)
        _soul.value = readLongFromSchema(snap, "soul", "soulValue", "charms")
    }

    private suspend fun loadReceivedGifts() {
        val uid = game.firebaseUid.value ?: return
        val snap = database.reference.child("users").child(uid).child("giftsReceived").get().await()
        val list = mutableListOf<ReceivedGiftSummary>()
        val senderNames = mutableMapOf<String, String>()
        val senderPhotos = _userProfilePhotos.value.toMutableMap()
        for (child in snap.children) {
            val coins = child.child("coins").getValue(Long::class.java) ?: continue
            val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
            val fromUserId = child.child("fromUserId").getValue(String::class.java)
            val giftId = child.child("giftId").getValue(String::class.java)
            val fromName = fromUserId?.let { uidSender ->
                senderNames[uidSender] ?: run {
                    val userSnap = runCatching {
                        database.reference.child("users").child(uidSender).get().await()
                    }.getOrNull()
                    val name = userSnap?.child("username")
                        ?.getValue(String::class.java)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                    val photo = userSnap?.child("profilePictureUrl")
                        ?.getValue(String::class.java)
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                    if (name != null) senderNames[uidSender] = name
                    if (photo != null) senderPhotos[uidSender] = photo
                    name
                }
            }
            list.add(
                ReceivedGiftSummary(
                    fromUserId = fromUserId,
                    fromDisplayName = fromName,
                    giftId = giftId,
                    coins = coins,
                    createdAt = createdAt,
                )
            )
        }
        list.sortByDescending { it.createdAt }
        _receivedGiftHistory.value = list
        _userProfilePhotos.value = senderPhotos
    }

    private suspend fun preloadHistoryUsernames(entries: List<GameHistoryEntry>) {
        val allUids = entries
            .flatMap { entry ->
                buildList {
                    entry.userId?.takeIf { it.isNotBlank() }?.let { add(it) }
                    addAll(entry.scoresByUser.keys)
                }
            }
            .filter { it.isNotBlank() }
            .toSet()
        if (allUids.isEmpty()) return

        val known = _historyUsernames.value
        val missing = allUids.filterNot { known.containsKey(it) }
        if (missing.isEmpty()) return

        val fetched = mutableMapOf<String, String>()
        val fetchedPhotos = mutableMapOf<String, String>()
        for (uid in missing) {
            val userSnap = runCatching {
                database.reference.child("users").child(uid).get().await()
            }.getOrNull()
            val name = userSnap?.child("username")
                ?.getValue(String::class.java)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val photo = userSnap?.child("profilePictureUrl")
                ?.getValue(String::class.java)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            if (name != null) fetched[uid] = name
            if (photo != null) fetchedPhotos[uid] = photo
        }
        if (fetched.isNotEmpty()) {
            _historyUsernames.value = known + fetched
        }
        if (fetchedPhotos.isNotEmpty()) {
            _userProfilePhotos.value = _userProfilePhotos.value + fetchedPhotos
        }
    }

    suspend fun updateUsername(newUsername: String) {
        val uid = game.firebaseUid.value ?: return
        val trimmed = newUsername.trim()
        if (trimmed.isBlank()) return
        database.reference.child("users").child(uid).child("username").setValue(trimmed).await()
        _username.value = trimmed
    }

    suspend fun updateSignature(newSignature: String) {
        val uid = game.firebaseUid.value ?: return
        val trimmed = newSignature.trim()
        database.reference.child("users").child(uid).child("signature").setValue(trimmed).await()
        _signature.value = trimmed
    }

    /**
     * Uploads a new avatar to Storage and updates Realtime Database.
     * @throws Exception with a user-readable message on failure (see [profilePhotoUploadMessage]).
     */
    suspend fun updateProfilePicture(uri: Uri) {
        val uid = game.firebaseUid.value ?: error("Not signed in")
        val url = runCatching {
            ProfilePhotoStorage.uploadAvatarAndGetDownloadUrl(
                storage = storage,
                uid = uid,
                uri = uri,
                contentResolver = getApplication<Application>().contentResolver,
            )
        }.getOrElse { e ->
            error(e.profilePhotoUploadMessage())
        }
        database.reference.child("users").child(uid).child("profilePictureUrl").setValue(url).await()
        _profilePictureUrl.value = url
    }

    suspend fun clearProfilePictureToDefault() {
        val uid = game.firebaseUid.value ?: return
        database.reference.child("users").child(uid).child("profilePictureUrl").setValue("").await()
        _profilePictureUrl.value = ""
    }

    fun logout() {
        auth.signOut()
    }
}

private fun readLongFromSchema(snap: DataSnapshot, vararg keys: String): Long? {
    for (key in keys) {
        val child = snap.child(key)
        child.getValue(Long::class.java)?.let { return it }
        child.getValue(Int::class.java)?.toLong()?.let { return it }
        child.getValue(Double::class.java)?.toLong()?.let { return it }
        child.getValue(String::class.java)?.trim()?.toLongOrNull()?.let { return it }
    }
    return null
}

data class ReceivedGiftSummary(
    val fromUserId: String?,
    val fromDisplayName: String?,
    val giftId: String?,
    val coins: Long,
    val createdAt: Long,
)
