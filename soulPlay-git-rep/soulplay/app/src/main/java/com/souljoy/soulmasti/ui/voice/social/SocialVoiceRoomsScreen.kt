package com.souljoy.soulmasti.ui.voice.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.souljoy.soulmasti.R
import android.widget.Toast

@Composable
fun SocialVoiceRoomsScreen(
    viewModel: SocialVoiceRoomsViewModel,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val entitlement by viewModel.entitlement.collectAsStateWithLifecycle()
    val myRoom by viewModel.myRoom.collectAsStateWithLifecycle()
    val friendRooms by viewModel.friendRooms.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val joinConfirm by viewModel.joinConfirm.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var showRoomPurchaseConfirm by remember { mutableStateOf(false) }
    var showVipPurchaseConfirm by remember { mutableStateOf(false) }
    var badgePreviewRes by remember { mutableStateOf<Int?>(null) }
    var joinFeeRoom by remember { mutableStateOf<FriendRoomCardUi?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F8))
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TopTab(label = "Your Room", selected = tab == 0, modifier = Modifier.weight(1f)) { tab = 0 }
            TopTab(label = "Friends' Rooms", selected = tab == 1, modifier = Modifier.weight(1f)) { tab = 1 }
        }
        Spacer(modifier = Modifier.height(14.dp))
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (tab == 0) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (entitlement.hasPermanentRoom) {
                            val roomName = myRoom?.roomName?.ifBlank { "My Voice Room" } ?: "My Voice Room"
                            val totalSoul = myRoom?.contributionTotalSoul.orEmpty().values.sum()
                            val roomLevel = roomLevelForSoulSummary(totalSoul)
                            val currentLevelMinSoul = RoomLevelDefsSummary.getOrNull(roomLevel - 1)?.requiredSoul ?: 0L
                            val nextLevelSoulRaw = RoomLevelDefsSummary.getOrNull(roomLevel)?.requiredSoul ?: currentLevelMinSoul
                            val levelProgress = ((totalSoul - currentLevelMinSoul).toFloat() /
                                (nextLevelSoulRaw - currentLevelMinSoul).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                            val nextLevelSoulDisplay = if (roomLevel <= 1) nextLevelSoulRaw else nextLevelSoulRaw + 1_000L
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF4C1D95), Color(0xFF6D28D9), Color(0xFF4338CA)),
                                        ),
                                    )
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0x33FFFFFF))
                                            .border(1.dp, Color(0x66FFFFFF), RoundedCornerShape(16.dp)),
                                    ) {
                                        if (myRoom?.roomCoverUrl?.isNotBlank() == true) {
                                            AsyncImage(
                                                model = myRoom?.roomCoverUrl,
                                                contentDescription = "Room image",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Brush.linearGradient(listOf(Color(0xFF22D3EE), Color(0xFF8B5CF6)))),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    roomName.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(5.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0xAA111827))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        ) {
                                            Text("LIVE", color = Color(0xFF67E8F9), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Text("Your Room", style = MaterialTheme.typography.labelMedium, color = Color(0xFFEDE9FE))
                                        Text(roomName, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text(roomLevelTitleSummary(roomLevel), style = MaterialTheme.typography.labelMedium, color = Color(0xFFC7D2FE))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clickable { badgePreviewRes = roomLevelBadgeIconResSummary(roomLevel) }
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x1AFFFFFF))),
                                            )
                                            .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(14.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                        ) {
                                            Icon(
                                                painter = painterResource(id = roomLevelBadgeIconResSummary(roomLevel)),
                                                contentDescription = "Room badge",
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(24.dp),
                                            )
                                            Text(
                                                "Badge",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFEDE9FE),
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Level $roomLevel", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    LinearProgressIndicator(
                                        progress = { levelProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(7.dp)
                                            .clip(RoundedCornerShape(999.dp)),
                                        color = Color(0xFFFDE047),
                                        trackColor = Color(0x4DFFFFFF),
                                    )
                                    Text(
                                        "$totalSoul / $nextLevelSoulDisplay souls",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFEDE9FE),
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Button(
                                        onClick = { viewModel.openMyRoom { roomId -> onOpenRoom(roomId) } },
                                    ) {
                                        Text("Open Your Room")
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF4B5563), Color(0xFF1F2937))))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0x26FFFFFF)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            Icons.Filled.Lock,
                                            contentDescription = null,
                                            tint = Color.White,
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Text("Your Room", style = MaterialTheme.typography.labelMedium, color = Color(0xFFE5E7EB))
                                        Text("Room Locked", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                        Text("Open room cost: 3000 coins", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD1D5DB))
                                    }
                                }
                                Text(
                                    text = "Unlock your personal voice room and make it your own space.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD1D5DB),
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                    Button(onClick = { showRoomPurchaseConfirm = true }) {
                                        Text("Buy & Open (3000)")
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
                            },
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF3B0764), Color(0xFF7E22CE), Color(0xFFDB2777)),
                                    ),
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFDE047))
                                    Text("VIP Lounge", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0x33FFFFFF))
                                        .border(1.dp, Color(0x55FFFFFF), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFFDE68A), modifier = Modifier.size(14.dp))
                                    Text("Locked", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFDE68A), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Text(
                                text = if (entitlement.vipActive) "Active until ${formatDate(entitlement.vipExpiresAt)}" else "Premium monthly plan: 6000 coins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF5D0FE),
                            )
                            Text(
                                text = "Exclusive badges, elite entry, and premium room styling.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFE9D5FF),
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Button(onClick = { Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show() }) {
                                    Text("Coming Soon")
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(friendRooms) { room ->
                    val roomLevel = roomLevelForSoulSummary(room.totalSoul)
                    val currentLevelMinSoul = RoomLevelDefsSummary.getOrNull(roomLevel - 1)?.requiredSoul ?: 0L
                    val nextLevelSoulRaw = RoomLevelDefsSummary.getOrNull(roomLevel)?.requiredSoul ?: currentLevelMinSoul
                    val roomProgress = if (!room.exists) 0f else {
                        ((room.totalSoul - currentLevelMinSoul).toFloat() /
                            (nextLevelSoulRaw - currentLevelMinSoul).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    }
                    val nextLevelSoulDisplay = if (roomLevel <= 1) nextLevelSoulRaw else nextLevelSoulRaw + 1_000L
                    val remainingToNext = (nextLevelSoulDisplay - room.totalSoul).coerceAtLeast(0L)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(14.dp),
                            )
                            .fillMaxWidth()
                            .aspectRatio(0.95f)
                            .clickable(enabled = room.exists) {
                                viewModel.checkFriendRoomJoinRequirement(
                                    friendUid = room.ownerUid,
                                    onAlreadyInRoom = { onOpenRoom(room.ownerUid) },
                                    onNeedsJoinFeeConfirm = { joinFeeRoom = room },
                                )
                            },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!room.roomPhoto.isNullOrBlank()) {
                                        AsyncImage(
                                            model = room.roomPhoto,
                                            contentDescription = "Room image",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Text(
                                            room.roomName.take(1).uppercase(),
                                            color = Color(0xFF475569),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        room.roomName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF111827),
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                    )
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(
                                                if (room.exists) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(if (room.exists) Color(0xFF16A34A) else Color(0xFFD97706)),
                                        )
                                        Text(
                                            text = if (room.exists) "LIVE" else "NOT OPENED",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (room.exists) Color(0xFF166534) else Color(0xFF92400E),
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(id = roomLevelBadgeIconResSummary(roomLevel)),
                                    contentDescription = "Room badge",
                                    tint = Color.Unspecified,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable { badgePreviewRes = roomLevelBadgeIconResSummary(roomLevel) },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${room.onlineCount} online",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    if (room.exists) "Tap to join" else "Buy room",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (room.exists) Color(0xFF4338CA) else Color(0xFFB45309),
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Text(
                                        room.ownerName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4338CA),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Text(
                                    "Level $roomLevel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                LinearProgressIndicator(
                                    progress = { roomProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(999.dp)),
                                    color = Color(0xFF6366F1),
                                    trackColor = Color(0xFFE2E8F0),
                                )
                                Text(
                                    "Room progress ${(roomProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                )
                                Text(
                                    "Total souls: ${room.totalSoul}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                )
                                Text(
                                    "Next level: $nextLevelSoulDisplay (need $remainingToNext)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    badgePreviewRes?.let { badgeRes ->
        AlertDialog(
            onDismissRequest = { badgePreviewRes = null },
            title = { Text("Room Badge Preview") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = badgeRes),
                        contentDescription = "Badge preview",
                        modifier = Modifier.size(180.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { badgePreviewRes = null }) { Text("Close") }
            },
        )
    }

    joinFeeRoom?.let { room ->
        AlertDialog(
            onDismissRequest = { joinFeeRoom = null },
            title = { Text("Join room?") },
            text = { Text("Joining fee: 50 coins. Continue to join ${room.roomName}?") },
            confirmButton = {
                TextButton(onClick = {
                    joinFeeRoom = null
                    viewModel.openFriendRoom(room.ownerUid) { onOpenRoom(room.ownerUid) }
                }) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { joinFeeRoom = null }) { Text("Cancel") }
            },
        )
    }

    if (showRoomPurchaseConfirm) {
        AlertDialog(
            onDismissRequest = { showRoomPurchaseConfirm = false },
            title = { Text("Buy voice room?") },
            text = { Text("This will deduct 3000 coins and unlock your room permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    showRoomPurchaseConfirm = false
                    viewModel.purchaseOwnRoom { onOpenRoom(myRoom?.roomId ?: return@purchaseOwnRoom) }
                }) { Text("Buy") }
            },
            dismissButton = {
                TextButton(onClick = { showRoomPurchaseConfirm = false }) { Text("Cancel") }
            },
        )
    }

    if (showVipPurchaseConfirm) {
        AlertDialog(
            onDismissRequest = { showVipPurchaseConfirm = false },
            title = { Text("Buy VIP monthly?") },
            text = { Text("This will deduct 6000 coins and activate/extend VIP for 30 days.") },
            confirmButton = {
                TextButton(onClick = {
                    showVipPurchaseConfirm = false
                    viewModel.purchaseVip()
                }) { Text("Buy VIP") }
            },
            dismissButton = {
                TextButton(onClick = { showVipPurchaseConfirm = false }) { Text("Cancel") }
            },
        )
    }

    joinConfirm?.let { prompt ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissJoinConfirm() },
            title = { Text("Leave room?") },
            text = {
                Text(
                    "You need to leave this room first. Continue?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmSwitchAndJoin { roomId ->
                        onOpenRoom(roomId)
                    }
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissJoinConfirm() }) { Text("No") }
            },
        )
    }
}

