package com.souljoy.soulmasti.domain.repository

import com.souljoy.soulmasti.domain.gift.GiftEvent
import com.souljoy.soulmasti.domain.gift.GiftSendContext
import kotlinx.coroutines.flow.Flow

data class GiftSendResult(
    val newBalance: Long,
    val eventId: String,
    val receiverCoins: Long = 0L,
)

/**
 * Shared gifting: deduct coins from the signed-in user, append an event for that context.
 * Listeners in Raja Rani, voice UI, or chat subscribe to [observeGiftEvents] with the same [GiftSendContext].
 */
interface GiftRepository {

    suspend fun sendGift(
        context: GiftSendContext,
        giftId: String,
        recipientUserId: String?,
    ): Result<GiftSendResult>

    /** New gifts only (child added), for animations / toasts. */
    fun observeGiftEvents(context: GiftSendContext): Flow<GiftEvent>
}
