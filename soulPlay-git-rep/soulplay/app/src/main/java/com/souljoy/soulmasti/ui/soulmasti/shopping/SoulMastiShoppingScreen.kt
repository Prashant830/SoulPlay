package com.souljoy.soulmasti.ui.soulmasti.shopping

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.souljoy.soulmasti.ui.rewards.AdMobRewardedAds
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SoulMastiShoppingScreen(
    onBack: () -> Unit,
    onOpenAdsTab: () -> Unit,
    vm: SoulMastiShoppingViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snack = remember { SnackbarHostState() }
    var selectedItem by remember { mutableStateOf<VoiceRoomShopItem?>(null) }
    var selectedDays by remember { mutableStateOf(1) }
    var bagItem by remember { mutableStateOf<VoiceRoomPurchase?>(null) }
    var showInventoryDialog by remember { mutableStateOf(false) }
    var loadingAdKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state.message) {
        val msg = state.message ?: return@LaunchedEffect
        snack.showSnackbar(msg)
        vm.consumeMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = "Shop",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .weight(1f),
                )
                BadgedBox(
                    badge = {
                        if (state.purchases.isNotEmpty()) {
                            Badge { Text(state.purchases.size.coerceAtMost(99).toString()) }
                        }
                    },
                ) {
                    IconButton(onClick = { showInventoryDialog = true }) {
                        Icon(Icons.Filled.Inventory2, contentDescription = "Inventory")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BalanceHeader(state.coins, state.soul)
            ShopTabs(selected = state.selectedTab, onSelect = vm::selectTab)

            if (state.selectedTab == 0) {
                CategoryRows(items = state.items, onItemClick = {
                    selectedItem = it
                    selectedDays = 1
                })
            } else {
                AdsTab(
                    loadingAdKey = loadingAdKey,
                    onWatchCoins100 = {
                        if (loadingAdKey != null) return@AdsTab
                        val activity = context.findActivity() ?: return@AdsTab
                        loadingAdKey = "coins_100"
                        AdMobRewardedAds.loadAndShow(
                            activity = activity,
                            onEarnedReward = { vm.applyShopAdReward(coins = 100L, souls = 0L) },
                            onDone = { loadingAdKey = null },
                            onFailed = { msg -> vm.showMessage(msg) },
                        )
                    },
                    onWatchCoins300 = {
                        if (loadingAdKey != null) return@AdsTab
                        val activity = context.findActivity() ?: return@AdsTab
                        loadingAdKey = "coins_300"
                        AdMobRewardedAds.loadAndShow(
                            activity = activity,
                            onEarnedReward = { vm.applyShopAdReward(coins = 300L, souls = 0L) },
                            onDone = { loadingAdKey = null },
                            onFailed = { msg -> vm.showMessage(msg) },
                        )
                    },
                    onWatchSouls20 = {
                        if (loadingAdKey != null) return@AdsTab
                        val activity = context.findActivity() ?: return@AdsTab
                        loadingAdKey = "souls_20"
                        AdMobRewardedAds.loadAndShow(
                            activity = activity,
                            onEarnedReward = { vm.applyShopAdReward(coins = 0L, souls = 20L) },
                            onDone = { loadingAdKey = null },
                            onFailed = { msg -> vm.showMessage(msg) },
                        )
                    },
                    onWatchSouls60 = {
                        if (loadingAdKey != null) return@AdsTab
                        val activity = context.findActivity() ?: return@AdsTab
                        loadingAdKey = "souls_60"
                        AdMobRewardedAds.loadAndShow(
                            activity = activity,
                            onEarnedReward = { vm.applyShopAdReward(coins = 0L, souls = 60L) },
                            onDone = { loadingAdKey = null },
                            onFailed = { msg -> vm.showMessage(msg) },
                        )
                    },
                    onWatchCombo = {
                        if (loadingAdKey != null) return@AdsTab
                        val activity = context.findActivity() ?: return@AdsTab
                        loadingAdKey = "combo_50_10"
                        AdMobRewardedAds.loadAndShow(
                            activity = activity,
                            onEarnedReward = { vm.applyShopAdReward(coins = 50L, souls = 10L) },
                            onDone = { loadingAdKey = null },
                            onFailed = { msg -> vm.showMessage(msg) },
                        )
                    },
                    onOpenDailyTasks = onOpenAdsTab,
                )
            }
        }
    }

    selectedItem?.let { item ->
        val totalCost = item.oneDayPrice * selectedDays.toLong()
        AlertDialog(
            onDismissRequest = { selectedItem = null },
            title = { Text(item.title, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FullPreviewCard(item = item)
                    DurationSelector(
                        selectedDays = selectedDays,
                        onSelect = { selectedDays = it },
                    )
                    Text(
                        text = "Price: $totalCost coins",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.buy(item, selectedDays)
                    selectedItem = null
                }) { Text("Buy") }
            },
            dismissButton = {
                Button(onClick = { selectedItem = null }) { Text("Close") }
            },
        )
    }

    bagItem?.let { purchase ->
        AlertDialog(
            onDismissRequest = { bagItem = null },
            title = { Text(purchase.itemTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FullPreviewCard(
                        item = VoiceRoomShopItem(
                            id = purchase.itemId,
                            title = purchase.itemTitle,
                            category = purchase.category,
                            previewRes = 0,
                            fullPreviewRes = 0,
                        ),
                    )
                    Text("Introduction", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                    Text(
                        "Use this purchased item to apply it in Voice Room.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF4B5563),
                    )
                    Text(
                        "Validity: ${formatDateTime(purchase.expiresAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.usePurchasedItem(purchase)
                    bagItem = null
                }) { Text("Use") }
            },
            dismissButton = { Button(onClick = { bagItem = null }) { Text("Close") } },
        )
    }

    if (showInventoryDialog) {
        AlertDialog(
            onDismissRequest = { showInventoryDialog = false },
            title = { Text("My Inventory", fontWeight = FontWeight.Bold) },
            text = {
                if (state.purchases.isEmpty()) {
                    Text("No purchased items yet.")
                } else {
                    val invRows = ((state.purchases.size + 2) / 3).coerceAtLeast(1)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                    ) {
                        items(state.purchases, key = { it.itemId }) { p ->
                            Card(
                                modifier = Modifier.clickable {
                                    showInventoryDialog = false
                                    bagItem = p
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FD)),
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    MiniPreview(
                                        item = VoiceRoomShopItem(
                                            id = p.itemId,
                                            title = p.itemTitle,
                                            category = p.category,
                                            previewRes = 0,
                                            fullPreviewRes = 0,
                                        ),
                                    )
                                    Text(
                                        text = p.itemTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                    )
                                    Text(
                                        text = "🪙 700/day",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFE0A800),
                                    )
                                    Text(
                                        text = if (state.activeByCategory[p.category] == p.itemId) "Using" else "Tap",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (state.activeByCategory[p.category] == p.itemId) Color(0xFF16A34A) else Color(0xFF2563EB),
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showInventoryDialog = false }) { Text("Close") }
            },
        )
    }
}

