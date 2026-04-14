package com.souljoy.soulmasti.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.souljoy.soulmasti.R
import com.souljoy.soulmasti.ui.common.SoulplayChatThreadTopBar
import com.souljoy.soulmasti.ui.common.gift.GiftCelebrationOverlayHost
import com.souljoy.soulmasti.ui.common.gift.rememberGiftCelebrationQueue
import com.souljoy.soulmasti.ui.voiceroom.GiftWallDialog
import com.souljoy.soulmasti.ui.voiceroom.defaultGiftWallItems
import kotlinx.coroutines.launch

// Me = left, blue. Other user = right, gray (per product spec).
private val BubbleMeBlue = Color(0xFF2563EB)
private val BubbleMeOnBlue = Color(0xFFFFFFFF)
private val BubbleOtherGray = Color(0xFFE2E8F0)
private val BubbleOtherOnGray = Color(0xFF0F172A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatThreadScreen(
    viewModel: ChatThreadViewModel,
    onBack: () -> Unit,
    onOpenPeerProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val peerName by viewModel.peerDisplayName.collectAsStateWithLifecycle()
    val peerPhotoUrl by viewModel.peerPhotoUrl.collectAsStateWithLifecycle()
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
        items = remember { defaultGiftWallItems() },
        sending = giftSending,
        errorMessage = giftError,
        onDismiss = {
            showGiftDialog = false
            giftError = null
        },
        onSend = { giftId ->
            giftSending = true
            giftError = null
            scope.launch {
                val result = viewModel.sendGift(giftId)
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

@Composable
private fun GiftMessageBubble(
    mine: Boolean,
    info: GiftChatPayload,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Filled.CardGiftcard,
                contentDescription = null,
                tint = if (mine) BubbleMeOnBlue else BubbleOtherOnGray
            )
            Text(
                text = stringResource(R.string.chat_gift_title, info.giftLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = if (mine) BubbleMeOnBlue else BubbleOtherOnGray
            )
        }
        Text(
            text = stringResource(R.string.chat_gift_coins_sent, info.giftCoins),
            style = MaterialTheme.typography.bodySmall,
            color = if (mine) BubbleMeOnBlue else BubbleOtherOnGray
        )
        Text(
            text = stringResource(R.string.chat_gift_receiver_coins, info.receiverCoins),
            style = MaterialTheme.typography.bodySmall,
            color = if (mine) BubbleMeOnBlue else BubbleOtherOnGray
        )
    }
}
