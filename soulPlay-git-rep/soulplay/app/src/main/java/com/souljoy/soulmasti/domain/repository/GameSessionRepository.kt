package com.souljoy.soulmasti.domain.repository

import com.souljoy.soulmasti.domain.model.GameHistoryEntry
import com.souljoy.soulmasti.domain.model.GameRoomSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface GameSessionRepository {
    val firebaseUid: StateFlow<String?>

    suspend fun ensureSignedInAndPresence()

    /** Add this user to matchmaking queue (writes `matchmaking/waitingPlayers/{uid}`). */
    suspend fun addToWaitingQueue()

    /** Remove from queue if still present (safe after server already removed). */
    suspend fun removeFromWaitingQueue()

    /**
     * Suspend until this user appears in a `rooms/{roomId}` with `players` containing their uid.
     */
    suspend fun awaitMatchedRoom(): String

    fun observeRoom(roomId: String): Flow<GameRoomSnapshot?>

    suspend fun setSelectedSuspect(roomId: String, suspectFirebaseUid: String)

    /** Updates `users/{uid}/online` for the current or fallback local id (best-effort). */
    suspend fun setLocalPlayerOnline(online: Boolean)

    /** Current coin balance from `users/{uid}/totalWinnings` (0 if unset). */
    suspend fun getCoinBalance(): Long

    /**
     * Atomically subtracts [com.souljoy.soulmasti.domain.RoomJoinEconomy.JOIN_ROOM_FEE_COINS] from
     * `users/{uid}/totalWinnings`. Fails if balance is insufficient.
     * @return balance after deduction
     */
    suspend fun deductJoinRoomFee(): Long

    /** Observes `users/{uid}/totalWinnings` for the current firebase/local id. */
    fun observeUserTotalWinnings(): Flow<Long?>

    /**
     * Finished games for this user, derived from [rooms] where `players` contains the uid
     * and `status` is ENDED (roles/scores from `gameState`).
     */
    fun observeUserGameHistory(): Flow<List<GameHistoryEntry>>
}
