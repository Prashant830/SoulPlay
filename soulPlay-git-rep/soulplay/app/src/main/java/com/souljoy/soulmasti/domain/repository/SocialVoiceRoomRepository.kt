package com.souljoy.soulmasti.domain.repository

import com.souljoy.soulmasti.domain.model.SocialVoiceChatMessage
import com.souljoy.soulmasti.domain.model.SocialSeatInvite
import com.souljoy.soulmasti.domain.model.SocialVoiceRoomSnapshot
import com.souljoy.soulmasti.domain.model.VoiceEntitlementState
import kotlinx.coroutines.flow.Flow

interface SocialVoiceRoomRepository {
    fun observeMyEntitlement(): Flow<VoiceEntitlementState>

    suspend fun purchaseOwnRoom(costCoins: Long = 3_000L): Result<Unit>

    suspend fun purchaseVipMonthly(costCoins: Long = 6_000L): Result<Unit>

    fun observeRoom(roomId: String): Flow<SocialVoiceRoomSnapshot?>

    suspend fun ensureOwnRoomCreated(): Result<String>

    suspend fun enterFriendRoom(roomId: String, friendEntryCostCoins: Long = 100L): Result<Unit>

    suspend fun setRoomPresence(roomId: String, online: Boolean): Result<Unit>

    suspend fun takeSeat(roomId: String, seatNo: Int): Result<Unit>

    suspend fun leaveSeat(roomId: String, seatNo: Int): Result<Unit>

    suspend fun setSeatLocked(roomId: String, seatNo: Int, locked: Boolean): Result<Unit>

    suspend fun setSeatMuted(roomId: String, seatNo: Int, muted: Boolean): Result<Unit>

    suspend fun removeSeatOccupant(roomId: String, seatNo: Int): Result<Unit>

    suspend fun sendSeatInvite(roomId: String, toUid: String, seatNo: Int): Result<Unit>

    suspend fun respondSeatInvite(roomId: String, inviteId: String, accept: Boolean): Result<Unit>

    fun observeSeatInvites(roomId: String): Flow<List<SocialSeatInvite>>

    suspend fun dismissMyPendingSeatInvites(roomId: String): Result<Unit>

    fun observeMyKickSignal(roomId: String): Flow<Long?>

    fun observeMyChatVisibleAfter(roomId: String): Flow<Long?>

    suspend fun markMyChatCleared(roomId: String): Result<Unit>

    suspend fun updateRoomName(roomId: String, roomName: String): Result<Unit>

    suspend fun updateRoomCoverUrl(roomId: String, coverUrl: String): Result<Unit>

    suspend fun updateRoomBackgroundName(roomId: String, backgroundName: String): Result<Unit>

    suspend fun collapseRoomIfOwnerLeft(roomId: String): Result<Unit>

    fun observeRoomChat(roomId: String): Flow<List<SocialVoiceChatMessage>>

    suspend fun sendRoomChatMessage(roomId: String, text: String): Result<Unit>

    suspend fun sendSeatEmote(roomId: String, seatNo: Int, emoteKey: String): Result<Unit>
}

