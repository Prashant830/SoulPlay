package com.souljoy.soulmasti.domain.gift

/**
 * Default gift ids and prices (coins). Override or extend via RTDB `giftCatalog/{giftId}` in the repository if you add server-side catalog later.
 */
object GiftCatalog {
    const val ROSE: String = "rose"
    const val CAKE: String = "cake"
    const val TEDDY: String = "teddy"
    const val ROCKET: String = "rocket"
    const val KISS: String = "kiss"
    const val LOVE: String = "love"
    const val RING: String = "ring"
    const val EROS: String = "eros"
    const val CHAMPAGNE: String = "champagne"
    const val FIRE_CRACKER: String = "fire cracker"
    const val CROWN: String = "crown"
    const val SPARKLE: String = "sparkle"
    /** Current id for the top-tier gift (was `"dream palace"`). */
    const val DRAGON: String = "dragon"

    private val defaultPrices: Map<String, Long> = mapOf(
        ROSE to 10L,
        CAKE to 50L,
        TEDDY to 100L,
        ROCKET to 200L,
        KISS to 300L,
        LOVE to 500L,
        RING to 800L,
        EROS to 1_200L,
        CHAMPAGNE to 1_800L,
        FIRE_CRACKER to 2_400L,
        CROWN to 6_000L,
        SPARKLE to 10_000L,
        DRAGON to 130_000L,
        // Legacy gift id still stored in older messages / RTDB rows
        "dream palace" to 130_000L,
    )

    fun priceCoinsOrNull(giftId: String): Long? = defaultPrices[giftId]

    /** Short label with emoji for UI (gift wall, banners). */
    fun displayLabel(giftId: String): String = when (giftId) {
        ROSE -> "🌹 Rose"
        CAKE -> "🎂 Cake"
        TEDDY -> "🧸 Teddy"
        ROCKET -> "🚀 Rocket"
        KISS -> "💋 Kiss"
        LOVE -> "💕 Love"
        RING -> "💍 Ring"
        EROS -> "💘 Eros"
        CHAMPAGNE -> "🍾 Champagne"
        FIRE_CRACKER -> "🧨 Fire Crackers"
        CROWN -> "👑 Crown"
        SPARKLE -> "✨ Sparkle"
        DRAGON -> "🐉 Dragon"
        "dream palace" -> "🐉 Dragon"
        else -> giftId
    }
}
