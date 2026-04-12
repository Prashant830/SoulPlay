package com.souljoy.soulmasti.domain.model

/**
 * A reaction emoji broadcast in a voice room (RTDB `rooms/{roomId}/emojiEvents/{id}`).
 */
data class RoomEmojiEvent(
    val eventId: String,
    val fromUid: String,
    val emoji: String,
    val sentAt: Long,
)
