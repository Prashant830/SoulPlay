package com.souljoy.soulmasti.ui.shop

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.ProductDetails
import com.souljoy.soulmasti.data.billing.CoinPurchaseCoordinator
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
    private val coinPurchases: CoinPurchaseCoordinator,
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

    init {
        viewModelScope.launch {
            billing.purchaseEvents.collect { event ->
                when (event) {
                    is PlayBillingRepository.PurchaseResult.Success -> {
                        logD("purchaseEvents: Success count=${event.purchases.size}")
                        val products = _uiState.value.products
                        event.purchases.forEach { purchase ->
                            val msg = runCatching {
                                coinPurchases.handlePurchaseFromPlay(purchase, products)
                            }.getOrElse { e ->
                                logW("purchaseEvents: handlePurchase failed", e)
                                null
                            }
                            if (msg != null) {
                                _uiState.update { it.copy(userMessage = msg) }
                            }
                        }
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
        val products = _uiState.value.products
        val syncMsg = runCatching {
            coinPurchases.reconcilePendingPurchases(knownProducts = products)
        }.getOrElse { e ->
            logW("refreshProducts: reconcile failed", e)
            null
        }
        if (syncMsg != null) {
            _uiState.update { it.copy(userMessage = syncMsg) }
        }
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        logD("launchPurchase: ${productDetails.productId}")
        val result = billing.launchBillingFlow(activity, productDetails)
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                logW("launchPurchase: ITEM_ALREADY_OWNED — reconciling pending purchase")
                viewModelScope.launch {
                    val msg = runCatching {
                        coinPurchases.reconcilePendingPurchases(knownProducts = _uiState.value.products)
                    }.getOrElse { e ->
                        logW("launchPurchase: reconcile after ITEM_ALREADY_OWNED failed", e)
                        "You already have a pending purchase. Open the gold shop again to finish syncing."
                    }
                    if (msg != null) {
                        _uiState.update { it.copy(userMessage = msg) }
                    } else {
                        _uiState.update {
                            it.copy(
                                userMessage = "Finishing a previous purchase — check your gold balance in a moment.",
                            )
                        }
                    }
                }
            }
            else -> {
                logW("launchPurchase failed: ${result.responseCode} ${result.debugMessage}")
                _uiState.update {
                    it.copy(userMessage = billingMessage(result.responseCode, result.debugMessage))
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
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                    "A purchase for this pack is still pending in Play. Reopen the gold shop to sync."
                else -> "Billing error ($code)."
            }
        }

    companion object {
        private const val TAG = "GoldShop"
    }
}
