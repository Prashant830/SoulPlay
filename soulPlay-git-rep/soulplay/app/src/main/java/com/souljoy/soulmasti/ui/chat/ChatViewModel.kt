package com.souljoy.soulmasti.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.domain.model.FriendRequest
import com.souljoy.soulmasti.domain.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Snapshot from Realtime Database `users/{uid}` (username + profilePictureUrl). */
data class RtdbUserPreview(
    val displayName: String,
    /** Non-blank HTTPS URL when the user has a profile photo in RTDB. */
    val photoUrl: String?,
)

class ChatViewModel(
    application: Application,
    private val social: SocialRepository,
    private val database: FirebaseDatabase,
) : AndroidViewModel(application) {

    val incomingFriendRequests: StateFlow<List<FriendRequest>> = social.incomingFriendRequests
    val friends: StateFlow<Set<String>> = social.friends
    val unreadMessageCounts: StateFlow<Map<String, Int>> = social.unreadMessageCounts

    private val _userPreviews = MutableStateFlow<Map<String, RtdbUserPreview>>(emptyMap())
    val userPreviews: StateFlow<Map<String, RtdbUserPreview>> = _userPreviews.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                social.incomingFriendRequests,
                social.friends,
                social.unreadMessageCounts
            ) { reqs, friendSet, unread ->
                reqs.map { it.fromUid }.toSet() + friendSet + unread.keys
            }.collect { uids -> preloadUserPreviewsFromRtdb(uids) }
        }
    }

    private suspend fun preloadUserPreviewsFromRtdb(uids: Set<String>) {
        if (uids.isEmpty()) return
        val known = _userPreviews.value
        val missing = uids.filterNot { known.containsKey(it) }
        if (missing.isEmpty()) return
        val loaded = mutableMapOf<String, RtdbUserPreview>()
        for (uid in missing) {
            val preview = runCatching {
                val snap = database.reference.child("users").child(uid).get().await()
                val name = snap.child("username").getValue(String::class.java)?.trim().orEmpty()
                val photo = snap.child("profilePictureUrl").getValue(String::class.java)?.trim().orEmpty()
                RtdbUserPreview(
                    displayName = name.ifBlank { FirebaseUidMapping.shortLabel(uid) },
                    photoUrl = photo.takeIf { it.isNotBlank() },
                )
            }.getOrElse {
                RtdbUserPreview(FirebaseUidMapping.shortLabel(uid), null)
            }
            loaded[uid] = preview
        }
        _userPreviews.value = known + loaded
    }

    fun dismissError() {
        _error.value = null
    }

    fun accept(fromUid: String) {
        viewModelScope.launch {
            val result = try {
                social.acceptFriendRequest(fromUid)
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: "Could not accept", e))
            }
            result.onFailure { e ->
                _error.value = e.message ?: "Could not accept"
            }
        }
    }

    fun decline(fromUid: String) {
        viewModelScope.launch {
            val result = try {
                social.declineFriendRequest(fromUid)
            } catch (e: Exception) {
                Result.failure(IllegalStateException(e.message ?: "Could not decline", e))
            }
            result.onFailure { e ->
                _error.value = e.message ?: "Could not decline"
            }
        }
    }

    fun displayNameFor(uid: String): String =
        _userPreviews.value[uid]?.displayName ?: FirebaseUidMapping.shortLabel(uid)

    fun unreadCountFor(uid: String): Int = unreadMessageCounts.value[uid] ?: 0
}
