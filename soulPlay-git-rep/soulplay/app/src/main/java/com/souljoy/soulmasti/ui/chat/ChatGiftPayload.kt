package com.souljoy.soulmasti.ui.chat

data class GiftChatPayload(
    val giftId: String,
    val giftLabel: String,
    val giftCoins: Long,
    val receiverCoins: Long,
)

fun parseGiftChatPayload(text: String): GiftChatPayload? {
    if (!text.startsWith("GIFT|")) return null
    val p = text.split("|")
    if (p.size < 5) return null
    val giftCoins = p[3].toLongOrNull() ?: 0L
    val receiverCoins = p[4].toLongOrNull() ?: 0L
    return GiftChatPayload(
        giftId = p[1],
        giftLabel = p[2],
        giftCoins = giftCoins,
        receiverCoins = receiverCoins,
    )
}
