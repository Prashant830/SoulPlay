package com.souljoy.soulmasti.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.souljoy.soulmasti.domain.model.ChatMessage
import com.souljoy.soulmasti.domain.model.FriendRequest
import com.souljoy.soulmasti.domain.repository.SocialRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Friend requests, friendships, and DMs live in **Firestore** (not Realtime Database).
 *
 * Collections:
 * - `friendRequests/{fromUid_toUid}` — pending requests (fields: fromUid, toUid, status, createdAt)
 * - `users/{uid}/friends/{friendUid}` — accepted friends
 * - `chats/{chatId}/messages/{msgId}` — DM messages (chatId = sorted pair of uids)
 */
class FirestoreSocialRepository(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
) : SocialRepository {

    private val _incomingFriendRequests = MutableStateFlow<List<FriendRequest>>(emptyList())
    override val incomingFriendRequests: StateFlow<List<FriendRequest>> =
        _incomingFriendRequests.asStateFlow()

    private val _friends = MutableStateFlow<Set<String>>(emptySet())
    override val friends: StateFlow<Set<String>> = _friends.asStateFlow()
    private val _unreadMessageCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    override val unreadMessageCounts: StateFlow<Map<String, Int>> = _unreadMessageCounts.asStateFlow()

    private var incomingRegistration: ListenerRegistration? = null
    private var friendsRegistration: ListenerRegistration? = null
    private var unreadRegistration: ListenerRegistration? = null

    init {
        refreshListeners()
        auth.addAuthStateListener { refreshListeners() }
    }

    private fun refreshListeners() {
        detachListeners()
        val uid = auth.currentUser?.uid ?: run {
            _incomingFriendRequests.value = emptyList()
            _friends.value = emptySet()
            _unreadMessageCounts.value = emptyMap()
            return
        }
        incomingRegistration = db.collection(COL_FRIEND_REQUESTS)
            .whereEqualTo("toUid", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _incomingFriendRequests.value = emptyList()
                    return@addSnapshotListener
                }
                val list = snapshot.documents.mapNotNull { doc ->
                    val fromUid = doc.getString("fromUid") ?: return@mapNotNull null
                    if (doc.getString("status") != STATUS_PENDING) return@mapNotNull null
                    val ts = doc.getTimestamp("createdAt")
                    FriendRequest(fromUid = fromUid, createdAt = ts?.toDate()?.time)
                }.sortedBy { it.createdAt ?: 0L }
                _incomingFriendRequests.value = list
            }

        friendsRegistration = db.collection("users").document(uid).collection(SUB_FRIENDS)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _friends.value = emptySet()
                    return@addSnapshotListener
                }
                _friends.value = snapshot.documents.map { it.id }.toSet()
            }

        unreadRegistration = db.collection("users").document(uid).collection(SUB_CHAT_INBOX)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    _unreadMessageCounts.value = emptyMap()
                    return@addSnapshotListener
                }
                _unreadMessageCounts.value = snapshot.documents.associate { doc ->
                    val count = doc.getLong("unreadCount")?.toInt()?.coerceAtLeast(0) ?: 0
                    doc.id to count
                }.filterValues { it > 0 }
            }
    }

    private fun detachListeners() {
        incomingRegistration?.remove()
        incomingRegistration = null
        friendsRegistration?.remove()
        friendsRegistration = null
        unreadRegistration?.remove()
        unreadRegistration = null
    }

    override suspend fun sendFriendRequest(toUid: String): Result<Unit> {
        val from = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (toUid.isBlank() || toUid == from) {
            return Result.failure(IllegalArgumentException("Invalid player"))
        }
        // All Firestore I/O must stay inside runCatching — otherwise PERMISSION_DENIED etc. crashes the app.
        return runCatching {
            if (
                db.collection("users").document(from).collection(SUB_FRIENDS).document(toUid)
                    .get().await().exists()
            ) {
                throw IllegalStateException("Already friends")
            }
            val theirRequestId = friendRequestDocId(toUid, from)
            val theirRequest =
                db.collection(COL_FRIEND_REQUESTS).document(theirRequestId).get().await()
            if (theirRequest.exists() && theirRequest.getString("status") == STATUS_PENDING) {
                acceptFriendRequest(toUid).getOrThrow()
                return@runCatching
            }
            val myRequestId = friendRequestDocId(from, toUid)
            val existingMine =
                db.collection(COL_FRIEND_REQUESTS).document(myRequestId).get().await()
            if (existingMine.exists() && existingMine.getString("status") == STATUS_PENDING) {
                throw IllegalStateException("Request already sent")
            }
            db.collection(COL_FRIEND_REQUESTS).document(myRequestId).set(
                mapOf(
                    "fromUid" to from,
                    "toUid" to toUid,
                    "status" to STATUS_PENDING,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
        }.mapFailureForUi()
    }

    override suspend fun acceptFriendRequest(fromUid: String): Result<Unit> {
        val me = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (fromUid.isBlank() || fromUid == me) {
            return Result.failure(IllegalArgumentException("Invalid user"))
        }
        val requestId = friendRequestDocId(fromUid, me)
        return runCatching {
            val batch = db.batch()
            batch.delete(db.collection(COL_FRIEND_REQUESTS).document(requestId))
            batch.set(
                db.collection("users").document(me).collection(SUB_FRIENDS).document(fromUid),
                mapOf("since" to FieldValue.serverTimestamp())
            )
            batch.set(
                db.collection("users").document(fromUid).collection(SUB_FRIENDS).document(me),
                mapOf("since" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Unit
        }.mapFailureForUi()
    }

    override suspend fun declineFriendRequest(fromUid: String): Result<Unit> {
        val me = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val requestId = friendRequestDocId(fromUid, me)
        return runCatching {
            db.collection(COL_FRIEND_REQUESTS).document(requestId).delete().await()
            Unit
        }.mapFailureForUi()
    }

    override fun observeChatMessages(peerUid: String): Flow<List<ChatMessage>> = callbackFlow {
        val me = auth.currentUser?.uid
        if (me.isNullOrBlank() || peerUid.isBlank() || peerUid == me) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val chatId = directChatId(me, peerUid)
        val ref = db.collection(COL_CHATS).document(chatId).collection(SUB_MESSAGES)
        val reg = ref.addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val items = snapshot.documents.mapNotNull { doc ->
                val from = doc.getString("fromUid") ?: return@mapNotNull null
                val text = doc.getString("text")?.trim().orEmpty()
                if (text.isEmpty()) return@mapNotNull null
                val ts = doc.getTimestamp("createdAt")
                ChatMessage(
                    id = doc.id,
                    fromUid = from,
                    text = text,
                    createdAt = ts?.toDate()?.time,
                )
            }.sortedBy { it.createdAt ?: 0L }
            trySend(items)
        }
        awaitClose { reg.remove() }
    }

    override suspend fun sendChatMessage(peerUid: String, text: String): Result<Unit> {
        val me = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.failure(IllegalArgumentException("Empty message"))
        if (peerUid.isBlank() || peerUid == me) {
            return Result.failure(IllegalArgumentException("Invalid chat"))
        }
        if (!_friends.value.contains(peerUid)) {
            return Result.failure(IllegalStateException("You can only chat with friends"))
        }
        val chatId = directChatId(me, peerUid)
        return runCatching {
            val chatRef = db.collection(COL_CHATS).document(chatId)
            chatRef.collection(SUB_MESSAGES).add(
                mapOf(
                    "fromUid" to me,
                    "text" to trimmed,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            val myInboxRef =
                db.collection("users").document(me).collection(SUB_CHAT_INBOX).document(peerUid)
            myInboxRef.set(
                mapOf(
                    "peerUid" to peerUid,
                    "lastMessage" to trimmed,
                    "lastFromUid" to me,
                    "lastAt" to FieldValue.serverTimestamp(),
                    "unreadCount" to 0,
                ),
                SetOptions.merge()
            ).await()

            val peerInboxRef =
                db.collection("users").document(peerUid).collection(SUB_CHAT_INBOX).document(me)
            db.runTransaction { tx ->
                val current = tx.get(peerInboxRef).getLong("unreadCount") ?: 0L
                tx.set(
                    peerInboxRef,
                    mapOf(
                        "peerUid" to me,
                        "lastMessage" to trimmed,
                        "lastFromUid" to me,
                        "lastAt" to FieldValue.serverTimestamp(),
                        "unreadCount" to (current + 1L),
                    ),
                    SetOptions.merge()
                )
                null
            }.await()
        }
    }

    override suspend fun markChatAsRead(peerUid: String): Result<Unit> {
        val me = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Not signed in"))
        if (peerUid.isBlank() || peerUid == me) {
            return Result.failure(IllegalArgumentException("Invalid chat"))
        }
        return runCatching {
            db.collection("users").document(me).collection(SUB_CHAT_INBOX).document(peerUid).set(
                mapOf(
                    "peerUid" to peerUid,
                    "unreadCount" to 0,
                    "lastReadAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge()
            ).await()
        }
    }

    private fun directChatId(a: String, b: String): String {
        val sorted = listOf(a, b).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    private companion object {
        const val COL_FRIEND_REQUESTS = "friendRequests"
        const val COL_CHATS = "chats"
        const val SUB_FRIENDS = "friends"
        const val SUB_MESSAGES = "messages"
        const val SUB_CHAT_INBOX = "chatInbox"
        const val STATUS_PENDING = "pending"
    }
}

private fun friendRequestDocId(fromUid: String, toUid: String): String = "${fromUid}_$toUid"

/**
 * Keeps [IllegalStateException] / [IllegalArgumentException] messages (e.g. "Request already sent");
 * maps raw Firestore errors to a short UI-safe message so failures never crash uncaught.
 */
private fun Result<Unit>.mapFailureForUi(): Result<Unit> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { e ->
            when (e) {
                is IllegalStateException, is IllegalArgumentException -> Result.failure(e)
                is FirebaseFirestoreException -> Result.failure(
                    IllegalStateException(firestoreMessageForUi(e), e)
                )
                else -> Result.failure(IllegalStateException(e.message ?: "Could not complete request", e))
            }
        }
    )

private fun firestoreMessageForUi(e: FirebaseFirestoreException): String = when (e.code) {
    FirebaseFirestoreException.Code.PERMISSION_DENIED ->
        "Could not send friend request. Enable Firestore and update security rules for friendRequests and users."
    FirebaseFirestoreException.Code.UNAVAILABLE ->
        "Network unavailable. Try again."
    else -> e.message ?: "Firestore error"
}
