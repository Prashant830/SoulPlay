package com.souljoy.soulmasti.data.billing

import android.app.Activity
import android.app.Application
import android.util.Log
import com.souljoy.soulmasti.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Google Play Billing for coin one-time products. [startConnection] must succeed before query/launch.
 */
class PlayBillingRepository(
    private val app: Application,
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null

    private fun logD(message: String) {
        Log.d(TAG, message)
    }

    private fun logW(message: String, throwable: Throwable? = null) {
        if (throwable != null) Log.w(TAG, message, throwable) else Log.w(TAG, message)
    }

    private val _purchaseEvents = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 8)
    val purchaseEvents = _purchaseEvents.asSharedFlow()

    sealed class PurchaseResult {
        data class Success(val purchases: List<Purchase>) : PurchaseResult()
        data object UserCanceled : PurchaseResult()
        data class Error(val code: Int, val debugMessage: String) : PurchaseResult()
    }

    fun startConnection(onReady: (Boolean) -> Unit) {
        if (billingClient?.isReady == true) {
            logD("startConnection: already ready")
            onReady(true)
            return
        }
        if (billingClient == null) {
            logD("startConnection: creating BillingClient")
            billingClient = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases()
                .build()
        }
        logD("startConnection: connecting…")
        billingClient!!.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val ok = billingResult.responseCode == BillingClient.BillingResponseCode.OK
                logD(
                    "onBillingSetupFinished ok=$ok code=${billingResult.responseCode} msg=${billingResult.debugMessage}",
                )
                if (!ok) {
                    logW("Billing setup failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                }
                onReady(ok)
            }

            override fun onBillingServiceDisconnected() {
                logW("onBillingServiceDisconnected")
            }
        })
    }

    suspend fun awaitConnection(): Boolean = suspendCoroutine { cont ->
        startConnection { ok -> cont.resume(ok) }
    }

    /**
     * Returns owned in-app purchases that are not yet consumed (e.g. slow test card completed after leaving the shop).
     * Per Google Play guidelines, call this on billing setup and when resuming the app, not only [onPurchasesUpdated].
     */
    suspend fun queryPurchasesInApp(): List<Purchase> = suspendCoroutine { cont ->
        val bc = billingClient
        if (bc == null || !bc.isReady) {
            logW("queryPurchasesInApp: billing not ready")
            cont.resume(emptyList())
            return@suspendCoroutine
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        bc.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                logW(
                    "queryPurchasesInApp: code=${billingResult.responseCode} msg=${billingResult.debugMessage}",
                )
                cont.resume(emptyList())
            } else {
                val list = purchases ?: emptyList()
                logD("queryPurchasesInApp: count=${list.size}")
                list.forEach { p ->
                    logD(
                        "  purchase products=${p.products} state=${p.purchaseState} acknowledged=${p.isAcknowledged}",
                    )
                }
                cont.resume(list)
            }
        }
    }

    fun queryCoinProducts(callback: (BillingResult, List<ProductDetails>) -> Unit) {
        val bc = billingClient
        if (bc == null || !bc.isReady) {
            logW("queryCoinProducts: billing not ready (clientNull=${bc == null})")
            callback(
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                    .setDebugMessage("Billing not ready")
                    .build(),
                emptyList(),
            )
            return
        }
        logD("queryCoinProducts: querying ${InAppCoinProducts.ALL_PRODUCT_IDS.size} SKUs: ${InAppCoinProducts.ALL_PRODUCT_IDS}")
        val productList = InAppCoinProducts.ALL_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        bc.queryProductDetailsAsync(params) { billingResult, details ->
            val list = details ?: emptyList()
            logD(
                "queryProductDetailsAsync result code=${billingResult.responseCode} msg=${billingResult.debugMessage} count=${list.size}",
            )
            list.forEach { p ->
                val price = p.oneTimePurchaseOfferDetails?.formattedPrice ?: "?"
                val coins = InAppCoinProducts.coinsFromProductDetails(p)
                logD("  product id=${p.productId} name=${p.name} title=${p.title} price=$price parsedCoins=$coins")
            }
            if (list.size < InAppCoinProducts.ALL_PRODUCT_IDS.size) {
                val found = list.map { it.productId }.toSet()
                val missing = InAppCoinProducts.ALL_PRODUCT_IDS.filter { it !in found }
                logW("queryCoinProducts: missing SKUs (not returned by Play): $missing")
            }
            val sorted = list.sortedBy { p ->
                InAppCoinProducts.coinsFromProductDetails(p) ?: Long.MAX_VALUE
            }
            callback(billingResult, sorted)
        }
    }

    suspend fun queryCoinProductsSuspend(): Pair<BillingResult, List<ProductDetails>> =
        suspendCoroutine { cont ->
            queryCoinProducts { result, list -> cont.resume(result to list) }
        }

    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails): BillingResult {
        val bc = billingClient
            ?: return BillingResult.newBuilder()
                .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                .build()
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build(),
        )
        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        val launchResult = bc.launchBillingFlow(activity, billingFlowParams)
        logD(
            "launchBillingFlow productId=${productDetails.productId} result=${launchResult.responseCode} msg=${launchResult.debugMessage}",
        )
        if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
            logW("launchBillingFlow failed: ${launchResult.debugMessage}")
        }
        return launchResult
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        logD(
            "onPurchasesUpdated code=${billingResult.responseCode} msg=${billingResult.debugMessage} purchaseCount=${purchases?.size ?: 0}",
        )
        purchases?.forEach { p ->
            logD(
                "  purchase products=${p.products} state=${p.purchaseState} acknowledged=${p.isAcknowledged} orderId=${p.orderId}",
            )
        }
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    _purchaseEvents.tryEmit(PurchaseResult.Success(purchases.toList()))
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                logD("onPurchasesUpdated: user canceled")
                _purchaseEvents.tryEmit(PurchaseResult.UserCanceled)
            }
            else -> {
                logW("onPurchasesUpdated: error ${billingResult.responseCode} ${billingResult.debugMessage}")
                _purchaseEvents.tryEmit(
                    PurchaseResult.Error(
                        billingResult.responseCode,
                        billingResult.debugMessage.orEmpty(),
                    ),
                )
            }
        }
    }

    suspend fun acknowledgePurchase(purchase: Purchase): BillingResult = suspendCoroutine { cont ->
        val bc = billingClient
        if (bc == null) {
            cont.resume(
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                    .build(),
            )
            return@suspendCoroutine
        }
        if (purchase.isAcknowledged) {
            cont.resume(
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.OK)
                    .build(),
            )
            return@suspendCoroutine
        }
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        bc.acknowledgePurchase(params) { result ->
            logD("acknowledgePurchase code=${result.responseCode} msg=${result.debugMessage}")
            cont.resume(result)
        }
    }

    suspend fun consumePurchase(purchase: Purchase): BillingResult = suspendCoroutine { cont ->
        val bc = billingClient
        if (bc == null) {
            cont.resume(
                BillingResult.newBuilder()
                    .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
                    .build(),
            )
            return@suspendCoroutine
        }
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        bc.consumeAsync(params) { result, _ ->
            logD("consumeAsync code=${result.responseCode} msg=${result.debugMessage}")
            cont.resume(result)
        }
    }

    fun endConnection() {
        logD("endConnection")
        billingClient?.endConnection()
        billingClient = null
    }

    companion object {
        private const val TAG = "PlayBilling"
    }
}