@Composable
private fun BalanceHeader(coins: Long, soul: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF6F7FB), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Balance", fontWeight = FontWeight.SemiBold)
        Column(horizontalAlignment = Alignment.End) {
            Text("🪙 $coins", fontWeight = FontWeight.Bold, color = Color(0xFFE0A800))
            Text("👻 $soul", style = MaterialTheme.typography.labelMedium, color = Color(0xFF7C3AED))
        }
    }
}

@Composable
private fun ShopTabs(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F3FA), RoundedCornerShape(12.dp))
            .padding(4.dp),
    ) {
        TabChip("Voice Room", selected == 0) { onSelect(0) }
        TabChip("Ads", selected == 1) { onSelect(1) }
    }
}

@Composable
private fun RowScope.TabChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .background(if (selected) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun CategoryRows(items: List<VoiceRoomShopItem>, onItemClick: (VoiceRoomShopItem) -> Unit) {
    CategoryBlock("Room Background", items.filter { it.category == ShopCategory.ROOM_BACKGROUND }, onItemClick)
}

@Composable
private fun CategoryBlock(
    title: String,
    items: List<VoiceRoomShopItem>,
    onItemClick: (VoiceRoomShopItem) -> Unit,
) {
    val rows = ((items.size + 2) / 3).coerceAtLeast(1)
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        userScrollEnabled = false,
        modifier = Modifier.wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(top = 6.dp),
    ) {
        items(items, key = { it.id }) { item ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(4.dp))
                    MiniPreview(item = item)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.title,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                    )
                    Text("🪙 700/day", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE0A800))
                }
            }
        }
    }
}

