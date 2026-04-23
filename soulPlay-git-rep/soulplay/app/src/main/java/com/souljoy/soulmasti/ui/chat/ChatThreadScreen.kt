package com.souljoy.soulmasti.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.souljoy.soulmasti.R
import com.souljoy.soulmasti.ui.common.SoulplayChatThreadTopBar
import com.souljoy.soulmasti.ui.common.gift.GiftCelebrationOverlayHost
import com.souljoy.soulmasti.ui.common.gift.rememberGiftCelebrationQueue
import com.souljoy.soulmasti.ui.voice.game.GiftWallDialog
import com.souljoy.soulmasti.ui.voice.game.defaultGiftWallItems
import kotlinx.coroutines.launch

// Me = left, blue. Other user = right, gray (per product spec).
private val BubbleMeBlue = Color(0xFF2563EB)
private val BubbleMeOnBlue = Color(0xFFFFFFFF)
private val BubbleOtherGray = Color(0xFFE2E8F0)
private val BubbleOtherOnGray = Color(0xFF0F172A)
private val GiftMineStart = Color(0xFF2563EB)
private val GiftMineEnd = Color(0xFF9333EA)
private val GiftOtherStart = Color(0xFFF8FAFC)
private val GiftOtherEnd = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    viewModel: ChatThreadViewModel,
    onBack: () -> Unit,
    pendingRoomInvite: PendingRoomInvite? = null,
    onPendingRoomInviteConsumed: () -> Unit = {},
    onJoinVoiceRoom: (String) -> Unit = {},
    onOpenPeerProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val peerName by viewModel.peerDisplayName.collectAsStateWithLifecycle()
    val peerPhotoUrl by viewModel.peerPhotoUrl.collectAsStateWithLifecycle()
    val myCoins by viewModel.myCoins.collectAsStateWithLifecycle()
    val sendError by viewModel.sendError.collectAsStateWithLifecycle()
    val myUid = viewModel.myUid
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    val sendGiftErrorText = stringResource(R.string.chat_send_gift_error)
    var showGiftDialog by remember { mutableStateOf(false) }
    var giftSending by remember { mutableStateOf(false) }
    var giftError by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    val giftCelebration = rememberGiftCelebrationQueue()
    /** Only messages created at/after this instant (minus skew) can play incoming gift FX (avoids replaying history). */
    val threadOpenedAtMs = remember { System.currentTimeMillis() }
    var giftFxBaselineReady by remember { mutableStateOf(false) }
    var giftFxKnownIds by remember { mutableStateOf(emptySet<String>()) }

    LaunchedEffect(messages, myUid) {
        val uid = myUid ?: return@LaunchedEffect
        if (!giftFxBaselineReady) {
            if (messages.isEmpty()) return@LaunchedEffect
            if (messages.size == 1) {
                val m = messages.first()
                giftFxKnownIds = setOf(m.id)
                giftFxBaselineReady = true
                if (m.fromUid != uid) {
                    val payload = parseGiftChatPayload(m.text)
                    if (payload != null) {
                        val looksLive =
                            m.createdAt != null && m.createdAt >= threadOpenedAtMs - 5_000L
                        if (looksLive) giftCelebration.enqueueIfFx(context, payload.giftId)
                    }
                }
                return@LaunchedEffect
            }
            giftFxKnownIds = messages.map { it.id }.toSet()
            giftFxBaselineReady = true
            return@LaunchedEffect
        }
        val prev = giftFxKnownIds
        val currentIds = messages.map { it.id }.toSet()
        val newMessages = messages.filter { it.id !in prev }
        giftFxKnownIds = currentIds
        for (msg in newMessages) {
            if (msg.fromUid == uid) continue
            val payload = parseGiftChatPayload(msg.text) ?: continue
            giftCelebration.enqueueIfFx(context, payload.giftId)
        }
    }

    LaunchedEffect(messages.size) {
        val lastIndex = messages.lastIndex
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    LaunchedEffect(pendingRoomInvite, viewModel.peerUid) {
        val invite = pendingRoomInvite ?: return@LaunchedEffect
        viewModel.sendRoomInvite(
            roomId = invite.roomId,
            roomName = invite.roomName,
            roomPhotoUrl = invite.roomPhotoUrl,
        )
        onPendingRoomInviteConsumed()
    }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            SoulplayChatThreadTopBar(
                peerName = peerName,
                peerPhotoUrl = peerPhotoUrl,
                onBack = onBack,
                onPeerProfileClick = { onOpenPeerProfile(viewModel.peerUid) },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            sendError?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.chat_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    val mine = myUid != null && msg.fromUid == myUid
                    val giftInfo = parseGiftChatPayload(msg.text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (mine) Arrangement.Start else Arrangement.End
                    ) {
                        if (giftInfo != null) {
                            GiftMessageBubble(
                                mine = mine,
                                info = giftInfo,
                                modifier = Modifier.widthIn(max = 300.dp)
                            )
                        } else {
                            val roomInvite = parseRoomInviteChatPayload(msg.text)
                            if (roomInvite != null) {
                                RoomInviteMessageBubble(
                                    info = roomInvite,
                                    modifier = Modifier.widthIn(max = 300.dp),
                                    onJoinClick = { onJoinVoiceRoom(roomInvite.roomId) },
                                )
                            } else {
                                Text(
                                    text = msg.text,
                                    modifier = Modifier
                                        .widthIn(max = 300.dp)
                                        .background(
                                            color = if (mine) BubbleMeBlue else BubbleOtherGray,
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (mine) 4.dp else 16.dp,
                                                bottomEnd = if (mine) 16.dp else 4.dp
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (mine) BubbleMeOnBlue else BubbleOtherOnGray
                                )
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.chat_input_placeholder)) },
                    maxLines = 4
                )
                IconButton(
                    onClick = {
                        giftError = null
                        showGiftDialog = true
                    }
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = stringResource(R.string.gift))
                }
                IconButton(
                    onClick = {
                        val t = draft.trim()
                        if (t.isNotEmpty()) {
                            viewModel.send(t)
                            draft = ""
                            viewModel.dismissSendError()
                        }
                    },
                    enabled = draft.trim().isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.send))
                }
            }
        }
    }

        GiftCelebrationOverlayHost(queue = giftCelebration)
    }

    GiftWallDialog(
        visible = showGiftDialog,
        recipientDisplayName = peerName,
        availableCoins = myCoins,
        items = remember { defaultGiftWallItems() },
        sending = giftSending,
        errorMessage = giftError,
        onDismiss = {
            showGiftDialog = false
            giftError = null
        },
        onSend = { giftId, selectedCount ->
            giftSending = true
            giftError = null
            scope.launch {
                val result = viewModel.sendGift(giftId, selectedCount)
                giftSending = false
                result.onSuccess {
                    showGiftDialog = false
                    giftCelebration.enqueueIfFx(context, giftId)
                }
                result.onFailure { e ->
                    giftError = e.message ?: sendGiftErrorText
                }
            }
        }
    )
}

