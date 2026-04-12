package com.souljoy.soulmasti.ui.shop

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.souljoy.soulmasti.BuildConfig
import com.souljoy.soulmasti.data.billing.InAppCoinProducts
import com.souljoy.soulmasti.data.billing.PlayBillingRepository
import com.souljoy.soulmasti.domain.repository.GameSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class GoldShopUiState(
    val loading: Boolean = true,
    val products: List<ProductDetails> = emptyList(),
    val billingError: String? = null,
    val userMessage: String? = null,
)

class GoldShopViewModel(
    private val billing: PlayBillingRepository,
    private val game: GameSessionRepository,
) : ViewModel() {

    private fun logD(msg: String) {
        Log.d(TAG, msg)
    }

    private fun logW(msg: String, t: Throwable? = null) {
        if (t != null) Log.w(TAG, msg, t) else Log.w(TAG, msg)
    }

    private val _uiState = MutableStateFlow(GoldShopUiState())
    val uiState: StateFlow<GoldShopUiState> = _uiState.asStateFlow()

    val coinBalance: StateFlow<Long?> =
        game.observeUserTotalWinnings()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val processedPurchaseTokens = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            billing.purchaseEvents.collect { event ->
                when (event) {
                    is PlayBillingRepository.PurchaseResult.Success -> {
                        logD("purchaseEvents: Success count=${event.purchases.size}")
                        event.purchases.forEach { processPurchase(it) }
                    }
                    PlayBillingRepository.PurchaseResult.UserCanceled -> {
                        logD("purchaseEvents: UserCanceled")
                    }
                    is PlayBillingRepository.PurchaseResult.Error -> {
                        logW("purchaseEvents: Error code=${event.code} msg=${event.debugMessage}")
                        _uiState.update {
                            it.copy(
                                userMessage = billingMessage(event.code, event.debugMessage),
                            )
                        }
                    }
                }
            }
        }
    }

    fun consumeUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    suspend fun refreshProducts() {
        logD("refreshProducts: start")
        _uiState.update { it.copy(loading = true, billingError = null) }
        suspendCoroutine { cont ->
            billing.startConnection { ok ->
                logD("refreshProducts: startConnection ok=$ok")
                if (!ok) {
                    logW("refreshProducts: billing connection failed")
                    _uiState.update {
                        it.copy(
                            loading = false,
                            billingError = "Google Play Billing is not available. Use a device with Play Store and a tester account.",
                        )
                    }
                    cont.resume(Unit)
                    return@startConnection
                }
                billing.queryCoinProducts { result, list ->
                    logD(
                        "refreshProducts: query result=${result.responseCode} products=${list.size} err=${result.debugMessage}",
                    )
                    list.forEach { d ->
                        logD(
                            "  ui product id=${d.productId} coins=${InAppCoinProducts.coinsFromProductDetails(d)} price=${d.oneTimePurchaseOfferDetails?.formattedPrice}",
                        )
                    }
                    val err = if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        result.debugMessage.ifBlank { "Billing error ${result.responseCode}" }
                    } else {
                        null
                    }
                    _uiState.update {
                        it.copy(
                            loading = false,
                            products = list,
                            billingError = err,
                        )
                    }
                    cont.resume(Unit)
                }
            }
        }
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        logD("launchPurchase: ${productDetails.productId}")
        val result = billing.launchBillingFlow(activity, productDetails)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            logW("launchPurchase failed: ${result.responseCode} ${result.debugMessage}")
            _uiState.update {
                it.copy(userMessage = billingMessage(result.responseCode, result.debugMessage))
            }
        }
    }

    private fun processPurchase(purchase: Purchase) {
        logD(
            "processPurchase: products=${purchase.products} state=${purchase.purchaseState} token=${purchase.purchaseToken.take(16)}…",
        )
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
            if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                logD("processPurchase: pending — waiting for user")
                _uiState.update {
                    it.copy(userMessage = "Payment is pending. Complete it in the Play Store, then reopen the shop.")
                }
            }
            return
        }
        synchronized(processedPurchaseTokens) {
            if (purchase.purchaseToken in processedPurchaseTokens) {
                logD("processPurchase: skip duplicate token")
                return
            }
        }
        val sku = purchase.products.firstOrNull() ?: run {
            logW("processPurchase: no product id on purchase")
            return
        }

        viewModelScope.launch {
            var details = _uiState.value.products.find { it.productId == sku }
            if (details == null) {
                logD("processPurchase: no cached ProductDetails for $sku — refreshProducts")
                refreshProducts()
                details = _uiState.value.products.find { it.productId == sku }
            }
            val coins = details?.let { InAppCoinProducts.coinsFromProductDetails(it) }
            logD("processPurchase: sku=$sku coins=$coins detailsFound=${details != null}")
            if (coins == null || coins <= 0L) {
                logW("processPurchase: could not parse coins for sku=$sku name=${details?.name} title=${details?.title}")
                _uiState.update {
                    it.copy(
                        userMessage = "Could not read coin amount from Play Store for this product. In Play Console, set the product name to digits only (e.g. 400), then reopen the shop.",
                    )
                }
                return@launch
            }

            // Play Store allows quantity > 1 on checkout; [Purchase.getQuantity] reflects that.
            val quantity = purchase.quantity.coerceAtLeast(1)
            val totalCoins = coins * quantity.toLong()

            runCatching {
                game.addPurchasedCoins(totalCoins)
            }.onFailure { e ->
                logW("processPurchase: addPurchasedCoins failed", e)
                _uiState.update {
                    it.copy(userMessage = e.message ?: "Could not add coins. Contact support.")
                }
            }.onSuccess {
                logD("processPurchase: credited total=$totalCoins (qty=$quantity × $coins per pack), acknowledge + consume")
                synchronized(processedPurchaseTokens) {
                    processedPurchaseTokens.add(purchase.purchaseToken)
                }
                billing.acknowledgePurchase(purchase)
                val consumeResult = billing.consumePurchase(purchase)
                if (consumeResult.responseCode != BillingClient.BillingResponseCode.OK &&
                    consumeResult.responseCode != BillingClient.BillingResponseCode.ITEM_NOT_OWNED
                ) {
                    logW("processPurchase: consume result=${consumeResult.responseCode} ${consumeResult.debugMessage}")
                }
                val msg = if (quantity > 1) {
                    "Added $totalCoins gold ($quantity × $coins per pack)."
                } else {
                    "Added $totalCoins gold to your balance."
                }
                _uiState.update {
                    it.copy(userMessage = msg)
                }
            }
        }
    }

    private fun billingMessage(code: Int, debug: String): String =
        debug.ifBlank {
            when (code) {
                BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                    "Play Billing configuration error (e.g. wrong product id or app not signed for Play testing)."
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                    "This pack is not available yet. Check Play Console product is active."
                BillingClient.BillingResponseCode.USER_CANCELED -> "Purchase canceled."
                else -> "Billing error ($code)."
            }
        }

    companion object {
        private const val TAG = "GoldShop"
    }
}