@Composable
private fun MiniPreview(item: VoiceRoomShopItem) {
    val colors = previewColors(item.id, full = false)
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(Brush.linearGradient(colors), RoundedCornerShape(8.dp)),
    )
}

@Composable
private fun FullPreviewCard(item: VoiceRoomShopItem) {
    val colors = previewColors(item.id, full = true)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Brush.verticalGradient(colors), RoundedCornerShape(12.dp)),
    ) {
        Text(
            text = "Welcome to SoulMast",
            color = Color.White.copy(alpha = 0.92f),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(start = 14.dp, top = 14.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp, top = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(4) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF6B7280), RoundedCornerShape(999.dp)),
                )
            }
        }

        Text(
            text = "🏆",
            color = Color(0xFFFBBF24),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 12.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 78.dp, end = 12.dp)
                .size(54.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🏆", color = Color(0xFFFBBF24))
        }
        Text(
            text = "🏆",
            color = Color(0xFFFBBF24),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 98.dp),
        )
        Text(
            text = "🏆",
            color = Color(0xFFFBBF24),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 98.dp),
        )

        Text(
            text = "System: Welcome to SoulMast Voice\nRoom.",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        )
    }
}

@Composable
private fun DurationSelector(selectedDays: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DurationCard(days = 1, price = 700L, selected = selectedDays == 1, onSelect = onSelect)
        DurationCard(days = 3, price = 2100L, selected = selectedDays == 3, onSelect = onSelect)
        DurationCard(days = 7, price = 4900L, selected = selectedDays == 7, onSelect = onSelect)
    }
}

@Composable
private fun RowScope.DurationCard(days: Int, price: Long, selected: Boolean, onSelect: (Int) -> Unit) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable { onSelect(days) },
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFE9F3FF) else Color(0xFFF7F7F7),
        ),
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$days day", fontWeight = FontWeight.Bold)
            Text("🪙 $price", color = Color(0xFFE0A800))
        }
    }
}