@Composable
private fun TopTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) Color(0xFFEC4899) else Color(0xFF9CA3AF),
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(120.dp)
                .background(if (selected) Color(0xFFEC4899) else Color.Transparent),
        )
    }
}

private fun formatDate(ts: Long?): String {
    if (ts == null || ts <= 0L) return "-"
    return java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.US).format(java.util.Date(ts))
}

private data class RoomLevelDefSummary(
    val level: Int,
    val requiredSoul: Long,
    val iconRes: Int,
)

private val RoomLevelDefsSummary = listOf(
    RoomLevelDefSummary(1, 0L, R.drawable.voice_room_level_1),
    RoomLevelDefSummary(2, 8_000L, R.drawable.voice_room_level_2),
    RoomLevelDefSummary(3, 30_000L, R.drawable.voice_room_level_3),
    RoomLevelDefSummary(4, 100_000L, R.drawable.voice_room_level_4),
    RoomLevelDefSummary(5, 300_000L, R.drawable.voice_room_level_5),
    RoomLevelDefSummary(6, 800_000L, R.drawable.voice_room_level_6),
    RoomLevelDefSummary(7, 2_000_000L, R.drawable.voice_room_level_7),
    RoomLevelDefSummary(8, 4_800_000L, R.drawable.voice_room_level_8),
    RoomLevelDefSummary(9, 11_000_000L, R.drawable.voice_room_level_9),
    RoomLevelDefSummary(10, 24_000_000L, R.drawable.voice_room_level_10),
)

private fun roomLevelForSoulSummary(totalSoul: Long): Int {
    val matched = RoomLevelDefsSummary.lastOrNull { totalSoul >= it.requiredSoul }
    return matched?.level ?: 1
}

private fun roomLevelTitleSummary(level: Int): String {
    return "Level $level"
}

private fun roomLevelBadgeIconResSummary(level: Int): Int {
    return RoomLevelDefsSummary.firstOrNull { it.level == level }?.iconRes ?: R.drawable.voice_room_level_1
}


