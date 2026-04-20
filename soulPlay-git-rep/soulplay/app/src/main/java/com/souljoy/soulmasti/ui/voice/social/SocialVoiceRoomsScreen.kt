package com.souljoy.soulmasti.ui.voice.social

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun SocialVoiceRoomsScreen(
    viewModel: SocialVoiceRoomsViewModel,
    onOpenRoom: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entitlement by viewModel.entitlement.collectAsStateWithLifecycle()
    val myRoom by viewModel.myRoom.collectAsStateWithLifecycle()
    val friendRooms by viewModel.friendRooms.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var showRoomPurchaseConfirm by remember { mutableStateOf(false) }
    var showVipPurchaseConfirm by remember { mutableStateOf(false) }

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
                    ) {
                        Column(
                            modifier = Modifier
                                .background(Brush.horizontalGradient(listOf(Color(0xFF8B5CF6), Color(0xFF6366F1))))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Your Room", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (entitlement.hasPermanentRoom) "Unlocked permanently" else "Open room cost: 3000 coins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFEDE9FE),
                            )
                            Button(
                                onClick = {
                                    if (entitlement.hasPermanentRoom) onOpenRoom(myRoom?.roomId ?: return@Button)
                                    else showRoomPurchaseConfirm = true
                                },
                            ) {
                                Text(if (entitlement.hasPermanentRoom) "Open Your Room" else "Buy & Open (3000)")
                            }
                        }
                    }
                }
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Column(
                            modifier = Modifier
                                .background(Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899))))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFDE047))
                                Text("Subscribe to VIP", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = if (entitlement.vipActive) "Active until ${formatDate(entitlement.vipExpiresAt)}" else "Monthly plan: 6000 coins",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFF5D0FE),
                            )
                            if (!entitlement.vipActive) {
                                Button(onClick = { showVipPurchaseConfirm = true }) {
                                    Text("Unlock VIP (6000 / month)")
                                }
                            }
                        }
                    }
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF67E8F9), Color(0xFF34D399)))),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Create room", tint = Color.White)
                        }
                        Text("Create normal Room", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1F2937), fontWeight = FontWeight.Bold)
                        Text("Entry starts from 100 coins", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.95f)
                            .clickable(enabled = room.exists) {
                                viewModel.openFriendRoom(room.ownerUid) { onOpenRoom(room.ownerUid) }
                            },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0xFFE2E8F0)),
                                    )
                                    Column {
                                        Text(
                                            room.ownerName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF6366F1),
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text("${room.onlineCount} online", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                                    }
                                }
                                Text(room.ownerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                Text(
                                    text = if (room.exists) "Active now" else "Room not created yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280),
                                )
                            }
                            Text(
                                text = if (entitlement.vipActive) "Join free" else "Join now (100)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (entitlement.vipActive) Color(0xFF0F766E) else Color(0xFF4F46E5),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
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

