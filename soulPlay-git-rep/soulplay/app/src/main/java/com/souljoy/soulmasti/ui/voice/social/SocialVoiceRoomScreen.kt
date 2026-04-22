package com.souljoy.soulmasti.ui.voice.social

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.souljoy.soulmasti.R
import coil.compose.AsyncImage
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.domain.gift.GiftEvent
import com.souljoy.soulmasti.domain.model.SeatRole
import com.souljoy.soulmasti.ui.common.SpeakingWaveRings
import com.souljoy.soulmasti.ui.common.soulBadgeIconForSoul
import com.souljoy.soulmasti.ui.common.gift.GiftCelebrationOverlayHost
import com.souljoy.soulmasti.ui.common.gift.rememberGiftCelebrationQueue
import com.souljoy.soulmasti.ui.voice.game.GiftWallDialog
import com.souljoy.soulmasti.ui.voice.game.defaultGiftWallItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.NumberFormat

private enum class RoomInfoTab { Online, Contribution }
private enum class ContributionTab { Daily, Weekly, Total }
private val SocialVoiceRoomGradientColors =
    listOf(Color(0xFF080B2D), Color(0xFF090D3B), Color(0xFF101943))

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SocialVoiceRoomScreen(
    viewModel: SocialVoiceRoomViewModel,
    hasVoicePermission: () -> Boolean,
    requestVoicePermission: () -> Unit,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit = {},
    onOpenSoulLevel: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val pickRoomCoverPhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.uploadRoomCover(uri)
    }
    val room by viewModel.room.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val giftEvents by viewModel.giftEvents.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val participants by viewModel.participants.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val audioLevels by viewModel.audioLevelsByUid.collectAsStateWithLifecycle()
    val incomingInvite by viewModel.incomingInvite.collectAsStateWithLifecycle()
    val userProfiles by viewModel.userProfiles.collectAsStateWithLifecycle()
    val coinBalance by viewModel.coinBalance.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val myUid = viewModel.myUid
    var showRoomMenu by remember { mutableStateOf(false) }
    var selectedSeatNo by remember { mutableStateOf<Int?>(null) }
    var showInvitePickerForSeat by remember { mutableStateOf<Int?>(null) }
    var joinTickerText by remember { mutableStateOf<String?>(null) }
    var showRenameRoomDialog by remember { mutableStateOf(false) }
    var renameDraft by remember { mutableStateOf("") }
    var showRoomSettingsSheet by remember { mutableStateOf(false) }
    var showRoomInfoSheet by remember { mutableStateOf(false) }
    var showRoomLevelSheet by remember { mutableStateOf(false) }
    var showRoomBackgroundSheet by remember { mutableStateOf(false) }
    var roomBackgroundPreviewTarget by remember { mutableStateOf<String?>(null) }
    var highQualityEnabled by remember { mutableStateOf(true) }
    var banTextChat by remember { mutableStateOf(false) }
    var banPhotoChat by remember { mutableStateOf(false) }
    var banRedPacket by remember { mutableStateOf(false) }
    var passwordEnabled by remember { mutableStateOf(false) }
    var manageEventEnabled by remember { mutableStateOf(false) }
    var profileDialogSeatNo by remember { mutableStateOf<Int?>(null) }
    var showContributorsDialog by remember { mutableStateOf(false) }
    var roomInfoTab by remember { mutableStateOf(RoomInfoTab.Contribution) }
    var contributionTab by remember { mutableStateOf(ContributionTab.Daily) }
    var giftRecipientUid by remember { mutableStateOf<String?>(null) }
    var giftSending by remember { mutableStateOf(false) }
    var giftError by remember { mutableStateOf<String?>(null) }
    var profilePreviewUid by remember { mutableStateOf<String?>(null) }
    val giftScope = rememberCoroutineScope()
    val giftCelebration = rememberGiftCelebrationQueue()
    val chatListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (hasVoicePermission()) viewModel.joinAgoraVoice() else requestVoicePermission()
    }
    LaunchedEffect(Unit) {
        viewModel.toastEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.joinTicker.collectLatest { text ->
            joinTickerText = text
            delay(1_000)
            joinTickerText = null
        }
    }

    val seats = room?.seats.orEmpty()
    val amOwnerOrAdmin = myUid != null && (myUid == room?.ownerUid || room?.adminUids?.contains(myUid) == true)
    val isOwnRoom = myUid != null && myUid == room?.ownerUid
    val roomActive = room?.ownerOnline == true && room?.collapsed != true
    val seatByNo = remember(seats) { seats.associateBy { it.seatNo } }
    val onlineUsers = remember(room, seats) {
        (
            room?.onlineUids.orEmpty() +
                seats.mapNotNull { it.occupantUid }
            )
            .filter { it.isNotBlank() }
            .toSet()
            .sorted()
    }
    val onlineCountDisplay = maxOf(room?.onlineCount ?: 0, onlineUsers.size, participants.size)
    val totalRoomSoul = room?.contributionTotalSoul.orEmpty().values.sum()
    val liveRoomLevel = roomLevelForSoul(totalRoomSoul)
    val savedBackgroundOption =
        roomBackgroundOptionForName(room?.roomBackgroundName)
            ?.takeIf { liveRoomLevel >= it.unlockLevel }
    val freeBackgroundOption = RoomBackgroundOptions.firstOrNull { it.name.equals("Free", ignoreCase = true) } ?: RoomBackgroundOptions.first()
    val activeBackgroundOption = savedBackgroundOption ?: freeBackgroundOption
    val liveRoomBackgroundColors = activeBackgroundOption.colors
    val topContributors = remember(room) {
        room?.contributionTotalSoul
            .orEmpty()
            .entries
            .sortedByDescending { it.value }
            .take(3)
    }
    val todayContributorCount = remember(room) {
        room?.contributionDailySoul.orEmpty().count { it.value > 0L }
    }
    val chatFeed: List<ChatFeedItem> = remember(messages, giftEvents) {
        val merged: List<ChatFeedItem> =
            messages.map { ChatFeedItem.TextMessage(it) } +
                giftEvents.map { ChatFeedItem.GiftMessage(it) }
        merged.sortedBy { it.timestamp }
    }
    LaunchedEffect(chatFeed.size) {
        if (chatFeed.isNotEmpty()) {
            chatListState.animateScrollToItem(chatFeed.lastIndex)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.giftFxEvents.collect { ev ->
            giftCelebration.enqueueIfFx(context, ev.giftId)
        }
    }
    fun handleSeatTap(seatNo: Int, occupiedBySelf: Boolean) {
        val seat = seatByNo[seatNo] ?: return
        val occupiedUid = seat.occupantUid
        val canTake = seat.occupantUid.isNullOrBlank() && !seat.locked
        if (!occupiedUid.isNullOrBlank()) {
            profileDialogSeatNo = seatNo
            return
        }
        if (!roomActive && !(myUid == room?.ownerUid)) {
            viewModel.showInfo("Owner is offline. Seats disabled")
        } else if (amOwnerOrAdmin || occupiedBySelf) {
            selectedSeatNo = seatNo
        } else if (canTake) {
            viewModel.onSeatClick(seatNo, occupiedBySelf = false)
        } else if (seat.locked) {
            viewModel.showInfo("Seat is locked")
        } else {
            viewModel.showInfo("Only owner can invite")
        }
    }

    val speakingLevelForUid: (String?) -> Float = { uid ->
        val safeUid = uid?.takeIf { it.isNotBlank() }
        if (safeUid == null) {
            0f
        } else {
        val agoraUid = FirebaseUidMapping.agoraUidFromFirebaseUid(safeUid)
        audioLevels[agoraUid] ?: 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = liveRoomBackgroundColors,
                ),
            ),
    ) {
        PremiumBackgroundEffects(level = liveRoomLevel, backgroundTier = activeBackgroundOption.unlockLevel)

        // Decorative neon glow blobs similar to reference.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x553B82F6), Color(0x334C1D95)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 20.dp)
                .size(140.dp)
                .background(Color(0x223B82F6), shape = RoundedCornerShape(999.dp)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 26.dp, top = 100.dp)
                .size(120.dp)
                .background(Color(0x22A855F7), shape = RoundedCornerShape(999.dp)),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Column(
                            modifier = Modifier.clickable { showRoomInfoSheet = true },
                        ) {
                            Text(
                                text = room?.roomName ?: "music ♪",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Id: ${room?.roomId?.takeLast(6) ?: viewModel.currentRoomId.takeLast(6)}",
                                color = Color(0xFF94A3B8),
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .clickable {
                                roomInfoTab = RoomInfoTab.Contribution
                                contributionTab = ContributionTab.Daily
                                showContributorsDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (topContributors.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x33475569)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("-", color = Color.White, style = MaterialTheme.typography.titleSmall)
                            }
                        } else {
                            topContributors.forEach { entry ->
                                val uid = entry.key
                                val photo = viewModel.profileImageUrl(uid)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(Color(0x33475569)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (!photo.isNullOrBlank()) {
                                            AsyncImage(
                                                model = photo,
                                                contentDescription = "Contributor",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        } else {
                                            Text(
                                                text = viewModel.displayName(uid).take(1).uppercase(),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = Color(0xCC1F2937),
                                        modifier = Modifier.padding(top = 2.dp),
                                    ) {
                                        Text(
                                            text = formatContribution(entry.value),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                            color = Color(0xFFFCD34D),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                                text = "${onlineCountDisplay.coerceAtLeast(0)}",
                                color = Color(0xFFFDE68A),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        Spacer(Modifier.width(2.dp))

                    }
                        Box {
                            IconButton(onClick = { showRoomMenu = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Room menu",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                RoomSeatRow(
                    seatOrder = listOf(1, 2),
                    seatByNo = seatByNo,
                    myUid = myUid,
                    ownerUid = room?.ownerUid,
                    amOwnerOrAdmin = amOwnerOrAdmin,
                    onSeatAction = ::handleSeatTap,
                    canTakeSeat = { seat -> seat.occupantUid.isNullOrBlank() && !seat.locked },
                    isSpeaking = { uid -> speakingLevelForUid(uid) >  0.035f },
                    speakingLevel = speakingLevelForUid,
                    displayNameForUid = { uid -> uid?.let { userProfiles[it]?.name } },
                    profileUrlForUid = { uid -> uid?.let { viewModel.profileImageUrl(it) } },
                    ownerOnline = room?.ownerOnline == true,
                    modifier = Modifier.fillMaxWidth(),
                )
                RoomSeatRow(
                    seatOrder = listOf(3, 4, 5, 6),
                    seatByNo = seatByNo,
                    myUid = myUid,
                    ownerUid = room?.ownerUid,
                    amOwnerOrAdmin = amOwnerOrAdmin,
                    onSeatAction = ::handleSeatTap,
                    canTakeSeat = { seat -> seat.occupantUid.isNullOrBlank() && !seat.locked },
                    isSpeaking = { uid -> speakingLevelForUid(uid) >  0.035f },
                    speakingLevel = speakingLevelForUid,
                    displayNameForUid = { uid -> uid?.let { userProfiles[it]?.name } },
                    profileUrlForUid = { uid -> uid?.let { viewModel.profileImageUrl(it) } },
                    ownerOnline = room?.ownerOnline == true,
                    isCompact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                RoomSeatRow(
                    seatOrder = listOf(7, 8, 9, 10),
                    seatByNo = seatByNo,
                    myUid = myUid,
                    ownerUid = room?.ownerUid,
                    amOwnerOrAdmin = amOwnerOrAdmin,
                    onSeatAction = ::handleSeatTap,
                    canTakeSeat = { seat -> seat.occupantUid.isNullOrBlank() && !seat.locked },
                    isSpeaking = { uid -> speakingLevelForUid(uid) > 0.035f },
                    speakingLevel = speakingLevelForUid,
                    displayNameForUid = { uid -> uid?.let { userProfiles[it]?.name } },
                    profileUrlForUid = { uid -> uid?.let { viewModel.profileImageUrl(it) } },
                    ownerOnline = room?.ownerOnline == true,
                    isCompact = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            error?.let {
                Text(it, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        joinTickerText?.let { ticker ->
                            Text(
                                text = ticker,
                                color = Color(0xFF93C5FD),
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        LazyColumn(
                            state = chatListState,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(chatFeed) { item ->
                                when (item) {
                                    is ChatFeedItem.TextMessage -> {
                                        ChatMessageCard(
                                            senderName = item.message.fromName,
                                            senderPhotoUrl = viewModel.profileImageUrl(item.message.fromUid),
                                            senderSoul = viewModel.userSoul(item.message.fromUid),
                                            message = item.message.text,
                                            onProfileClick = { profilePreviewUid = item.message.fromUid },
                                        onBadgeClick = { onOpenSoulLevel(item.message.fromUid) },
                                        )
                                    }
                                    is ChatFeedItem.GiftMessage -> {
                                        GiftChatCard(
                                            event = item.event,
                                            senderName = viewModel.displayName(item.event.fromUserId),
                                            senderSoul = viewModel.userSoul(item.event.fromUserId),
                                            recipientName = item.event.toUserId?.let { viewModel.displayName(it) }.orEmpty(),
                                            senderPhotoUrl = viewModel.profileImageUrl(item.event.fromUserId),
                                            summaryText = viewModel.giftSummaryText(item.event),
                                        onProfileClick = { profilePreviewUid = item.event.fromUserId },
                                        onBadgeClick = { onOpenSoulLevel(item.event.fromUserId) },
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0x441A2258),
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = draft,
                                    onValueChange = { draft = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Type...", color = Color(0xFF94A3B8)) },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            val t = draft.trim()
                                            if (t.isNotBlank()) {
                                                viewModel.sendMessage(t)
                                                draft = ""
                                            }
                                        },
                                    ),
                                    singleLine = true,
                                )
                                Button(
                                    onClick = {
                                        val t = draft.trim()
                                        if (t.isNotBlank()) {
                                            viewModel.sendMessage(t)
                                            draft = ""
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                                ) {
                                    Text("Send", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    val selectedSeat = room?.seats?.firstOrNull { it.seatNo == selectedSeatNo }
    if (selectedSeat != null) {
        val occupiedBySelf = myUid != null && selectedSeat.occupantUid == myUid
        val amOwnerOrAdminLocal = myUid != null && (myUid == room?.ownerUid || room?.adminUids?.contains(myUid) == true)
        ModalBottomSheet(
            onDismissRequest = { selectedSeatNo = null },
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                if (amOwnerOrAdminLocal) {
                    DropdownLikeAction(
                        text = if (selectedSeat.locked) "Open Seat" else "Close Seat",
                        onClick = {
                            viewModel.setSeatLocked(selectedSeat.seatNo, !selectedSeat.locked)
                            selectedSeatNo = null
                        },
                    )
                    DropdownLikeAction(
                        text = if (selectedSeat.muted) "Unmute Seat" else "Mute Seat",
                        onClick = {
                            viewModel.setSeatMuted(selectedSeat.seatNo, !selectedSeat.muted)
                            selectedSeatNo = null
                        },
                    )
                    if (!selectedSeat.occupantUid.isNullOrBlank()) {
                        DropdownLikeAction(
                            text = "Unseat",
                            onClick = {
                                if (occupiedBySelf) {
                                    viewModel.onSeatClick(selectedSeat.seatNo, occupiedBySelf = true)
                                    viewModel.showInfo("You left the seat")
                                } else {
                                    viewModel.removeSeatOccupant(selectedSeat.seatNo)
                                    viewModel.showInfo("User unseated")
                                }
                                selectedSeatNo = null
                            },
                        )
                    } else if (!selectedSeat.locked) {
                        DropdownLikeAction(
                            text = "Invite",
                            onClick = {
                                showInvitePickerForSeat = selectedSeat.seatNo
                                selectedSeatNo = null
                            },
                        )
                    }
                    DropdownLikeAction(text = "Cancel", onClick = { selectedSeatNo = null })
                } else if (occupiedBySelf) {
                    DropdownLikeAction(
                        text = if (selectedSeat.muted) "Unmute" else "Mute",
                        onClick = {
                            viewModel.setSeatMuted(selectedSeat.seatNo, !selectedSeat.muted)
                            viewModel.toggleMute()
                            selectedSeatNo = null
                        },
                    )
                    DropdownLikeAction(
                        text = "Unseat",
                        onClick = {
                            viewModel.onSeatClick(selectedSeat.seatNo, occupiedBySelf = true)
                            viewModel.showInfo("You left the seat")
                            selectedSeatNo = null
                        },
                    )
                    DropdownLikeAction(text = "Cancel", onClick = { selectedSeatNo = null })
                } else {
                    DropdownLikeAction(text = "Cancel", onClick = { selectedSeatNo = null })
                }
            }
        }
    }

    val profileSeat = room?.seats?.firstOrNull { it.seatNo == profileDialogSeatNo && !it.occupantUid.isNullOrBlank() }
    if (profileSeat != null) {
        val targetUid = profileSeat.occupantUid.orEmpty()
        val isSelf = myUid != null && myUid == targetUid
        val isOwner = myUid != null && myUid == room?.ownerUid
        val isAdmin = myUid != null && room?.adminUids?.contains(myUid) == true
        val canMute = (isOwner || isAdmin || isSelf)
        val canUnseat = (isOwner || isAdmin || isSelf) && profileSeat.seatNo != 1
        val canGift = true
        val displayName = userProfiles[targetUid]?.name?.takeIf { it.isNotBlank() }
            ?: profileSeat.occupantName
            ?: "User"
        val photoUrl = viewModel.profileImageUrl(targetUid)
        AlertDialog(
            onDismissRequest = { profileDialogSeatNo = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x3322D3EE))
                            .clickable {
                                profileDialogSeatNo = null
                                onOpenUserProfile(targetUid)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(999.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = displayName.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Column {
                        Text(displayName, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                        Text("Seat ${profileSeat.seatNo}", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (canMute) {
                        Button(
                            onClick = {
                                viewModel.setSeatMuted(profileSeat.seatNo, !profileSeat.muted)
                                profileDialogSeatNo = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        ) {
                            Text(if (profileSeat.muted) "Unmute" else "Mute")
                        }
                    }
                    if (canUnseat) {
                        Button(
                            onClick = {
                                if (isSelf) {
                                    viewModel.onSeatClick(profileSeat.seatNo, occupiedBySelf = true)
                                } else {
                                    viewModel.removeSeatOccupant(profileSeat.seatNo)
                                }
                                profileDialogSeatNo = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C)),
                        ) {
                            Text("Unseat")
                        }
                    }
                    if (canGift) {
                        Button(
                            onClick = {
                                giftRecipientUid = targetUid
                                giftError = null
                                profileDialogSeatNo = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                        ) {
                            Text("Gift")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { profileDialogSeatNo = null }) { Text("Close") }
            },
        )
    }

    profilePreviewUid?.let { uid ->
        val displayName = userProfiles[uid]?.name?.takeIf { it.isNotBlank() } ?: viewModel.displayName(uid)
        val photoUrl = viewModel.profileImageUrl(uid)
        AlertDialog(
            onDismissRequest = { profilePreviewUid = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0x3322D3EE))
                            .clickable {
                                profilePreviewUid = null
                                onOpenUserProfile(uid)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(999.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = displayName.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                    Column {
                        Text(displayName, color = Color(0xFF111827), fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            androidx.compose.foundation.Image(
                                painter = painterResource(soulBadgeIconForSoul(viewModel.userSoul(uid))),
                                contentDescription = "Badge",
                                modifier = Modifier.height(16.dp).clickable { onOpenSoulLevel(uid) },
                            )
                            Text(
                                "Tap badge for details",
                                color = Color(0xFF64748B),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            text = { Text("Send gift to this user.", color = Color(0xFF64748B)) },
            confirmButton = {
                TextButton(onClick = {
                    profilePreviewUid = null
                    giftRecipientUid = uid
                    giftError = null
                }) { Text("Send Gift") }
            },
            dismissButton = {
                TextButton(onClick = { profilePreviewUid = null }) { Text("Close") }
            },
        )
    }

    val giftRecipientLabel = giftRecipientUid?.let { uid ->
        userProfiles[uid]?.name?.takeIf { it.isNotBlank() } ?: viewModel.displayName(uid)
    }.orEmpty()
    GiftWallDialog(
        visible = giftRecipientUid != null,
        recipientDisplayName = giftRecipientLabel.ifBlank { "Recipient" },
        availableCoins = coinBalance,
        items = remember { defaultGiftWallItems() },
        sending = giftSending,
        errorMessage = giftError,
        onDismiss = {
            giftRecipientUid = null
            giftError = null
        },
        onSend = { giftId, selectedCount ->
            val toUid = giftRecipientUid ?: return@GiftWallDialog
            giftSending = true
            giftError = null
            giftScope.launch {
                val result = viewModel.sendGift(toUid, giftId, selectedCount)
                giftSending = false
                result.onSuccess {
                    giftRecipientUid = null
                    giftError = null
                }.onFailure { e ->
                    giftError = e.message ?: "Could not send gift"
                }
            }
        },
    )

    val inviteSeatNo = showInvitePickerForSeat
    if (inviteSeatNo != null) {
        val inviteOnlineUsers = onlineUsers.filter { it != myUid }
        AlertDialog(
            onDismissRequest = { showInvitePickerForSeat = null },
            title = { Text("Invite to seat $inviteSeatNo") },
            text = {
                if (inviteOnlineUsers.isEmpty()) {
                    Text("No online users available in this room.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        inviteOnlineUsers.forEach { uid ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.sendSeatInvite(inviteSeatNo, uid)
                                        showInvitePickerForSeat = null
                                    }
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(viewModel.displayName(uid), color = Color(0xFF111827))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInvitePickerForSeat = null }) { Text("Close") }
            },
        )
    }

    incomingInvite?.let { invite ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Take Seat Request") },
            text = { Text("${viewModel.displayName(invite.fromUid)} invited you to take seat ${invite.seatNo}.") },
            confirmButton = {
                TextButton(onClick = { viewModel.respondToIncomingInvite(accept = true) }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.respondToIncomingInvite(accept = false) }) { Text("No") }
            },
        )
    }

        if (showRoomMenu) {
            val menuItems = buildList {
                add(
                    RoomMenuAction("Invite Friends", Icons.Filled.SupervisorAccount) {
                        viewModel.showInfo("Invite friends coming soon")
                    },
                )
                if (isOwnRoom) {
                    add(RoomMenuAction("Add Music", Icons.Filled.MusicNote) { viewModel.showInfo("Music coming soon") })
                }
                add(RoomMenuAction("Report Room", Icons.Filled.Info) { viewModel.showInfo("Report room coming soon") })
                if (isOwnRoom) {
                    add(RoomMenuAction("Settings", Icons.Filled.Settings) { showRoomSettingsSheet = true })
                }
                add(RoomMenuAction("Exit", Icons.Filled.ExitToApp) { viewModel.leaveRoom(onDone = onBack) })
            }
            RoomFunctionMenuOverlay(
                actions = menuItems,
                onDismiss = { showRoomMenu = false },
                onAction = { action ->
                    showRoomMenu = false
                    action.onClick()
                },
            )
        }

    if (showRenameRoomDialog) {
        AlertDialog(
            onDismissRequest = { showRenameRoomDialog = false },
            title = { Text("Change room name") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(8) },
                    singleLine = true,
                    placeholder = { Text("Enter room name (max 8)") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRenameRoomDialog = false
                    viewModel.updateRoomName(renameDraft)
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameRoomDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showRoomInfoSheet) {
        val roomName = room?.roomName.orEmpty().ifBlank { "music" }
        val roomIdShort = room?.roomId?.takeLast(6) ?: viewModel.currentRoomId.takeLast(6)
        val totalSoul = room?.contributionTotalSoul.orEmpty().values.sum()
        val previewPeople = remember(room, onlineUsers) {
            val owner = listOfNotNull(room?.ownerUid)
            val online = onlineUsers
            val seated = room?.seats.orEmpty().mapNotNull { it.occupantUid }
            (owner + online + seated).filter { it.isNotBlank() }.distinct().take(4)
        }
        val contributorCount = room?.contributionTotalSoul.orEmpty().count { it.value > 0L }
        val roomLevel = roomLevelForSoul(totalSoul)
        ModalBottomSheet(onDismissRequest = { showRoomInfoSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center,
                            ) {
                                val coverUrl = room?.roomCoverUrl?.takeIf { it.isNotBlank() }
                                if (!coverUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = "Room cover",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    Text(roomName.take(1).uppercase(), color = Color(0xFF334155), fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                if (isOwnRoom) {
                                    IconButton(onClick = {
                                        showRoomInfoSheet = false
                                        showRoomSettingsSheet = true
                                    }) {
                                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color(0xFF475569))
                                    }
                                }
                                Image(
                                    painter = painterResource(id = roomLevelIconRes(roomLevel)),
                                    contentDescription = "Room Level",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clickable { showRoomLevelSheet = true },
                                )
                                Text(
                                    text = roomLevelLabel(roomLevel),
                                    color = Color(0xFF6B7280),
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(roomName, color = Color(0xFF111827), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Text("Friends   Advanced Room  ID:$roomIdShort", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (previewPeople.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                previewPeople.forEach { uid ->
                                    val photo = viewModel.profileImageUrl(uid)
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color(0xFFE2E8F0)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (!photo.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = photo,
                                                    contentDescription = "Member",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Text(viewModel.displayName(uid).take(1).uppercase(), color = Color(0xFF334155))
                                            }
                                        }
                                        Text(
                                            viewModel.displayName(uid).take(10),
                                            color = Color(0xFF374151),
                                            style = MaterialTheme.typography.labelMedium,
                                        )
                                        Text(
                                            if (uid == room?.ownerUid) "Owner" else "Member",
                                            color = if (uid == room?.ownerUid) Color(0xFFCA8A04) else Color(0xFF2563EB),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            InfoMetricCard("Online", onlineCountDisplay.toString(), Color(0xFF16A34A), Modifier.weight(1f))
                            InfoMetricCard("Contributors", contributorCount.toString(), Color(0xFFA16207), Modifier.weight(1f))
                        }
                    }
                }
                TextButton(onClick = { showRoomInfoSheet = false }, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }

    if (showRoomSettingsSheet) {
        val ownerPhoto = viewModel.profileImageUrl(room?.ownerUid)
        val roomCoverUrl = room?.roomCoverUrl?.takeIf { it.isNotBlank() }
        ModalBottomSheet(onDismissRequest = { showRoomSettingsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
                SettingsGroupCard {
                    SettingsArrowRow(
                        label = "Title",
                        value = room?.roomName.orEmpty().ifBlank { "room" },
                        onClick = {
                            renameDraft = room?.roomName.orEmpty()
                            showRoomSettingsSheet = false
                            showRenameRoomDialog = true
                        },
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    SettingsCoverRow(
                        coverUrl = roomCoverUrl,
                        fallbackPhoto = ownerPhoto,
                        fallbackInitial = (room?.roomName ?: "R").take(1).uppercase(),
                        onClick = {
                            if (!isOwnRoom) {
                                viewModel.showInfo("Only owner can change cover")
                            } else {
                                pickRoomCoverPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        },
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    SettingsArrowRow("Announcement", "It is a voice room", onClick = {})
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    SettingsArrowRow(
                        "Voice Info Detail Preview",
                        "Preview",
                        onClick = {
                            showRoomSettingsSheet = false
                            showRoomInfoSheet = true
                        },
                    )
                }
                SettingsGroupCard {
                    SettingsArrowRow(
                        label = "Room ID",
                        value = room?.roomId?.takeLast(6).orEmpty(),
                        onClick = {},
                        enabled = false,
                    )
                }
                TextButton(onClick = { showRoomSettingsSheet = false }, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }

    if (showRoomLevelSheet) {
        val totalSoul = room?.contributionTotalSoul.orEmpty().values.sum()
        val currentLevel = roomLevelForSoul(totalSoul)
        val currentLevelMinSoul = RoomLevelDefs.getOrNull(currentLevel - 1)?.requiredSoul ?: 0L
        val nextLevelSoulRaw = RoomLevelDefs.getOrNull(currentLevel)?.requiredSoul ?: currentLevelMinSoul
        val nextLevelSoulDisplay = if (currentLevel <= 1) nextLevelSoulRaw else nextLevelSoulRaw + 1_000L
        val levelProgress = ((totalSoul - currentLevelMinSoul).toFloat() / (nextLevelSoulRaw - currentLevelMinSoul).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
        ModalBottomSheet(onDismissRequest = { showRoomLevelSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Levels of Advanced Voice Room", color = Color(0xFF111827), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(id = roomLevelIconRes(currentLevel)),
                        contentDescription = "Current level",
                        modifier = Modifier.size(30.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Lv$currentLevel", color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                        LinearProgressIndicator(
                            progress = { levelProgress },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFFE2E8F0),
                        )
                        Text("$totalSoul / $nextLevelSoulDisplay souls", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(RoomLevelDefs) { level ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = level.iconRes),
                                        contentDescription = "Level ${level.level}",
                                        modifier = Modifier.size(26.dp),
                                    )
                                    Column {
                                        Text("Level ${level.level}", color = Color(0xFF111827), fontWeight = FontWeight.SemiBold)
                                        Text("${displayRankSoul(level)} souls needed", color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                if (!level.backgroundName.isNullOrBlank()) {
                                    TextButton(
                                        onClick = {
                                            roomBackgroundPreviewTarget = RoomBackgroundOptions
                                                .lastOrNull { level.level >= it.unlockLevel }
                                                ?.name
                                                ?: "Free"
                                            showRoomLevelSheet = false
                                            showRoomBackgroundSheet = true
                                        },
                                    ) { Text("Preview") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRoomBackgroundSheet) {
        val totalSoul = room?.contributionTotalSoul.orEmpty().values.sum()
        val roomLevel = roomLevelForSoul(totalSoul)
        val backgroundOptions = RoomBackgroundOptions
        var selectedBackground by remember(roomLevel, room?.roomBackgroundName, roomBackgroundPreviewTarget) {
            mutableStateOf(
                backgroundOptions.firstOrNull { it.name.equals(roomBackgroundPreviewTarget, ignoreCase = true) }
                    ?: backgroundOptions.firstOrNull { it.name == room?.roomBackgroundName }
                    ?: backgroundOptions.firstOrNull { it.name.equals("Free", ignoreCase = true) }
                    ?: backgroundOptions.first(),
            )
        }
        val canSaveSelected = isOwnRoom && roomLevel >= selectedBackground.unlockLevel
        ModalBottomSheet(
            onDismissRequest = {
                roomBackgroundPreviewTarget = null
                showRoomBackgroundSheet = false
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFF3F4F6)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Room Background", color = Color(0xFF111827), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (canSaveSelected) {
                        TextButton(
                            onClick = {
                                viewModel.updateRoomBackgroundName(selectedBackground.name)
                                roomBackgroundPreviewTarget = null
                                showRoomBackgroundSheet = false
                            },
                        ) { Text("Save") }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .width(230.dp)
                            .height(410.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.verticalGradient(selectedBackground.colors))
                            .border(1.dp, Color(0x22000000), RoundedCornerShape(18.dp)),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Welcome to SoulMast", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(Color.White.copy(alpha = 0.28f)),
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "System: Welcome to SoulMast Voice Room.",
                                color = Color.White.copy(alpha = 0.95f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        PremiumBackgroundEffects(level = roomLevel, backgroundTier = selectedBackground.unlockLevel)
                        if (roomLevel >= selectedBackground.unlockLevel) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Premium",
                                tint = Color(0xFFFFD166),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(18.dp),
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    backgroundOptions.forEach { option ->
                        val unlocked = roomLevel >= option.unlockLevel
                        val selected = selectedBackground.name == option.name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .width(76.dp)
                                .clickable {
                                    selectedBackground = option
                                },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 76.dp, height = 56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.verticalGradient(option.colors))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) Color(0xFF38BDF8) else Color(0x33000000),
                                        shape = RoundedCornerShape(10.dp),
                                    ),
                            ) {
                                if (unlocked) {
                                    Icon(
                                        imageVector = Icons.Filled.EmojiEvents,
                                        contentDescription = "Unlocked premium",
                                        tint = Color(0xFFFFD166),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(12.dp),
                                    )
                                }
                            }
                            Text(
                                text = option.name,
                                color = if (unlocked) Color(0xFF374151) else Color(0xFF9CA3AF),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            if (!unlocked) {
                                Text("Lv${option.unlockLevel}", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                Text(
                    text = "Background unlocks: Free, Lv3 Brief, Lv5 Music, Lv7 Mountains, Lv9 Nature, Lv10 Mahel",
                    color = Color(0xFF94A3B8),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    if (showContributorsDialog) {
        val contributionMap = when (contributionTab) {
            ContributionTab.Daily -> room?.contributionDailySoul.orEmpty()
            ContributionTab.Weekly -> room?.contributionWeeklySoul.orEmpty()
            ContributionTab.Total -> room?.contributionTotalSoul.orEmpty()
        }
        val sortedContributors = contributionMap.entries.sortedByDescending { it.value }
        val sortedOnlineUsers = onlineUsers.sortedBy { viewModel.displayName(it) }
        ModalBottomSheet(
            onDismissRequest = { showContributorsDialog = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    TopTabLabel(
                        text = "Online $onlineCountDisplay",
                        selected = roomInfoTab == RoomInfoTab.Online,
                        onClick = { roomInfoTab = RoomInfoTab.Online },
                    )
                    TopTabLabel(
                        text = "Contribution",
                        selected = roomInfoTab == RoomInfoTab.Contribution,
                        onClick = { roomInfoTab = RoomInfoTab.Contribution },
                    )
                }
                if (roomInfoTab == RoomInfoTab.Contribution) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChipButton(
                            text = "Daily",
                            selected = contributionTab == ContributionTab.Daily,
                            onClick = { contributionTab = ContributionTab.Daily },
                        )
                        FilterChipButton(
                            text = "Weekly",
                            selected = contributionTab == ContributionTab.Weekly,
                            onClick = { contributionTab = ContributionTab.Weekly },
                        )
                        FilterChipButton(
                            text = "Total",
                            selected = contributionTab == ContributionTab.Total,
                            onClick = { contributionTab = ContributionTab.Total },
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    if (roomInfoTab == RoomInfoTab.Online) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (sortedOnlineUsers.isEmpty()) {
                                item {
                                    Text(
                                        text = "-",
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                            } else {
                                items(sortedOnlineUsers) { uid ->
                                    ContributorRow(
                                        rank = null,
                                        name = viewModel.displayName(uid),
                                        photoUrl = viewModel.profileImageUrl(uid),
                                        soul = viewModel.userSoul(uid),
                                        valueLabel = "",
                                        onProfileClick = { profilePreviewUid = uid },
                                        onBadgeClick = { onOpenSoulLevel(uid) },
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (sortedContributors.isEmpty()) {
                                item {
                                    Text(
                                        text = "-",
                                        color = Color(0xFF94A3B8),
                                        style = MaterialTheme.typography.titleLarge,
                                    )
                                }
                            } else {
                                itemsIndexed(sortedContributors) { index, entry ->
                                    ContributorRow(
                                        rank = index + 1,
                                        name = viewModel.displayName(entry.key),
                                        photoUrl = viewModel.profileImageUrl(entry.key),
                                        soul = viewModel.userSoul(entry.key),
                                        valueLabel = formatFullPoints(entry.value),
                                        onProfileClick = { profilePreviewUid = entry.key },
                                        onBadgeClick = { onOpenSoulLevel(entry.key) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    GiftCelebrationOverlayHost(queue = giftCelebration)

}

@Composable
private fun SettingsGroupCard(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 2.dp),
        content = content,
    )
}

@Composable
private fun SettingsArrowRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color(0xFF111827), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (value.isNotBlank()) {
                Text(value, color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodyMedium)
            }
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(16.dp),
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFB4BAC5),
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color(0xFF111827), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsCoverRow(
    coverUrl: String?,
    fallbackPhoto: String?,
    fallbackInitial: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text("Cover", color = Color(0xFF111827), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE5E7EB)),
                contentAlignment = Alignment.Center,
            ) {
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else if (!fallbackPhoto.isNullOrBlank()) {
                    AsyncImage(
                        model = fallbackPhoto,
                        contentDescription = "Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Text(fallbackInitial, color = Color(0xFF475569))
                }
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFB4BAC5),
            )
        }
    }
}

private data class RoomLevelDef(
    val level: Int,
    val requiredSoul: Long,
    val iconRes: Int,
    val backgroundName: String? = null,
)

private data class RoomBackgroundOption(
    val name: String,
    val unlockLevel: Int,
    val colors: List<Color>,
)

private val RoomBackgroundOptions = listOf(
    RoomBackgroundOption("Free", 1, listOf(Color(0xFF581C87), Color(0xFF7C3AED))),
    RoomBackgroundOption("Brief", 3, listOf(Color(0xFF111827), Color(0xFF1F2937))),
    RoomBackgroundOption("Music", 5, listOf(Color(0xFF080B2D), Color(0xFF090D3B), Color(0xFF101943))),
    RoomBackgroundOption("Mountains", 7, listOf(Color(0xFF374151), Color(0xFF0EA5E9))),
    RoomBackgroundOption("Nature", 9, listOf(Color(0xFF14532D), Color(0xFF22C55E))),
    RoomBackgroundOption("Mahel", 10, listOf(Color(0xFF7C2D12), Color(0xFFF59E0B))),
)

private val RoomLevelDefs = listOf(
    RoomLevelDef(1, 0L, R.drawable.rank_bronze_1),
    RoomLevelDef(2, 8_000L, R.drawable.rank_bronze_2),
    RoomLevelDef(3, 30_000L, R.drawable.rank_silver_1, "Club"),
    RoomLevelDef(4, 100_000L, R.drawable.rank_silver_2),
    RoomLevelDef(5, 300_000L, R.drawable.rank_platinum_1, "Diamonden Party"),
    RoomLevelDef(6, 800_000L, R.drawable.rank_platinum_2),
    RoomLevelDef(7, 2_000_000L, R.drawable.rank_gold_1, "Stars of a different world"),
    RoomLevelDef(8, 4_800_000L, R.drawable.rank_gold_2),
    RoomLevelDef(9, 11_000_000L, R.drawable.rank_ace_1, "Future City"),
    RoomLevelDef(10, 24_000_000L, R.drawable.rank_ace_2, "Custom background"),
)

private fun roomLevelForSoul(totalSoul: Long): Int {
    val matched = RoomLevelDefs.lastOrNull { totalSoul >= it.requiredSoul }
    return matched?.level ?: 1
}

private fun roomLevelIconRes(level: Int): Int {
    return RoomLevelDefs.firstOrNull { it.level == level }?.iconRes ?: R.drawable.rank_bronze_1
}

private fun roomLevelLabel(level: Int): String {
    return when {
        level >= 10 -> "Royal Conqueror"
        level >= 9 -> "Ace Sovereign"
        level >= 7 -> "Golden Elite"
        level >= 5 -> "Platinum Elite"
        level >= 3 -> "Silver Elite"
        level >= 2 -> "Bronze II"
        else -> "Bronze I"
    }
}

private fun displayRankSoul(level: RoomLevelDef): Long {
    return if (level.level == 1) level.requiredSoul else level.requiredSoul + 1_000L
}

private fun roomBackgroundColorsForLevel(level: Int): List<Color> {
    return when {
        level >= 10 -> listOf(Color(0xFF2A0A00), Color(0xFF7C2D12), Color(0xFFFFB347)) // Mahel (royal amber)
        level >= 9 -> listOf(Color(0xFF052E16), Color(0xFF14532D), Color(0xFF4ADE80)) // Nature (emerald premium)
        level >= 7 -> listOf(Color(0xFF111827), Color(0xFF334155), Color(0xFF38BDF8)) // Mountains (steel cyan)
        level >= 5 -> listOf(Color(0xFF2E1065), Color(0xFF581C87), Color(0xFFA78BFA)) // Music (royal purple)
        level >= 3 -> listOf(Color(0xFF0F172A), Color(0xFF111827), Color(0xFF374151)) // Brief (deep graphite)
        else -> SocialVoiceRoomGradientColors
    }
}

private fun roomBackgroundColorsForName(name: String?): List<Color>? {
    val safe = name?.takeIf { it.isNotBlank() } ?: return null
    return RoomBackgroundOptions.firstOrNull { it.name.equals(safe, ignoreCase = true) }?.colors
}

private fun roomBackgroundOptionForName(name: String?): RoomBackgroundOption? {
    val safe = name?.takeIf { it.isNotBlank() } ?: return null
    return RoomBackgroundOptions.firstOrNull { it.name.equals(safe, ignoreCase = true) }
}

private fun roomBackgroundUnlockLevel(name: String?): Int? {
    val safe = name?.takeIf { it.isNotBlank() } ?: return null
    return RoomBackgroundOptions.firstOrNull { it.name.equals(safe, ignoreCase = true) }?.unlockLevel
}

@Composable
private fun PremiumBackgroundEffects(level: Int, backgroundTier: Int) {
    if (backgroundTier < 3) return

    val transition = rememberInfiniteTransition(label = "premium-bg")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.68f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-alpha",
    )
    val floatY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float-y",
    )
    val shimmerX by transition.animateFloat(
        initialValue = -420f,
        targetValue = 1400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )
    val shimmerX2 by transition.animateFloat(
        initialValue = 1400f,
        targetValue = -420f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x-2",
    )

    val accent = when {
        level >= 10 -> Color(0xFFFFC857)
        level >= 9 -> Color(0xFF4ADE80)
        level >= 7 -> Color(0xFF38BDF8)
        level >= 5 -> Color(0xFFA78BFA)
        level >= 3 -> Color(0xFF94A3B8)
        else -> Color.Transparent
    }
    val tierStrength = when {
        backgroundTier >= 10 -> 1f
        backgroundTier >= 9 -> 0.92f
        backgroundTier >= 7 -> 0.82f
        backgroundTier >= 5 -> 0.72f
        else -> 0.55f
    }
    val baseIconSize = when {
        backgroundTier >= 10 -> 26.dp
        backgroundTier >= 9 -> 24.dp
        backgroundTier >= 7 -> 22.dp
        backgroundTier >= 5 -> 20.dp
        else -> 16.dp // level 3
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(alpha = pulseAlpha),
    ) {
        // Premium corner aura glow (stronger at higher levels).
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(220.dp)
                .graphicsLayer { translationY = floatY * 0.5f }
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.24f * tierStrength), Color.Transparent),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(260.dp)
                .graphicsLayer { translationY = -floatY * 0.35f }
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.17f * tierStrength), Color.Transparent),
                    ),
                ),
        )

        // Shimmer starts from level 5 and increases by tiers.
        if (backgroundTier >= 5) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (backgroundTier >= 9) 140.dp else 120.dp)
                    .graphicsLayer {
                        translationX = shimmerX
                        rotationZ = 13f
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = (if (backgroundTier >= 9) 0.16f else 0.11f) * tierStrength),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }
        if (backgroundTier >= 7) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(if (backgroundTier >= 10) 120.dp else 96.dp)
                    .graphicsLayer {
                        translationX = shimmerX2
                        rotationZ = -11f
                    }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                accent.copy(alpha = 0.15f * tierStrength),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        // Royal icons - much larger and visible.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 66.dp, end = 8.dp)
                .size(if (backgroundTier >= 9) 40.dp else 34.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color.White.copy(alpha = 0.14f * tierStrength)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = accent.copy(alpha = 0.98f),
                modifier = Modifier
                    .graphicsLayer { translationY = floatY }
                    .size(baseIconSize),
            )
        }
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = accent.copy(alpha = 0.96f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp, top = 120.dp)
                .graphicsLayer { translationY = -floatY * 0.6f }
                .size((baseIconSize.value - 4).coerceAtLeast(14f).dp),
        )
        Icon(
            imageVector = Icons.Filled.EmojiEvents,
            contentDescription = null,
            tint = accent.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 128.dp)
                .graphicsLayer { translationY = floatY * 0.4f }
                .size((baseIconSize.value - 6).coerceAtLeast(13f).dp),
        )
        if (backgroundTier >= 7) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = accent.copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 30.dp, top = 86.dp)
                    .graphicsLayer { translationY = -floatY * 0.45f }
                    .size(17.dp),
            )
        }
        if (backgroundTier >= 9) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFF3B0).copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 88.dp)
                    .graphicsLayer { translationY = floatY * 0.8f }
                    .size(18.dp),
            )
        }
        if (backgroundTier >= 10) {
            Icon(
                imageVector = Icons.Filled.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFE082),
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { translationY = floatY * 0.35f }
                    .size(13.dp),
            )
        }
    }
}

@Composable
private fun InfoMetricCard(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, color = tint, style = MaterialTheme.typography.bodySmall)
            Text(value, color = tint, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private data class RoomMenuAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Composable
private fun RoomFunctionMenuOverlay(
    actions: List<RoomMenuAction>,
    onDismiss: () -> Unit,
    onAction: (RoomMenuAction) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                .background(Color(0xEE060A17))
                .clickable(enabled = false, onClick = {})
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                modifier = Modifier.padding(0.dp,10.dp,0.dp , 0.dp),
                text = "Room function",
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                maxItemsInEachRow = 3,
                horizontalArrangement = Arrangement.SpaceAround,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                actions.forEach { action ->
                    Column(
                        modifier = Modifier
                            .width(74.dp)
                            .clickable { onAction(action) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = Color.White,
                            )
                        }
                        Text(
                            text = action.label,
                            color = Color.White.copy(alpha = 0.92f),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

private sealed class ChatFeedItem(open val timestamp: Long) {
    data class TextMessage(
        val message: com.souljoy.soulmasti.domain.model.SocialVoiceChatMessage,
    ) : ChatFeedItem(message.createdAt ?: 0L)

    data class GiftMessage(
        val event: GiftEvent,
    ) : ChatFeedItem(event.createdAt ?: 0L)
}

@Composable
private fun GiftChatCard(
    event: GiftEvent,
    senderName: String,
    senderSoul: Long,
    recipientName: String,
    senderPhotoUrl: String?,
    summaryText: String,
    onProfileClick: () -> Unit,
    onBadgeClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable(onClick = onProfileClick),
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0x33475569)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!senderPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = senderPhotoUrl,
                            contentDescription = "Sender",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Text(
                            text = senderName.take(1).uppercase(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Text(
                    text = senderName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(soulBadgeIconForSoul(senderSoul)),
                    contentDescription = "Sender badge",
                    modifier = Modifier.height(16.dp).clickable(onClick = onBadgeClick),
                )
            }
            Text(
                text = summaryText,
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.18f))
            val rewards = buildString {
                if (event.receiverCoins > 0L) append("Gold +${event.receiverCoins}")
                if (event.receiverSoul > 0L) {
                    if (isNotEmpty()) append(", ")
                    append("Charm +${event.receiverSoul}")
                }
                if (isEmpty()) append("Gift sent")
            }
            Text(
                text = if (recipientName.isBlank()) rewards else "$recipientName: $rewards",
                color = Color(0xFFFDE68A),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ChatMessageCard(
    senderName: String,
    senderPhotoUrl: String?,
    senderSoul: Long,
    message: String,
    onProfileClick: () -> Unit,
    onBadgeClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProfileClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x33475569)),
            contentAlignment = Alignment.Center,
        ) {
            if (!senderPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = senderPhotoUrl,
                    contentDescription = "Sender",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = senderName.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = senderName,
                    color = Color(0xFFE2E8F0),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(soulBadgeIconForSoul(senderSoul)),
                    contentDescription = "Badge",
                    modifier = Modifier
                        .height(14.dp)
                        .clickable(onClick = onBadgeClick),
                )
            }
            Text(
                text = message,
                color = Color(0xFFE2E8F0),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun FilterChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
            contentColor = if (selected) Color(0xFF1D4ED8) else Color(0xFF334155),
        ),
    ) {
        Text(text, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun TopTabLabel(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            color = if (selected) Color(0xFF111827) else Color(0xFF9CA3AF),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (selected) 38.dp else 0.dp)
                .background(Color(0xFF0EA5E9), RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun ContributorRow(
    rank: Int?,
    name: String,
    photoUrl: String?,
    soul: Long,
    valueLabel: String,
    onProfileClick: (() -> Unit)? = null,
    onBadgeClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onProfileClick != null) Modifier.clickable(onClick = onProfileClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (rank != null) {
            Text(
                text = rank.toString(),
                color = Color(0xFF6B7280),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.width(18.dp),
            )
        }
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFE5E7EB)),
            contentAlignment = Alignment.Center,
        ) {
            if (!photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = name.take(1).uppercase(),
                    color = Color(0xFF374151),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                color = Color(0xFF111827),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp),
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(soulBadgeIconForSoul(soul)),
                    contentDescription = "Soul badge",
                    modifier = Modifier
                        .height(16.dp)
                        .then(if (onBadgeClick != null) Modifier.clickable(onClick = onBadgeClick) else Modifier),
                )
            }
        }
        if (valueLabel.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_soul_cute_ghost),
                    contentDescription = "Soul",
                    modifier = Modifier.height(16.dp),
                )
                Text(
                    text = valueLabel,
                    color = Color(0xFFD97706),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

private fun formatContribution(value: Long): String = when {
    value >= 1_000_000L -> "${(value / 100_000L) / 10f}m"
    value >= 1_000L -> "${(value / 100L) / 10f}k"
    else -> value.toString()
}

private fun formatFullPoints(value: Long): String =
    NumberFormat.getIntegerInstance().format(value.coerceAtLeast(0L))

@Composable
private fun DropdownLikeAction(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium, color = Color(0xFF111827))
    }
}

@Composable
private fun RoomSeatRow(
    seatOrder: List<Int>,
    seatByNo: Map<Int, com.souljoy.soulmasti.domain.model.SocialVoiceSeat>,
    myUid: String?,
    ownerUid: String?,
    amOwnerOrAdmin: Boolean,
    onSeatAction: (seatNo: Int, occupiedBySelf: Boolean) -> Unit,
    canTakeSeat: (com.souljoy.soulmasti.domain.model.SocialVoiceSeat) -> Boolean,
    isSpeaking: (String?) -> Boolean,
    speakingLevel: (String?) -> Float,
    displayNameForUid: (String?) -> String?,
    profileUrlForUid: (String?) -> String?,
    ownerOnline: Boolean,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    val twoSeatRow = seatOrder.size == 2
    Row(
        modifier = modifier,
        horizontalArrangement = if (twoSeatRow) Arrangement.Center else Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        seatOrder.forEachIndexed { idx, no ->
            val seat = seatByNo[no] ?: return@forEachIndexed
            val isMyOwnerSeat = no == 1 && myUid != null && myUid == ownerUid
            val occupiedBySelf = myUid != null && (seat.occupantUid == myUid || isMyOwnerSeat)
            val speakingUid = when {
                occupiedBySelf -> myUid
                no == 1 && !ownerUid.isNullOrBlank() -> ownerUid
                else -> seat.occupantUid
            }
            val canTake = canTakeSeat(seat)
            val isOwnerSeat = no == 1
            val ownerIsOfflineSeat = isOwnerSeat && !ownerOnline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .then(if (twoSeatRow) Modifier.wrapContentWidth() else Modifier)
                    .clickable {
                        onSeatAction(no, occupiedBySelf)
                    }
                    .padding(horizontal = 2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 88.dp else 100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSpeaking(speakingUid)) {
                        SpeakingWaveRings(
                            speakingLevel = speakingLevel(speakingUid),
                            ringColor = Color(0xFF22D3EE),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(if (isCompact) 46.dp else 58.dp)
                            .border(
                                width = if (isSpeaking(speakingUid)) 2.dp else 1.dp,
                                color = if (isSpeaking(speakingUid)) Color(0xFF22D3EE) else Color(
                                    0xFFD1B96D
                                ),
                                shape = RoundedCornerShape(999.dp),
                            )
                            .background(
                                color = if (seat.locked) Color(0xFF3F1D1D) else Color(0x330B1220),
                                shape = RoundedCornerShape(999.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (seat.occupantUid.isNullOrBlank() && !ownerIsOfflineSeat) "+" else "",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        val photoUrl = profileUrlForUid(speakingUid)
                        if (!photoUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "User profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(999.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        }
                        if (seat.locked) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(14.dp).align(Alignment.TopEnd),
                            )
                        }
                        if (seat.muted) {
                            Box(
                                modifier = Modifier
                                    .size(if (isCompact) 18.dp else 22.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 1.dp, y = 1.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MicOff,
                                    contentDescription = "Muted",
                                    tint = Color(0xFF22C5D6),
                                    modifier = Modifier.size(if (isCompact) 12.dp else 14.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        ownerIsOfflineSeat -> "Owner offline"
                        !displayNameForUid(speakingUid).isNullOrBlank() -> displayNameForUid(speakingUid).orEmpty()
                        !seat.occupantName.isNullOrBlank() -> seat.occupantName
                        else -> "Seat $no"
                    },
                    color = Color(0xFFE2E8F0),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

