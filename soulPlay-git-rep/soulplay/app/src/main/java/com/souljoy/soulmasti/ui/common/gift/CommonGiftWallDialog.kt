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
import androidx.compose.runtime.remember
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
    items: List<GiftWallItem>,
    sending: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSend: (giftId: String) -> Unit,
) {
    if (!visible) return
    val configuration = LocalConfiguration.current
    val sheetMaxHeight = (configuration.screenHeightDp * 0.88f).dp
    val scrollAreaMaxHeight = (configuration.screenHeightDp * 0.52f).dp.coerceAtLeast(220.dp)
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
                                            enabled = enabled,
                                            onClick = { onSend(item.id) },
                                        )
                                    }
                                }
                            }
                        }
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
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                                contentPadding = ButtonDefaults.ContentPadding,
                                modifier = Modifier.defaultMinSize(minWidth = 120.dp, minHeight = 48.dp),
                            ) {
                                Text(
                                    text = "Close",
                                    color = Color(0xFF334155),
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

@Composable
private fun GiftWallCard(
    item: GiftWallItem,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
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
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.priceCoins} 🪙",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF64748B)
            )
        }
    }
}
