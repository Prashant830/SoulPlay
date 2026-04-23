package com.souljoy.soulmasti.ui.soulmasti.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.souljoy.soulmasti.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class VoiceRoomShopItem(
    val id: String,
    val title: String,
    val category: ShopCategory,
    val previewRes: Int,
    val fullPreviewRes: Int,
    val oneDayPrice: Long = 700L,
)

enum class ShopCategory { ROOM_BACKGROUND }

data class VoiceRoomPurchase(
    val itemId: String,
    val itemTitle: String,
    val category: ShopCategory,
    val durationDays: Int,
    val purchasedAt: Long,
    val expiresAt: Long,
)

data class SoulMastiShoppingUiState(
    val loading: Boolean = true,
    val selectedTab: Int = 0,
    val coins: Long = 0L,
    val soul: Long = 0L,
    val items: List<VoiceRoomShopItem> = emptyList(),
    val purchases: List<VoiceRoomPurchase> = emptyList(),
    val activeByCategory: Map<ShopCategory, String> = emptyMap(),
    val message: String? = null,
)

class SoulMastiShoppingViewModel(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SoulMastiShoppingUiState(
            items = defaultItems(),
        ),
    )
    val uiState: StateFlow<SoulMastiShoppingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun refresh() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: run {
                _uiState.update { it.copy(loading = false, message = "Please login first") }
                return@launch
            }
            val userSnap = runCatching {
                database.reference.child("users").child(uid).get().await()
            }.getOrNull()
            val coins = userSnap?.child("totalWinnings")?.getValue(Long::class.java) ?: 0L
            val soul = userSnap?.child("soul")?.getValue(Long::class.java) ?: 0L

            val now = System.currentTimeMillis()
            val itemMap = _uiState.value.items.associateBy { it.id }
            val purchases = userSnap?.child("voiceRoomShopV1")?.child("purchases")
                ?.children
                ?.mapNotNull { node ->
                    val itemId = node.key.orEmpty()
                    val item = itemMap[itemId] ?: return@mapNotNull null
                    val expiresAt = node.child("expiresAt").getValue(Long::class.java) ?: return@mapNotNull null
                    if (expiresAt <= now) return@mapNotNull null
                    VoiceRoomPurchase(
                        itemId = itemId,
                        itemTitle = item.title,
                        category = item.category,
                        durationDays = (node.child("durationDays").getValue(Long::class.java) ?: 1L).toInt(),
                        purchasedAt = node.child("purchasedAt").getValue(Long::class.java) ?: now,
                        expiresAt = expiresAt,
                    )
                }
                ?.sortedBy { it.expiresAt }
                .orEmpty()
            val activeByCategory = mutableMapOf<ShopCategory, String>()
            val activeNode = userSnap?.child("voiceRoomShopV1")?.child("active")
            val purchaseVoiceColorUsing = userSnap?.child("voiceRoomShopV1")?.child("purchaseVoiceColorUsing")?.getValue(Boolean::class.java) == true
            val itemId = activeNode?.child(ShopCategory.ROOM_BACKGROUND.name)?.child("itemId")?.getValue(String::class.java).orEmpty()
            if (purchaseVoiceColorUsing && itemId.isNotBlank() && purchases.any { it.itemId == itemId }) {
                activeByCategory[ShopCategory.ROOM_BACKGROUND] = itemId
            }

            _uiState.update {
                it.copy(
                    loading = false,
                    coins = coins,
                    soul = soul,
                    purchases = purchases,
                    activeByCategory = activeByCategory,
                )
            }
        }
    }

    fun buy(item: VoiceRoomShopItem, durationDays: Int) {
        val cost = item.oneDayPrice * durationDays.toLong()
        viewModelScope.launch {
            val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return@launch
            val currentCoins = _uiState.value.coins
            if (currentCoins < cost) {
                _uiState.update { it.copy(message = "Not enough coins") }
                return@launch
            }
            val now = System.currentTimeMillis()
            val oldExpiry = _uiState.value.purchases.firstOrNull { it.itemId == item.id }?.expiresAt ?: now
            val base = if (oldExpiry > now) oldExpiry else now
            val newExpiry = base + durationDays * DAY_MS
            val newCoins = currentCoins - cost
            val updates = mapOf<String, Any>(
                "totalWinnings" to newCoins,
                "voiceRoomShopV1/purchases/${item.id}/durationDays" to durationDays,
                "voiceRoomShopV1/purchases/${item.id}/purchasedAt" to now,
                "voiceRoomShopV1/purchases/${item.id}/expiresAt" to newExpiry,
                "voiceRoomShopV1/purchases/${item.id}/category" to item.category.name,
                "voiceRoomShopV1/purchases/${item.id}/title" to item.title,
            )
            runCatching {
                database.reference.child("users").child(uid).updateChildren(updates).await()
            }.onSuccess {
                _uiState.update { it.copy(message = "Purchased ${item.title}. Open Bag and tap Use to apply.") }
                refresh()
            }.onFailure {
                _uiState.update { it.copy(message = it.message ?: "Purchase failed") }
            }
        }
    }

    fun usePurchasedItem(purchase: VoiceRoomPurchase) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return@launch
            val now = System.currentTimeMillis()
            if (purchase.expiresAt <= now) {
                _uiState.update { it.copy(message = "Item expired") }
                return@launch
            }
            val updates = mapOf<String, Any>(
                "voiceRoomShopV1/active/${purchase.category.name}/itemId" to purchase.itemId,
                "voiceRoomShopV1/active/${purchase.category.name}/appliedAt" to now,
                "voiceRoomShopV1/purchaseVoiceColorUsing" to true,
            )
            runCatching {
                database.reference.child("users").child(uid).updateChildren(updates).await()
            }.onSuccess {
                _uiState.update { it.copy(message = "${purchase.itemTitle} applied") }
                refresh()
            }.onFailure {
                _uiState.update { it.copy(message = it.message ?: "Could not apply item") }
            }
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    fun applyShopAdReward(coins: Long, souls: Long) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return@launch
            val userRef = database.reference.child("users").child(uid)
            val currentCoins = _uiState.value.coins
            val currentSoul = runCatching {
                userRef.child("soul").get().await().getValue(Long::class.java) ?: 0L
            }.getOrDefault(0L)
            val updates = mutableMapOf<String, Any>(
                "totalWinnings" to (currentCoins + coins),
                "soul" to (currentSoul + souls),
                "shopAdsV1/lastRewardAt" to System.currentTimeMillis(),
            )
            runCatching { userRef.updateChildren(updates).await() }
                .onSuccess {
                    val msg = buildString {
                        if (coins > 0) append("+$coins coins")
                        if (souls > 0) {
                            if (isNotEmpty()) append(" + ")
                            append("$souls souls")
                        }
                        if (isEmpty()) append("Reward claimed")
                    }
                    _uiState.update { it.copy(coins = currentCoins + coins, soul = currentSoul + souls, message = msg) }
                }
                .onFailure {
                    _uiState.update { it.copy(message = it.message ?: "Could not add reward") }
                }
        }
    }

    private fun defaultItems(): List<VoiceRoomShopItem> = listOf(
        VoiceRoomShopItem(
            id = "bg_golden_brick",
            title = "Golden Brick",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_golden_brick,
            fullPreviewRes = R.drawable.shop_bg_golden_brick,
        ),
        VoiceRoomShopItem(
            id = "bg_emperor_castle",
            title = "Emperor Castle",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_emperor_castle,
            fullPreviewRes = R.drawable.shop_bg_emperor_castle,
        ),
        VoiceRoomShopItem(
            id = "bg_summer_breeze",
            title = "Summer Breeze",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_summer_breeze,
            fullPreviewRes = R.drawable.shop_bg_summer_breeze,
        ),
        VoiceRoomShopItem(
            id = "bg_mountains",
            title = "Mountains",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_summer_breeze,
            fullPreviewRes = R.drawable.shop_bg_summer_breeze,
        ),
        VoiceRoomShopItem(
            id = "bg_nature",
            title = "Nature",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_summer_breeze,
            fullPreviewRes = R.drawable.shop_bg_summer_breeze,
        ),
        VoiceRoomShopItem(
            id = "bg_mahel",
            title = "Mahel",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_golden_brick,
            fullPreviewRes = R.drawable.shop_bg_golden_brick,
        ),
        VoiceRoomShopItem(
            id = "bg_midnight_blue",
            title = "Midnight Blue",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_summer_breeze,
            fullPreviewRes = R.drawable.shop_bg_summer_breeze,
        ),
        VoiceRoomShopItem(
            id = "bg_rose_dream",
            title = "Rose Dream",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_summer_breeze,
            fullPreviewRes = R.drawable.shop_bg_summer_breeze,
        ),
        VoiceRoomShopItem(
            id = "bg_crystal_ice",
            title = "Crystal Ice",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_summer_breeze,
            fullPreviewRes = R.drawable.shop_bg_summer_breeze,
        ),
        VoiceRoomShopItem(
            id = "bg_sunset_gold",
            title = "Sunset Gold",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_golden_brick,
            fullPreviewRes = R.drawable.shop_bg_golden_brick,
        ),
        VoiceRoomShopItem(
            id = "bg_royal_purple",
            title = "Royal Purple",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_summer_breeze,
            fullPreviewRes = R.drawable.shop_bg_summer_breeze,
        ),
        VoiceRoomShopItem(
            id = "bg_neon_city",
            title = "Neon City",
            category = ShopCategory.ROOM_BACKGROUND,
            previewRes = R.drawable.shop_bg_summer_breeze,
            fullPreviewRes = R.drawable.shop_bg_summer_breeze,
        ),
    )

    companion object {
        private const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}

