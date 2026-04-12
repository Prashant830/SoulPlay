package com.souljoy.soulmasti.data.billing

import com.android.billingclient.api.ProductDetails

/**
 * One-time in-app product IDs registered in Play Console.
 *
 * **Coin amount** is read from Play Billing [ProductDetails]:
 * - Prefer [ProductDetails.getName] when it is all digits (matches Play "Product name", e.g. `400`).
 * - Otherwise first number in `name`, then leading number in `title` (often `400 (App name)`).
 *
 * Set each product’s **name** in Play Console to the total gold (digits only) for reliable parsing.
 */
object InAppCoinProducts {

    const val COIN_1 = "souljoy.soulmasti.coin.1"
    const val COIN_2 = "souljoy.soulmasti.coin.2"
    const val COIN_3 = "souljoy.soulmasti.coin.3"
    const val COIN_4 = "souljoy.soulmasti.coin.4"
    const val COIN_5 = "souljoy.soulmasti.coin.5"
    const val COIN_6 = "souljoy.soulmasti.coin.6"

    val ALL_PRODUCT_IDS: List<String> = listOf(
        COIN_1,
        COIN_2,
        COIN_3,
        COIN_4,
        COIN_5,
        COIN_6,
    )

    /**
     * Coin pack size from Play Store product metadata (not hardcoded per SKU).
     */
    fun coinsFromProductDetails(details: ProductDetails): Long? {
        val name = details.name?.trim().orEmpty()
        if (name.isNotEmpty() && name.all { it.isDigit() }) {
            return name.toLongOrNull()?.takeIf { it > 0L }
        }
        Regex("(\\d+)").find(name)?.groupValues?.getOrNull(1)?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?.let { return it }

        val title = details.title?.trim().orEmpty()
        Regex("^\\s*(\\d+)").find(title)?.groupValues?.getOrNull(1)?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?.let { return it }
        Regex("(\\d+)").find(title)?.groupValues?.getOrNull(1)?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?.let { return it }

        return null
    }

    /** Optional marketing line per tier (still keyed by product id). */
    fun bonusLabelForProductId(productId: String): String? = when (productId) {
        COIN_1 -> "+30 bonus"
        COIN_2 -> "+60 bonus"
        COIN_3 -> "+180 bonus"
        COIN_4 -> "+300 bonus"
        COIN_5 -> "+1000 bonus"
        COIN_6 -> "+5000 bonus"
        else -> null
    }
}
