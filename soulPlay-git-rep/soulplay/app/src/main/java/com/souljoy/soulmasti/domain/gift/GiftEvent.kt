package com.souljoy.soulmasti.domain.gift

/**
 * One gift line item written under `giftEvents/...` and emitted by [GiftRepository.observeGiftEvents].
 */
data class GiftEvent(
    val eventId: String,
    val context: GiftSendContext,
    val fromUserId: String,
    val toUserId: String?,
    val giftId: String,
    val selectedCount: Int,
    val coins: Long,
    /** Coins actually awarded to the receiver (randomized portion of [coins]). */
    val receiverCoins: Long,
    /** Soul awarded to receiver, derived from gift value (10 gold = 1 soul). */
    val receiverSoul: Long,
    val createdAt: Long?,
)
