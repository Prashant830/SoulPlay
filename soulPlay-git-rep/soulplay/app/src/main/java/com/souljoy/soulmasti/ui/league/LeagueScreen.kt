package com.souljoy.soulmasti.ui.league

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.souljoy.soulmasti.ui.common.SoulBadgeTiers
import com.souljoy.soulmasti.ui.common.soulBadgeIconForSoul

@Composable
fun LeagueScreen(
    viewModel: LeagueViewModel,
    onOpenUserProfile: (String) -> Unit,
    onOpenRoom: (String) -> Unit,
    onOpenRewardInbox: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val userToday by viewModel.userToday.collectAsStateWithLifecycle()
    val userWeek by viewModel.userWeek.collectAsStateWithLifecycle()
    val userTotal by viewModel.userTotal.collectAsStateWithLifecycle()
    val roomToday by viewModel.roomToday.collectAsStateWithLifecycle()
    val roomWeek by viewModel.roomWeek.collectAsStateWithLifecycle()
    val roomTotal by viewModel.roomTotal.collectAsStateWithLifecycle()
    val myUid = viewModel.myUid

    var typeTab by remember { mutableIntStateOf(0) } // 0 users, 1 rooms
    var periodTab by remember { mutableIntStateOf(0) } // 0 today, 1 week, 2 total

    val users = when (periodTab) {
        0 -> userToday
        1 -> userWeek
        else -> userTotal
    }
    val rooms = when (periodTab) {
        0 -> roomToday
        1 -> roomWeek
        else -> roomTotal
    }

    val rewardRule = when (periodTab) {
        0 -> "Today: Top 3 get 10% coin + 5% soul"
        1 -> "Week: Top 5 get 10% coin + 5% soul"
        else -> "Total (month): Top 10 get 10% coin + 5% soul"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF3F4F6)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFFD8B4FE), Color(0xFF9333EA))))
                .statusBarsPadding()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(config.leagueName, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(config.arenaTitle, color = Color.White.copy(alpha = 0.9f))
                }
                Box(modifier = Modifier.weight(1f))
                IconButton(onClick = onOpenRewardInbox, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Reward Inbox",
                        tint = Color.White,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SegmentSwitch(
                selected = typeTab,
                labels = listOf("Users", "Voice Rooms"),
                onSelected = { typeTab = it },
            )
            SegmentSwitch(
                selected = periodTab,
                labels = listOf("Today", "Week", "Total"),
                onSelected = { periodTab = it },
            )

            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF3E8FF)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(18.dp) // reduce size here
                            )                        }
                        Text("Prize Pool", color = Color(0xFF111827), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Earn more soul and coins to reach Top 10 in user and voice room league. And get extra benefits",
                        color = Color(0xFF667085),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        text = rewardRule,
                        color = Color(0xFF6D28D9),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (typeTab == 0) {
                UserRankingList(rankings = users, myUid = myUid, onOpenUserProfile = onOpenUserProfile)
            } else {
                RoomRankingList(rankings = rooms, onOpenRoom = onOpenRoom)
            }
        }
    }
}

@Composable
private fun SegmentSwitch(
    selected: Int,
    labels: List<String>,
    onSelected: (Int) -> Unit,
) {
    Surface(shape = RoundedCornerShape(999.dp), color = Color(0xFFE5E7EB), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            labels.forEachIndexed { idx, label ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelected(idx) },
                    shape = RoundedCornerShape(999.dp),
                    color = if (idx == selected) Color(0xFF7C3AED) else Color.Transparent,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (label.contains("User")) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = if (idx == selected) Color.White else Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                        } else if (label.contains("Room")) {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = if (idx == selected) Color.White else Color(0xFF6B7280), modifier = Modifier.size(16.dp))
                        }
                        if (label.contains("User") || label.contains("Room")) {
                            Box(Modifier.width(6.dp))
                        }
                        Text(label, color = if (idx == selected) Color.White else Color(0xFF374151), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun UserRankingList(
    rankings: List<LeagueUserRow>,
    myUid: String?,
    onOpenUserProfile: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(rankings) { index, row ->
                val me = row.uid == myUid
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (me) Color(0xFFF5F3FF) else Color(0xFFF9FAFB))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onOpenUserProfile(row.uid) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("#${index + 1}", modifier = Modifier.width(38.dp), color = Color(0xFF6B7280))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (row.photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = row.photoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(row.username.take(1).ifBlank { "U" }, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.username.ifBlank { row.uid.take(6) }, fontWeight = FontWeight.SemiBold)
                        val showBadge = row.profileTotalSoul >= SoulBadgeTiers.first().minSoul
                        if (showBadge || me) {
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (showBadge) {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(id = soulBadgeIconForSoul(row.profileTotalSoul)),
                                        contentDescription = "Profile badge",
                                        modifier = Modifier.heightIn(min = 14.dp, max = 14.dp),
                                    )
                                }
                                if (me) {
                                    Text("Me", color = Color(0xFF7C3AED), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_soul_cute_ghost),
                            contentDescription = "Soul",
                            modifier = Modifier.size(16.dp),
                        )
                        Text(formatPoints(row.soul), color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomRankingList(
    rankings: List<LeagueRoomRow>,
    onOpenRoom: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(rankings) { index, row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9FAFB))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onOpenRoom(row.roomId) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("#${index + 1}", modifier = Modifier.width(38.dp), color = Color(0xFF6B7280))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFEDE9FE)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (row.ownerPhotoUrl.isNotBlank()) {
                            AsyncImage(
                                model = row.ownerPhotoUrl,
                                contentDescription = "Owner profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(Icons.Filled.Mic, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(18.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(row.roomName.ifBlank { "Room ${row.roomId.takeLast(6)}" }, fontWeight = FontWeight.SemiBold)
                        Text("ID: ${row.roomId.takeLast(6)}", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.ic_soul_cute_ghost),
                            contentDescription = "Soul",
                            modifier = Modifier.size(16.dp),
                        )
                        Text(formatPoints(row.soul), color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightTile(
    title: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
            Text(title, color = Color(0xFF111827), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, color = Color(0xFF667085), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun compact(value: Long): String = when {
    value >= 1_000_000L -> String.format("%.1fM", value / 1_000_000f)
    value >= 1_000L -> String.format("%.1fk", value / 1_000f)
    else -> value.toString()
}

private fun formatPoints(value: Long): String = "%,d".format(value)

private fun rankBadgeRes(index: Int): Int = when (index) {
    0 -> R.drawable.rank_bronze_3
    1 -> R.drawable.rank_bronze_2
    2 -> R.drawable.rank_bronze_1
    else -> R.drawable.rank_bronze_1
}
