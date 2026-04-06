package com.masti.soulplay.ui.voiceroom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.masti.soulplay.domain.gift.GiftCatalog
import kotlinx.coroutines.delay

data class GiftBannerUi(
    val eventId: String,
    val senderDisplay: String,
    val giftDisplayName: String,
    val recipientDisplay: String?,
    val coins: Long,
    val receiverCoins: Long,
) {
    val titleLine: String get() = "$senderDisplay sent $giftDisplayName"
    val subtitleLine: String get() = recipientDisplay?.let { "To $it" } ?: ""
}

private val GiftAccent = Color(0xFFEC4899)
private val GiftSheetBg = Color(0xFFFFFBFC)

data class GiftWallItem(
    val id: String,
    val label: String,
    val priceCoins: Long,
)

fun defaultGiftWallItems(): List<GiftWallItem> = listOf(
    GiftWallItem(GiftCatalog.ROSE, GiftCatalog.displayLabel(GiftCatalog.ROSE), GiftCatalog.priceCoinsOrNull(GiftCatalog.ROSE) ?: 0L),
    GiftWallItem(GiftCatalog.CAKE, GiftCatalog.displayLabel(GiftCatalog.CAKE), GiftCatalog.priceCoinsOrNull(GiftCatalog.CAKE) ?: 0L),
    GiftWallItem(GiftCatalog.ROCKET, GiftCatalog.displayLabel(GiftCatalog.ROCKET), GiftCatalog.priceCoinsOrNull(GiftCatalog.ROCKET) ?: 0L),
)

@Composable
fun GiftBannerOverlay(viewModel: VoiceRoomViewModel) {
    var banner by remember { mutableStateOf<GiftBannerUi?>(null) }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.giftBannerEvents.collect { banner = it }
    }
    LaunchedEffect(banner?.eventId) {
        val id = banner?.eventId ?: return@LaunchedEffect
        revealed = false
        delay(1600)
        // Reveal banner, then hide after delay.
        if (banner?.eventId == id) revealed = true
        delay(2200)
        if (banner?.eventId == id) banner = null
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = banner != null,
            enter = fadeIn() + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + scaleOut(targetScale = 0.92f)
        ) {
            val b = banner ?: return@AnimatedVisibility
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = Color.White.copy(alpha = 0.97f),
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.CardGiftcard,
                        contentDescription = null,
                        tint = GiftAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = b.titleLine,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (b.subtitleLine.isNotBlank()) {
                            Text(
                                text = b.subtitleLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "🪙 ${b.coins}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                        if (revealed) {
                            if (b.receiverCoins > 0L && !b.recipientDisplay.isNullOrBlank()) {
                                Text(
                                    text = "🪙 +${b.receiverCoins} gold to ${b.recipientDisplay}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF16A34A),
                                    textAlign = TextAlign.End
                                )
                                Text(
                                    text = "Tap profile → Gift wall to see all gifts",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.End
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = GiftAccent
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Revealing…",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEC4899),
                                    textAlign = TextAlign.End
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
                color = GiftSheetBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .clickable(enabled = false) {}
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Send a gift",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "To $recipientDisplayName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.heightIn(min = 160.dp, max = 220.dp)
                    ) {
                        items(items, key = { it.id }) { item ->
                            val enabled = !sending
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                tonalElevation = 1.dp,
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = enabled) { onSend(item.id) }
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
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = GiftAccent,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Button(
                                onClick = onDismiss,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                                contentPadding = ButtonDefaults.ContentPadding
                            ) {
                                Text("Close", color = Color(0xFF334155))
                            }
                        }
                    }
                }
            }
        }
    }
}
