package com.souljoy.soulmasti.ui.common.gift

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.souljoy.soulmasti.domain.gift.GiftCatalog

/** Each horizontal “page” is this many gift cards stacked vertically (fixed height strip). */
private const val GiftWallRows = 3

/** Card column width — keeps tiles readable while scrolling horizontally. */
private val GiftWallColumnWidth = 112.dp

/** Fits three stacked cards + spacing (no vertical scroll; overflow scrolls horizontally). */
private val GiftWallStripHeight = 260.dp

data class GiftWallItem(
    val id: String,
    val label: String,
    val priceCoins: Long,
)

fun defaultGiftWallItems(): List<GiftWallItem> = listOf(
    GiftWallItem(GiftCatalog.ROSE, GiftCatalog.displayLabel(GiftCatalog.ROSE), GiftCatalog.priceCoinsOrNull(GiftCatalog.ROSE) ?: 0L),
    GiftWallItem(GiftCatalog.CAKE, GiftCatalog.displayLabel(GiftCatalog.CAKE), GiftCatalog.priceCoinsOrNull(GiftCatalog.CAKE) ?: 0L),
    GiftWallItem(GiftCatalog.TEDDY, GiftCatalog.displayLabel(GiftCatalog.TEDDY), GiftCatalog.priceCoinsOrNull(GiftCatalog.TEDDY) ?: 0L),
    GiftWallItem(GiftCatalog.ROCKET, GiftCatalog.displayLabel(GiftCatalog.ROCKET), GiftCatalog.priceCoinsOrNull(GiftCatalog.ROCKET) ?: 0L),
    GiftWallItem(GiftCatalog.KISS, GiftCatalog.displayLabel(GiftCatalog.KISS), GiftCatalog.priceCoinsOrNull(GiftCatalog.KISS) ?: 0L),
    GiftWallItem(GiftCatalog.LOVE, GiftCatalog.displayLabel(GiftCatalog.LOVE), GiftCatalog.priceCoinsOrNull(GiftCatalog.LOVE) ?: 0L),
    GiftWallItem(GiftCatalog.RING, GiftCatalog.displayLabel(GiftCatalog.RING), GiftCatalog.priceCoinsOrNull(GiftCatalog.RING) ?: 0L),
    GiftWallItem(GiftCatalog.EROS, GiftCatalog.displayLabel(GiftCatalog.EROS), GiftCatalog.priceCoinsOrNull(GiftCatalog.EROS) ?: 0L),
    GiftWallItem(GiftCatalog.CHAMPAGNE, GiftCatalog.displayLabel(GiftCatalog.CHAMPAGNE), GiftCatalog.priceCoinsOrNull(GiftCatalog.CHAMPAGNE) ?: 0L),
    GiftWallItem(GiftCatalog.FIRE_CRACKER, GiftCatalog.displayLabel(GiftCatalog.FIRE_CRACKER), GiftCatalog.priceCoinsOrNull(GiftCatalog.FIRE_CRACKER) ?: 0L),
    GiftWallItem(GiftCatalog.CROWN, GiftCatalog.displayLabel(GiftCatalog.CROWN), GiftCatalog.priceCoinsOrNull(GiftCatalog.CROWN) ?: 0L),
    GiftWallItem(GiftCatalog.SPARKLE, GiftCatalog.displayLabel(GiftCatalog.SPARKLE), GiftCatalog.priceCoinsOrNull(GiftCatalog.SPARKLE) ?: 0L),
    GiftWallItem(GiftCatalog.DRAGON, GiftCatalog.displayLabel(GiftCatalog.DRAGON), GiftCatalog.priceCoinsOrNull(GiftCatalog.DRAGON) ?: 0L),
)

@Composable
fun GiftWallDialog(
    visible: Boolean,
    recipientDisplayName: String,
    availableCoins: Long? = null,
    items: List<GiftWallItem>,
    sending: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSend: (giftId: String, selectedCount: Int) -> Unit,
) {
    if (!visible) return
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.88f).dp
    val scrollAreaMaxHeight = (configuration.screenHeightDp * 0.52f).dp.coerceAtLeast(220.dp)
    var selectedGiftId by remember(visible, items) { mutableStateOf(items.firstOrNull()?.id) }
    var selectedCount by remember(visible) { mutableStateOf(1) }
    val comboOptions = remember { listOf(1, 5, 10, 20, 50) }
    val selectedGift = items.firstOrNull { it.id == selectedGiftId }
    val totalCoins = (selectedGift?.priceCoins ?: 0L) * selectedCount.toLong()
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                color = Color(0xFFFFFBFC),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = sheetMaxHeight)
                    .clickable(enabled = false) {}
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp)
                            .heightIn(max = scrollAreaMaxHeight)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Send a gift",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                )
                                Text(
                                    text = "To $recipientDisplayName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF64748B),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    availableCoins?.let { coins ->
                                        Text(
                                            text = "You have: $coins coins",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF0F766E),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Text(
                                    text = if (selectedGift == null) "Total: 0 coins" else "Total: $totalCoins coins",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (errorMessage != null) {
                            Text(
                                text = errorMessage,
                                color = Color(0xFFDC2626),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                            )
                        }
                        val columns = remember(items) { items.chunked(GiftWallRows) }
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(GiftWallStripHeight),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            items(
                                items = columns,
                                key = { col -> col.joinToString("|") { it.id } },
                            ) { columnItems ->
                                Column(
                                    modifier = Modifier.width(GiftWallColumnWidth),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    columnItems.forEach { item ->
                                        val enabled = !sending
                                        GiftWallCard(
                                            item = item,
                                            selected = item.id == selectedGiftId,
                                            enabled = enabled,
                                            onClick = { selectedGiftId = item.id },
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Combo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                        ) {
                            items(comboOptions) { combo ->
                                val selected = combo == selectedCount
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (selected) Color(0xFFFCE7F3) else Color.White,
                                    tonalElevation = 1.dp,
                                    shadowElevation = if (selected) 3.dp else 1.dp,
                                    modifier = Modifier.clickable(enabled = !sending) { selectedCount = combo }
                                ) {
                                    Text(
                                        text = "x$combo",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (selected) Color(0xFF9D174D) else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = Color(0xFFEC4899),
                                strokeWidth = 3.dp,
                            )
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                                    contentPadding = ButtonDefaults.ContentPadding,
                                    modifier = Modifier.defaultMinSize(minWidth = 104.dp, minHeight = 48.dp),
                                ) {
                                    Text(
                                        text = "Close",
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Button(
                                    onClick = {
                                        selectedGiftId?.let { onSend(it, selectedCount) }
                                    },
                                    enabled = selectedGiftId != null,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEC4899)),
                                    contentPadding = ButtonDefaults.ContentPadding,
                                    modifier = Modifier.defaultMinSize(minWidth = 136.dp, minHeight = 48.dp),
                                ) {
                                    val selected = items.firstOrNull { it.id == selectedGiftId }
                                    Text(
                                        text = if (selected == null) {
                                            "Send gift"
                                        } else {
                                            "Send ${selected.label} x$selectedCount"
                                        },
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GiftWallCard(
    item: GiftWallItem,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Color(0xFFFCE7F3) else Color.White,
        tonalElevation = 1.dp,
        shadowElevation = if (selected) 4.dp else 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = if (selected) Color(0xFF9D174D) else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.priceCoins} 🪙",
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color(0xFF9D174D) else Color(0xFF64748B)
            )
        }
    }
}
