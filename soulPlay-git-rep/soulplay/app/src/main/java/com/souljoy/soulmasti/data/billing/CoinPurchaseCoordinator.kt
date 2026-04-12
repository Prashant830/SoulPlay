package com.souljoy.soulmasti.data.billing

import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.souljoy.soulmasti.domain.repository.GameSessionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Handles Play Billing purchases: credits coins idempotently in RTDB, then acknowledges and consumes
 * so the same SKU can be bought again. Always pair [onPurchasesUpdated] with [reconcilePendingPurchases]
 * (slow cards / background completion / response code 7).
 */
class CoinPurchaseCoordinator(
    private val billing: PlayBillingRepository,
    private val game: GameSessionRepository,
) {

    private val mutex = Mutex()
    private val processedTokensThisSession = mutableSetOf<String>()

    private fun logD(msg: String) {
        Log.d(TAG, msg)
    }

    private fun logW(msg: String, t: Throwable? = null) {
        if (t != null) Log.w(TAG, msg, t) else Log.w(TAG, msg)
    }

    /**
     * Query Play for unconsumed in-app purchases and finish the pipeline (credit + consume).
     * Call after billing connects and on app foreground.
     */
    suspend fun reconcilePendingPurchases(knownProducts: List<ProductDetails> = emptyList()): String? =
        mutex.withLock {
            if (!billing.awaitConnection()) {
                logW("reconcilePendingPurchases: billing not connected")
                return null
            }
            val (_, queriedProducts) = billing.queryCoinProductsSuspend()
            val products =
                if (knownProducts.isNotEmpty()) knownProducts else queriedProducts
            val purchases = billing.queryPurchasesInApp()
            var lastMessage: String? = null
            for (p in purchases) {
                // Call locked handler directly — do not nest [handlePurchaseFromPlay] (same mutex).
                handlePurchaseLocked(p, products)?.let { lastMessage = it }
            }
            lastMessage
        }

    /**
     * Process a purchase delivered by [PurchasesUpdatedListener] or [queryPurchasesInApp].
     */
    suspend fun handlePurchaseFromPlay(
        purchase: Purchase,
        knownProducts: List<ProductDetails>,
    ): String? = mutex.withLock {
        handlePurchaseLocked(purchase, knownProducts)
    }

    private suspend fun handlePurchaseLocked(
        purchase: Purchase,
        knownProducts: List<ProductDetails>,
    ): String? {
        logD(
            "handlePurchase: products=${purchase.products} state=${purchase.purchaseState} token=${purchase.purchaseToken.take(12)}…",
        )
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            return "Payment is pending. Complete it in the Play Store — your gold will sync when it clears."
        }
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            return null
        }

        synchronized(processedTokensThisSession) {
            if (purchase.purchaseToken in processedTokensThisSession) {
                logD("handlePurchase: skip duplicate session token")
                return null
            }
        }

        val sku = purchase.products.firstOrNull() ?: run {
            logW("handlePurchase: no product id")
            return null
        }

        var details = knownProducts.find { it.productId == sku }
        if (details == null) {
            logD("handlePurchase: refresh product list for $sku")
            val (_, list) = billing.queryCoinProductsSuspend()
            details = list.find { it.productId == sku }
        }

        val coins = details?.let { InAppCoinProducts.coinsFromProductDetails(it) }
        if (coins == null || coins <= 0L) {
            logW("handlePurchase: could not parse coins for sku=$sku")
            return "Could not read coin amount from Play Store for this product. In Play Console, set the product name to digits only (e.g. 400), then reopen the shop."
        }

        val quantity = purchase.quantity.coerceAtLeast(1)
        val totalCoins = coins * quantity.toLong()
        val token = purchase.purchaseToken
        val alreadyApplied = runCatching { game.hasPlayPurchaseBeenApplied(token) }.getOrElse { false }

        runCatching {
            game.applyPurchasedCoinsForPlayPurchase(token, totalCoins)
        }.onFailure { e ->
            logW("handlePurchase: applyPurchasedCoinsForPlayPurchase failed", e)
            return e.message ?: "Could not add coins. Try “Restore purchases” from the shop or check your connection."
        }

        billing.acknowledgePurchase(purchase)
        val consumeResult = billing.consumePurchase(purchase)
        if (consumeResult.responseCode != BillingClient.BillingResponseCode.OK &&
            consumeResult.responseCode != BillingClient.BillingResponseCode.ITEM_NOT_OWNED
        ) {
            logW("handlePurchase: consume result=${consumeResult.responseCode} ${consumeResult.debugMessage}")
        }

        synchronized(processedTokensThisSession) {
            processedTokensThisSession.add(token)
        }

        val msg = when {
            !alreadyApplied && quantity > 1 ->
                "Added $totalCoins gold ($quantity × $coins per pack)."
            !alreadyApplied ->
                "Added $totalCoins gold to your balance."
            else ->
                "Synced a completed purchase with Play — your balance is up to date."
        }
        return msg
    }

    companion object {
        private const val TAG = "CoinPurchase"
    }
}
