package com.souljoy.soulmasti.ui.voiceroom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
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
import com.souljoy.soulmasti.ui.common.gift.GiftWallDialog as CommonGiftWallDialog
import com.souljoy.soulmasti.ui.common.gift.GiftWallItem as CommonGiftWallItem
import com.souljoy.soulmasti.ui.common.gift.defaultGiftWallItems as commonDefaultGiftWallItems
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
typealias GiftWallItem = CommonGiftWallItem
fun defaultGiftWallItems(): List<GiftWallItem> = commonDefaultGiftWallItems()

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
) = CommonGiftWallDialog(
    visible = visible,
    recipientDisplayName = recipientDisplayName,
    items = items,
    sending = sending,
    errorMessage = errorMessage,
    onDismiss = onDismiss,
    onSend = onSend,
)
