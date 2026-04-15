package com.souljoy.soulmasti.ui.chat

data class GiftChatPayload(
    val giftId: String,
    val giftLabel: String,
    val giftCoins: Long,
    val receiverCoins: Long,
    val receiverSoul: Long,
    val selectedCount: Int,
)

fun parseGiftChatPayload(text: String): GiftChatPayload? {
    if (!text.startsWith("GIFT|")) return null
    val p = text.split("|")
    if (p.size < 5) return null
    val giftCoins = p[3].toLongOrNull() ?: 0L
    val receiverCoins = p[4].toLongOrNull() ?: 0L
    val receiverSoul = p.getOrNull(5)?.toLongOrNull() ?: (giftCoins / 10L)
    val selectedCount = p.getOrNull(6)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    return GiftChatPayload(
        giftId = p[1],
        giftLabel = p[2],
        giftCoins = giftCoins,
        receiverCoins = receiverCoins,
        receiverSoul = receiverSoul,
        selectedCount = selectedCount,
    )
}
