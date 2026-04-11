package com.souljoy.soulmasti.domain.gift

/**
 * Where a gift is sent from. Use the same [GiftRepository] from Raja Rani, voice room, chat, etc.
 * RTDB path: `giftEvents/{segment}/{scopeId}/{eventId}`.
 */
sealed class GiftSendContext {
    abstract val rtdbSegment: String
    abstract val scopeId: String

    /** Raja Rani / match room — same [roomId] as `rooms/{roomId}`. */
    data class RajaRaniGame(val roomId: String) : GiftSendContext() {
        override val rtdbSegment: String = "rajaRani"
        override val scopeId: String = roomId
    }

    /** Standalone voice room (not necessarily tied to a game room). */
    data class VoiceRoom(val roomId: String) : GiftSendContext() {
        override val rtdbSegment: String = "voice"
        override val scopeId: String = roomId
    }

    /** Direct / group chat — use your stable chat thread id (e.g. `uid1_uid2` sorted). */
    data class Chat(val chatId: String) : GiftSendContext() {
        override val rtdbSegment: String = "chat"
        override val scopeId: String = chatId
    }
}
