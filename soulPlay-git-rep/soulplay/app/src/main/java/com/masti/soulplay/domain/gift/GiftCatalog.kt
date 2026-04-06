package com.masti.soulplay.domain.gift

/**
 * Default gift ids and prices (coins). Override or extend via RTDB `giftCatalog/{giftId}` in the repository if you add server-side catalog later.
 */
object GiftCatalog {
    const val ROSE: String = "rose"
    const val CAKE: String = "cake"
    const val ROCKET: String = "rocket"

    private val defaultPrices: Map<String, Long> = mapOf(
        ROSE to 10L,
        CAKE to 50L,
        ROCKET to 200L,
    )

    fun priceCoinsOrNull(giftId: String): Long? = defaultPrices[giftId]

    /** Short label with emoji for UI (gift wall, banners). */
    fun displayLabel(giftId: String): String = when (giftId) {
        ROSE -> "🌹 Rose"
        CAKE -> "🎂 Cake"
        ROCKET -> "🚀 Rocket"
        else -> giftId
    }
}
