package com.souljoy.soulmasti.domain.model

/**
 * One finished-game row derived from RTDB `rooms/{roomId}` when this user is in `players`
 * and the match has ended (`status` ENDED). Role and scores come from `gameState`.
 */
data class GameHistoryEntry(
    val id: String,
    val roomId: String,
    val endedAt: Long,
    /** Firebase uid of this player (matches `rooms/.../players/{uid}`). */
    val userId: String?,
    val role: String?,
    /** This user’s points from `rooms/.../gameState/scores/{uid}`. */
    val gamePoints: Long?,
    val outcome: MatchOutcome,
    val result: String?,
    val summary: String?,
    /** Full scoreboard copied from room `gameState/scores`. */
    val scoresByUser: Map<String, Long>,
)
