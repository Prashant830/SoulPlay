package com.souljoy.soulmasti.ui.common

import com.souljoy.soulmasti.R

data class SoulBadgeTier(
    val minSoul: Long,
    val name: String,
    val soulLabel: String,
    val iconRes: Int,
)

val SoulBadgeTiers = listOf(
    SoulBadgeTier(minSoul = 1_200L, name = "Bronze I", soulLabel = "1,200", iconRes = R.drawable.rank_bronze_1),
    SoulBadgeTier(minSoul = 2_400L, name = "Bronze II", soulLabel = "2,400", iconRes = R.drawable.rank_bronze_2),
    SoulBadgeTier(minSoul = 3_600L, name = "Bronze III", soulLabel = "3,600", iconRes = R.drawable.rank_bronze_3),
    SoulBadgeTier(minSoul = 4_800L, name = "Silver I", soulLabel = "4,800", iconRes = R.drawable.rank_silver_1),
    SoulBadgeTier(minSoul = 6_000L, name = "Silver II", soulLabel = "6,000", iconRes = R.drawable.rank_silver_2),
    SoulBadgeTier(minSoul = 7_200L, name = "Silver III", soulLabel = "7,200", iconRes = R.drawable.rank_silver_3),
    SoulBadgeTier(minSoul = 8_400L, name = "Gold I", soulLabel = "8,400", iconRes = R.drawable.rank_gold_1),
    SoulBadgeTier(minSoul = 9_600L, name = "Gold II", soulLabel = "9,600", iconRes = R.drawable.rank_gold_2),
    SoulBadgeTier(minSoul = 12_000L, name = "Gold III", soulLabel = "12k", iconRes = R.drawable.rank_gold_3),
    SoulBadgeTier(minSoul = 40_000L, name = "Platinum I", soulLabel = "40k", iconRes = R.drawable.rank_platinum_1),
    SoulBadgeTier(minSoul = 60_000L, name = "Platinum II", soulLabel = "60k", iconRes = R.drawable.rank_platinum_2),
    SoulBadgeTier(minSoul = 90_000L, name = "Platinum III", soulLabel = "90k", iconRes = R.drawable.rank_platinum_3),
    SoulBadgeTier(minSoul = 180_000L, name = "Ace I", soulLabel = "180k", iconRes = R.drawable.rank_ace_1),
    SoulBadgeTier(minSoul = 500_000L, name = "Ace II", soulLabel = "500k", iconRes = R.drawable.rank_ace_2),
    SoulBadgeTier(minSoul = 950_000L, name = "Ace III", soulLabel = "950k", iconRes = R.drawable.rank_ace_3),
    SoulBadgeTier(minSoul = 4_000_000L, name = "Ace IV", soulLabel = "4m", iconRes = R.drawable.rank_ace_4),
    SoulBadgeTier(minSoul = 10_000_000L, name = "Ace V", soulLabel = "10m", iconRes = R.drawable.rank_ace_5),
    SoulBadgeTier(minSoul = 21_000_000L, name = "Ace VI", soulLabel = "21m", iconRes = R.drawable.rank_ace_6),
    SoulBadgeTier(minSoul = 51_000_000L, name = "Conqueror", soulLabel = "51m", iconRes = R.drawable.rank_conqueror),
)

fun soulBadgeTierForSoul(soul: Long): SoulBadgeTier =
    SoulBadgeTiers.lastOrNull { soul >= it.minSoul } ?: SoulBadgeTiers.first()

fun soulBadgeIconForSoul(soul: Long): Int = soulBadgeTierForSoul(soul).iconRes
