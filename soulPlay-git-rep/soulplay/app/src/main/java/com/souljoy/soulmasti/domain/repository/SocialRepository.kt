package com.souljoy.soulmasti.domain.repository

import com.souljoy.soulmasti.domain.model.ChatMessage
import com.souljoy.soulmasti.domain.model.FriendRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SocialRepository {
    /** Pending incoming friend requests (others → current user). */
    val incomingFriendRequests: StateFlow<List<FriendRequest>>

    /** Friends of the current user (both directions stored in RTDB). */
    val friends: StateFlow<Set<String>>

    /** `peerUid -> unreadCount` for DM notifications in Chat list and badges. */
    val unreadMessageCounts: StateFlow<Map<String, Int>>

    suspend fun sendFriendRequest(toUid: String): Result<Unit>

    suspend fun acceptFriendRequest(fromUid: String): Result<Unit>

    suspend fun declineFriendRequest(fromUid: String): Result<Unit>

    fun observeChatMessages(peerUid: String): Flow<List<ChatMessage>>

    suspend fun sendChatMessage(peerUid: String, text: String): Result<Unit>

    suspend fun markChatAsRead(peerUid: String): Result<Unit>
}