data class PendingRoomInvite(
    val roomId: String,
    val roomName: String,
    val roomPhotoUrl: String?,
)

@Composable
private fun GiftMessageBubble(
    mine: Boolean,
    info: GiftChatPayload,
    modifier: Modifier = Modifier,
) {
    val bubbleTextColor = if (mine) Color.White else Color(0xFF0F172A)
    val bubbleSubTextColor = if (mine) Color(0xFFE0E7FF) else Color(0xFF334155)
    val giftShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (mine) 6.dp else 18.dp,
        bottomEnd = if (mine) 18.dp else 6.dp
    )
    Column(
        modifier = modifier
            .background(
                brush = if (mine) {
                    Brush.linearGradient(listOf(GiftMineStart, GiftMineEnd))
                } else {
                    Brush.linearGradient(listOf(GiftOtherStart, GiftOtherEnd))
                },
                shape = giftShape
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (mine) "🎁 Sent gift" else "🎉 Gift received",
                style = MaterialTheme.typography.labelSmall,
                color = bubbleSubTextColor
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Filled.CardGiftcard,
                contentDescription = null,
                tint = bubbleTextColor
            )
            Text(
                text = "${info.giftLabel} x${info.selectedCount}",
                style = MaterialTheme.typography.titleSmall,
                color = bubbleTextColor
            )
        }
        Text(
            text = "Gift value: ${info.giftCoins} coins",
            style = MaterialTheme.typography.bodySmall,
            color = bubbleSubTextColor
        )
        Text(
            text = if (mine) {
                "💰 Receiver gets: +${info.receiverCoins} coins"
            } else {
                "💰 You received: +${info.receiverCoins} coins"
            },
            style = MaterialTheme.typography.bodySmall,
            color = bubbleSubTextColor
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_soul_cute_ghost),
                contentDescription = "Soul",
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(16.dp),
            )
            Text(
                text = if (mine) {
                    "Receiver gets: +${info.receiverSoul} soul"
                } else {
                    "You received: +${info.receiverSoul} soul"
                },
                style = MaterialTheme.typography.bodySmall,
                color = bubbleSubTextColor
            )
        }
    }
}

private data class RoomInvitePayload(
    val roomId: String,
    val roomName: String,
    val roomPhotoUrl: String?,
)

private fun parseRoomInviteChatPayload(text: String): RoomInvitePayload? {
    val raw = text.trim()
    if (!raw.startsWith("ROOM_INVITE|")) return null
    val parts = raw.split("|")
    if (parts.size < 4) return null
    val roomId = parts[1].trim()
    if (roomId.isBlank()) return null
    val roomName = parts[2].trim().ifBlank { "Voice Room" }
    val roomPhoto = parts.subList(3, parts.size).joinToString("|").trim().ifBlank { "" }
    return RoomInvitePayload(
        roomId = roomId,
        roomName = roomName,
        roomPhotoUrl = roomPhoto.takeIf { it.isNotBlank() },
    )
}

@Composable
private fun RoomInviteMessageBubble(
    info: RoomInvitePayload,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .background(color = Color(0xFFF8FAFC), shape = shape)
            .border(width = 1.dp, color = Color(0xFFC7D2FE), shape = shape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Voice Room Invite",
            style = MaterialTheme.typography.labelMedium,
            color = Color(0xFF6B7280),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!info.roomPhotoUrl.isNullOrBlank()) {
                ChatProfileAvatar(
                    photoUrl = info.roomPhotoUrl,
                    contentDescription = info.roomName,
                    size = 42.dp,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFE5E7EB), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎙", style = MaterialTheme.typography.titleMedium)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = info.roomName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Text(
                    text = "Room: ${info.roomId.takeLast(6)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B7280),
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E7FF), RoundedCornerShape(999.dp))
                .clickable { onJoinClick() }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Tap to Join",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF1D4ED8),
            )
        }
    }
}