@Composable
private fun BagPreview(
    purchases: List<VoiceRoomPurchase>,
    activeByCategory: Map<ShopCategory, String>,
    onItemClick: (VoiceRoomPurchase) -> Unit,
) {
    Text("Bag Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    if (purchases.isEmpty()) {
        Text("No active voice room items", color = Color.Gray)
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(purchases.size) { index ->
            val p = purchases[index]
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FD)),
                modifier = Modifier.clickable { onItemClick(p) },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(p.itemTitle, fontWeight = FontWeight.SemiBold)
                        Text("Validity: ${formatDateTime(p.expiresAt)}", style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        text = if (activeByCategory[p.category] == p.itemId) "Using" else "${p.durationDays}d",
                        color = if (activeByCategory[p.category] == p.itemId) Color(0xFF16A34A) else Color(0xFF1E88E5),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdsTab(
    loadingAdKey: String?,
    onWatchCoins100: () -> Unit,
    onWatchCoins300: () -> Unit,
    onWatchSouls20: () -> Unit,
    onWatchSouls60: () -> Unit,
    onWatchCombo: () -> Unit,
    onOpenDailyTasks: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF0B1220), Color(0xFF172554), Color(0xFF0F172A)),
                        ),
                    )
                    .padding(14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Voice Room Ads Rewards",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Watch ads to earn instant coins and souls in shop.",
                        color = Color.White.copy(alpha = 0.86f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        AdRewardCard(
            title = "Coin Boost",
            subtitle = "Quick refill for purchases",
            rewardText = "+100 Coins",
            adLoading = loadingAdKey == "coins_100",
            onClick = onWatchCoins100,
            colors = listOf(Color(0xFF7C3AED), Color(0xFF2563EB)),
        )
        AdRewardCard(
            title = "Mega Coin Boost",
            subtitle = "Big coin top-up from ads",
            rewardText = "+300 Coins",
            adLoading = loadingAdKey == "coins_300",
            onClick = onWatchCoins300,
            colors = listOf(Color(0xFF6D28D9), Color(0xFF0EA5E9)),
        )
        AdRewardCard(
            title = "Soul Spark",
            subtitle = "Earn souls for profile growth",
            rewardText = "+20 Souls",
            adLoading = loadingAdKey == "souls_20",
            onClick = onWatchSouls20,
            colors = listOf(Color(0xFFBE185D), Color(0xFF7C3AED)),
        )
        AdRewardCard(
            title = "Soul Storm",
            subtitle = "Higher soul reward from one ad",
            rewardText = "+60 Souls",
            adLoading = loadingAdKey == "souls_60",
            onClick = onWatchSouls60,
            colors = listOf(Color(0xFF9D174D), Color(0xFF4C1D95)),
        )
        AdRewardCard(
            title = "Combo Pack",
            subtitle = "Balanced reward",
            rewardText = "+50 Coins + 10 Souls",
            adLoading = loadingAdKey == "combo_50_10",
            onClick = onWatchCombo,
            colors = listOf(Color(0xFFB45309), Color(0xFFDB2777)),
        )

        Button(onClick = onOpenDailyTasks, modifier = Modifier.fillMaxWidth()) {
            Text("Open Daily Tasks")
        }
    }
}

@Composable
private fun AdRewardCard(
    title: String,
    subtitle: String,
    rewardText: String,
    adLoading: Boolean,
    onClick: () -> Unit,
    colors: List<Color>,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White.copy(alpha = 0.88f), style = MaterialTheme.typography.bodySmall)
                Text(rewardText, color = Color(0xFFFDE68A), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onClick, enabled = !adLoading) {
                Text(if (adLoading) "Loading..." else "Watch")
            }
        }
    }
}

private fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(timestamp))

private fun previewColors(itemId: String, full: Boolean): List<Color> = when (itemId) {
    "bg_golden_brick" -> listOf(Color(0xFF8A5A12), Color(0xFFD8A33A), Color(0xFF5F3B0A))
    "bg_emperor_castle" -> listOf(Color(0xFF2B1A06), Color(0xFF8A6424), Color(0xFFF2D287))
    "bg_summer_breeze" -> listOf(Color(0xFFA78BFA), Color(0xFFF0ABFC), Color(0xFF7DD3FC))
    "bg_mountains" -> listOf(Color(0xFF0F172A), Color(0xFF334155), Color(0xFF67E8F9))
    "bg_nature" -> listOf(Color(0xFF064E3B), Color(0xFF10B981), Color(0xFF86EFAC))
    "bg_mahel" -> listOf(Color(0xFF2A0A00), Color(0xFF7C2D12), Color(0xFFFFB347))
    "bg_midnight_blue" -> listOf(Color(0xFF0B1027), Color(0xFF1E3A8A), Color(0xFF60A5FA))
    "bg_rose_dream" -> listOf(Color(0xFF831843), Color(0xFFDB2777), Color(0xFFF9A8D4))
    "bg_crystal_ice" -> listOf(Color(0xFF0F172A), Color(0xFF0891B2), Color(0xFFBAE6FD))
    "bg_sunset_gold" -> listOf(Color(0xFF7C2D12), Color(0xFFF59E0B), Color(0xFFFDE68A))
    "bg_royal_purple" -> listOf(Color(0xFF2E1065), Color(0xFF7E22CE), Color(0xFFC4B5FD))
    "bg_neon_city" -> listOf(Color(0xFF020617), Color(0xFF0EA5E9), Color(0xFFA78BFA))
    else -> listOf(Color(0xFF64748B), Color(0xFF94A3B8))
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

