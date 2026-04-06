package com.masti.soulplay.domain.gift

/**
 * One gift line item written under `giftEvents/...` and emitted by [GiftRepository.observeGiftEvents].
 */
data class GiftEvent(
    val eventId: String,
    val context: GiftSendContext,
    val fromUserId: String,
    val toUserId: String?,
    val giftId: String,
    val coins: Long,
    /** Coins actually awarded to the receiver (randomized portion of [coins]). */
    val receiverCoins: Long,
    val createdAt: Long?,
)
