package com.souljoy.soulmasti.domain.model

enum class GamePhase {
    RAJA_DECISION,
    MANTRI_CONFIRM,
    VOICE_DISCUSSION,
    MANTRI_GUESS,
    GAME_OVER,
    UNKNOWN;

    companion object {
        fun fromRaw(value: String?): GamePhase {
            if (value.isNullOrBlank()) return UNKNOWN
            return entries.find { it.name == value } ?: UNKNOWN
        }
    }
}

data class GameRoomSnapshot(
    val roomId: String,
    val status: String,
    val createdAt: Long,
    val message: String?,
    val endReason: String?,
    val players: Map<String, PlayerInRoom>,
    val phase: GamePhase,
    val roles: Map<String, String>,
    val scores: Map<String, Long>,
    val selectedSuspect: String,
    val timerEndAt: Long?,
    /** RTDB `gameState/winner` or `gameState/winnerUid` when the server sets it. */
    val winnerUid: String?,
)

data class PlayerInRoom(
    val online: Boolean,
)
