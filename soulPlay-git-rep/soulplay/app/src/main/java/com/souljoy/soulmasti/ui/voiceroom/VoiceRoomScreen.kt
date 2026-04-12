package com.souljoy.soulmasti.ui.voiceroom

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souljoy.soulmasti.BuildConfig
import com.souljoy.soulmasti.R
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.domain.model.GamePhase
import com.souljoy.soulmasti.domain.model.GameRoomSnapshot
import com.souljoy.soulmasti.domain.model.VoiceConnectionState
import com.souljoy.soulmasti.ui.common.gift.GiftCelebrationOverlayHost
import com.souljoy.soulmasti.ui.common.gift.rememberGiftCelebrationQueue
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

private val RoyalCourtBgTop = Color(0xFFF8FAFC)
private val RoyalCourtBgBottom = Color(0xFFEFF6FF)
private val AccentBlue = Color(0xFF2563EB)
private val AccentPink = Color(0xFFEC4899)
private val LiveGreen = Color(0xFF22C55E)
private val SeatNeutralRing = Color(0xFF64748B)

private sealed interface PhaseTimerDisplay {
    data class Countdown(val seconds: Long) : PhaseTimerDisplay
    data class ServerWait(val sinceExpirySec: Int) : PhaseTimerDisplay
}

private data class EmojiFloatUi(
    val key: String,
    val emoji: String,
    val hFraction: Float,
    val senderUid: String,
)

private val RoomQuickReactionEmojis =
    listOf("😀", "😂", "😍", "🔥", "👏", "🎉", "❤️", "😮", "🙏", "💯", "😈", "🤫")

private fun roleRingColor(roleRaw: String?): Color = when (roleRaw?.uppercase()) {
    "RAJA" -> Color(0xFFD97706)
    "MANTRI" -> Color(0xFF2563EB)
    "CHOR" -> Color(0xFFDC2626)
    "SIPAHI" -> Color(0xFF059669)
    else -> SeatNeutralRing
}

private fun formatRoleLabel(roleRaw: String?): String = when (roleRaw?.uppercase()) {
    "RAJA" -> "Raja"
    "MANTRI" -> "Mantri"
    "CHOR" -> "Chor"
    "SIPAHI" -> "Sipahi"
    else -> if (roleRaw.isNullOrBlank()) "—" else roleRaw
}

private fun isGameEnded(room: GameRoomSnapshot?): Boolean =
    room != null && (room.status == "ENDED" || room.phase == GamePhase.GAME_OVER)

/** 1-based seat number in room order (same as grid). */
private fun seatNumberForFirebaseUid(room: GameRoomSnapshot, firebaseUid: String): Int {
    val uids = room.players.keys.sorted()
    val idx = uids.indexOf(firebaseUid)
    return if (idx >= 0) idx + 1 else -1
}

private data class PlayerSlotUi(
    val seatIndex: Int,
    val firebaseUid: String?,
    val agoraUid: Int?,
    val isEmpty: Boolean,
    val ringColor: Color,
    /** Only non-null for this device’s player — never show other seats’ roles. */
    val roleLabel: String?,
    /** Round points for this seat when the match has finished. */
    val roundPoints: Long?,
    /** From RTDB `players/{uid}/online`; empty seats are treated as online for UI. */
    val isOnline: Boolean,
    val displayName: String?,
    val profilePictureUrl: String?,
)

private data class SuspectOption(
    val firebaseUid: String,
    /** Matches table seat number (e.g. 1, 2, 4 when you occupy seat 3). */
    val label: String,
    val subtitle: String,
)

@Composable
fun VoiceRoomScreen(
    viewModel: VoiceRoomViewModel,
    hasVoicePermission: () -> Boolean,
    requestVoicePermission: () -> Unit,
    onRoomClosed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val participants by viewModel.participants.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val audioLevels by viewModel.audioLevelsByUid.collectAsStateWithLifecycle()
    val room by viewModel.roomSnapshot.collectAsStateWithLifecycle()
    val myUid by viewModel.myFirebaseUid.collectAsStateWithLifecycle()
    val userProfiles by viewModel.userProfiles.collectAsStateWithLifecycle()
    val coinBalance by viewModel.coinBalance.collectAsStateWithLifecycle()

    val inRoom = connectionState is VoiceConnectionState.InRoom
    val myRole = myUid?.let { uid -> room?.roles?.get(uid) }
    val phase = room?.phase ?: GamePhase.UNKNOWN
    val canMantriGuess =
        inRoom &&
            room?.status == "PLAYING" &&
            myRole == "MANTRI" &&
            phase == GamePhase.MANTRI_GUESS

    var showGuessDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var giftRecipientUid by remember { mutableStateOf<String?>(null) }
    var giftSending by remember { mutableStateOf(false) }
    var giftError by remember { mutableStateOf<String?>(null) }
    var playerMenuUid by remember { mutableStateOf<String?>(null) }
    var socialFeedback by remember { mutableStateOf<String?>(null) }
    var showGiftPlayerPicker by remember { mutableStateOf(false) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    BackHandler {
        when {
            showEmojiPicker -> showEmojiPicker = false
            giftRecipientUid != null -> {
                giftRecipientUid = null
                giftError = null
            }
            showGiftPlayerPicker -> showGiftPlayerPicker = false
            playerMenuUid != null -> playerMenuUid = null
            showGuessDialog -> showGuessDialog = false
            showExitDialog -> showExitDialog = false
            else -> viewModel.leaveRoom(onRoomClosed)
        }
    }
    val giftScope = rememberCoroutineScope()
    val emojiRemovalScope = rememberCoroutineScope()
    val emojiFloatItems = remember { mutableStateListOf<EmojiFloatUi>() }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val roomSnapshot = room
    val voiceJoinKey = "${viewModel.displayedRoomId}_${myUid.orEmpty()}"
    var voiceAutoJoined by remember(voiceJoinKey) { mutableStateOf(false) }
    LaunchedEffect(voiceJoinKey, hasVoicePermission()) {
        if (myUid.isNullOrBlank()) return@LaunchedEffect
        if (voiceAutoJoined) return@LaunchedEffect
        if (BuildConfig.AGORA_APP_ID.isBlank()) return@LaunchedEffect
        if (!hasVoicePermission()) return@LaunchedEffect
        delay(400)
        viewModel.joinVoiceChannel()
        voiceAutoJoined = true
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            tick = System.currentTimeMillis()
        }
    }
    LaunchedEffect(socialFeedback) {
        val msg = socialFeedback ?: return@LaunchedEffect
        if (msg.isNotBlank()) {
            delay(2800)
            socialFeedback = null
        }
    }
    val phaseTimerDisplay: PhaseTimerDisplay? = remember(room, tick) {
        val r = room ?: return@remember null
        val end = r.timerEndAt ?: return@remember null
        val playing = r.status == "PLAYING"
        val ph = r.phase
        if (!playing || ph == GamePhase.GAME_OVER || ph == GamePhase.UNKNOWN) return@remember null
        val raw = ((end - tick) / 1000L).coerceAtLeast(0L)
        if (raw > 0L) return@remember PhaseTimerDisplay.Countdown(raw)
        val grace = ((tick - end) / 1000L).coerceIn(0L, 300L).toInt()
        PhaseTimerDisplay.ServerWait(grace)
    }

    LaunchedEffect(viewModel) {
        viewModel.roomEmojiEvents.collect { ev ->
            val item = EmojiFloatUi(
                key = ev.eventId,
                emoji = ev.emoji,
                hFraction = Random.nextFloat(),
                senderUid = ev.fromUid,
            )
            emojiFloatItems.add(item)
            emojiRemovalScope.launch {
                delay(3200)
                emojiFloatItems.remove(item)
            }
        }
    }

    val slots = remember(roomSnapshot, myUid, userProfiles) {
        buildPlayerSlots(roomSnapshot, myUid, userProfiles)
    }

    val suspectOptions = remember(roomSnapshot, myUid, userProfiles) {
        val r = roomSnapshot
        val uid = myUid
        if (r == null || uid == null) emptyList()
        else {
            r.players.keys
                .filter { it != uid }
                .sorted()
                .map { id ->
                    val seatNo = seatNumberForFirebaseUid(r, id)
                    SuspectOption(
                        firebaseUid = id,
                        label = "Seat $seatNo",
                        subtitle = userProfiles[id]?.username?.takeIf { it.isNotBlank() }
                            ?: FirebaseUidMapping.shortLabel(id)
                    )
                }
        }
    }

    var selectedSuspectUid by remember { mutableStateOf("") }
    LaunchedEffect(suspectOptions) {
        selectedSuspectUid = suspectOptions.firstOrNull()?.firebaseUid.orEmpty()
    }

    val centerLabel = centerActionLabel(
        inRoom = inRoom,
        room = room,
        canMantriGuess = canMantriGuess,
        makeGuessLabel = stringResource(R.string.center_action_make_guess),
    )
    val centerEnabled =
        connectionState !is VoiceConnectionState.Connecting &&
            (!inRoom || canMantriGuess)

    val giftRecipientLabel = giftRecipientUid?.let { uid ->
        userProfiles[uid]?.username?.takeIf { it.isNotBlank() }
            ?: FirebaseUidMapping.shortLabel(uid)
    }.orEmpty()
    val friendRequestSentText = stringResource(R.string.friend_request_sent)
    val requestAlreadySentText = stringResource(R.string.request_already_sent)
    val friendRequestFailedText = stringResource(R.string.friend_request_failed)
    val sendGiftFailedText = stringResource(R.string.send_gift_failed)
    val context = LocalContext.current
    val giftCelebration = rememberGiftCelebrationQueue()
    var giftBannerUi by remember { mutableStateOf<GiftBannerUi?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.giftBannerEvents.collect { banner ->
            giftCelebration.enqueueIfFx(context, banner.giftId) {
                giftBannerUi = banner
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                Brush.verticalGradient(
                    listOf(RoyalCourtBgTop, RoyalCourtBgBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            RoomTopBar(
                inRoom = inRoom,
                roomLabel = viewModel.displayedRoomId,
                onExitClick = { showExitDialog = true }
            )
            Spacer(modifier = Modifier.height(12.dp))
            val bannerRoom = roomSnapshot
            if (bannerRoom != null) {
                GameStateBanner(
                    room = bannerRoom,
                    phase = phase,
                    phaseTimer = phaseTimerDisplay,
                    voicePeers = participants.size
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isGameEnded(bannerRoom)) {
                    GameEndScoresCard(room = bannerRoom, myUid = myUid, userProfiles = userProfiles)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else {
                Text(
                    text = "Loading game from Firebase…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Players",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    !inRoom -> "Connecting to voice…"
                    else -> "${participants.size} connected in voice (channel = game id)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                slots.chunked(2).forEach { rowSlots ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowSlots.forEach { slot ->
                            val level = slot.agoraUid?.let { uid -> audioLevels[uid] } ?: 0f
                            val onSeatClick = when {
                                slot.isEmpty || slot.firebaseUid == null -> null
                                else -> {
                                    {
                                        giftError = null
                                        playerMenuUid = slot.firebaseUid
                                    }
                                }
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                PlayerSlotCard(
                                    slot = slot,
                                    speakingLevel = level,
                                    onSlotClick = onSeatClick
                                )
                            }
                        }
                    }
                }
            }
            if (!inRoom) {
                Spacer(modifier = Modifier.height(12.dp))
                VoiceRoomLobbyHint(connectionState = connectionState)
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        // Float above the seat cards without taking space in the scroll column (no layout jump).
        if (emojiFloatItems.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 76.dp)
                    .height(168.dp)
                    .graphicsLayer { clip = false },
            ) {
                emojiFloatItems.forEach { item ->
                    key(item.key) {
                        val senderLabel = emojiSenderLabel(
                            senderUid = item.senderUid,
                            myUid = myUid,
                            userProfiles = userProfiles,
                        )
                        FloatingRoomEmoji(
                            emoji = item.emoji,
                            horizontalFraction = item.hFraction,
                            senderLabel = senderLabel,
                        )
                    }
                }
            }
        }

        GiftBannerOverlay(
            banner = giftBannerUi,
            onBannerCycleFinished = { giftBannerUi = null },
        )

        socialFeedback?.takeIf { it.isNotBlank() }?.let { msg ->
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 88.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0F172A).copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(14.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        playerMenuUid?.let { targetUid ->
            VoiceRoomPlayerActionDialog(
                targetUid = targetUid,
                myUid = myUid,
                profile = userProfiles[targetUid],
                onDismiss = { playerMenuUid = null },
                onAddFriend = {
                    viewModel.sendFriendRequest(targetUid) { result ->
                        playerMenuUid = null
                        socialFeedback = result.fold(
                            onSuccess = { friendRequestSentText },
                            onFailure = { e ->
                                when {
                                    e.message?.contains("already sent", ignoreCase = true) == true ->
                                        requestAlreadySentText
                                    else -> e.message ?: friendRequestFailedText
                                }
                            }
                        )
                    }
                },
                onSendGift = {
                    giftRecipientUid = targetUid
                    playerMenuUid = null
                    giftError = null
                },
                accentBlue = AccentBlue,
                accentPink = AccentPink,
            )
        }

        if (showGiftPlayerPicker) {
            val others = slots
                .filter { s ->
                    !s.isEmpty &&
                        s.firebaseUid != null &&
                        (myUid == null || s.firebaseUid != myUid)
                }
                .mapNotNull { s ->
                    val uid = s.firebaseUid ?: return@mapNotNull null
                    val label = userProfiles[uid]?.username?.takeIf { it.isNotBlank() }
                        ?: FirebaseUidMapping.shortLabel(uid)
                    uid to label
                }
            VoiceRoomGiftRecipientDialog(
                others = others,
                onDismiss = { showGiftPlayerPicker = false },
                onPickRecipient = { uid ->
                    giftRecipientUid = uid
                    showGiftPlayerPicker = false
                }
            )
        }

        GiftWallDialog(
            visible = giftRecipientUid != null,
            recipientDisplayName = giftRecipientLabel.ifBlank { stringResource(R.string.gift_recipient_fallback) },
            items = remember { defaultGiftWallItems() },
            sending = giftSending,
            errorMessage = giftError,
            onDismiss = {
                giftRecipientUid = null
                giftError = null
            },
            onSend = { giftId ->
                val toUid = giftRecipientUid ?: return@GiftWallDialog
                giftSending = true
                giftError = null
                giftScope.launch {
                    val r = viewModel.sendGift(toUid, giftId)
                    giftSending = false
                    r.onSuccess {
                        giftRecipientUid = null
                        giftError = null
                    }
                    r.onFailure { e ->
                        giftError = e.message ?: sendGiftFailedText
                    }
                }
            }
        )

        VoiceActionBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            inRoom = inRoom,
            isMuted = isMuted,
            onMicClick = {
                if (inRoom) viewModel.toggleMute()
            },
            onGiftClick = {
                giftError = null
                showGiftPlayerPicker = true
            },
            onEmojiClick = {
                if (inRoom) showEmojiPicker = true
            },
            emojiContentDescription = stringResource(R.string.room_emoji_cd),
            onCenterClick = {
                when {
                    canMantriGuess -> showGuessDialog = true
                    !inRoom -> {
                        if (hasVoicePermission()) viewModel.joinVoiceChannel()
                        else requestVoicePermission()
                    }
                }
            },
            centerEnabled = centerEnabled,
            centerLabel = centerLabel
        )

        if (showEmojiPicker) {
            RoomEmojiPickerDialog(
                onDismiss = { showEmojiPicker = false },
                onPick = { em ->
                    viewModel.sendRoomEmoji(em)
                    showEmojiPicker = false
                },
            )
        }

        if (showGuessDialog && suspectOptions.isNotEmpty()) {
            MantriGuessDialog(
                suspects = suspectOptions,
                selectedUid = selectedSuspectUid.ifEmpty { suspectOptions.first().firebaseUid },
                onSelectedChange = { selectedSuspectUid = it },
                onDismiss = { showGuessDialog = false },
                onConfirm = {
                    val pick = selectedSuspectUid.ifEmpty { suspectOptions.first().firebaseUid }
                    viewModel.submitMantriGuess(pick)
                    showGuessDialog = false
                }
            )
        }

        if (showExitDialog) {
            LeaveGameDialog(
                room = roomSnapshot,
                onDismiss = { showExitDialog = false },
                onConfirmLeave = {
                    showExitDialog = false
                    viewModel.leaveRoom(onRoomClosed)
                }
            )
        }

        GiftCelebrationOverlayHost(queue = giftCelebration)
    }
}

@Composable
private fun VoiceRoomLobbyHint(connectionState: VoiceConnectionState) {
    val message = when (connectionState) {
        is VoiceConnectionState.Connecting -> "Connecting to the room…"
        is VoiceConnectionState.Error ->
            "Could not join: ${connectionState.message}. Tap Join voice room below to try again."
        else ->
            "Tap Join voice room below to connect. Seats stay empty until you're in the room and others join."
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Lobby",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )
        }
    }
}

@Composable
private fun GameEndScoresCard(
    room: GameRoomSnapshot,
    myUid: String?,
    userProfiles: Map<String, VoiceUserProfile>,
) {
    val uids = room.players.keys.sorted()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Round points",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF166534)
            )
            uids.forEachIndexed { index, uid ->
                val seatNo = index + 1
                val role = formatRoleLabel(room.roles[uid])
                val pts = room.scores[uid] ?: 0L
                val you = myUid != null && uid == myUid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Seat $seatNo · $role${if (you) " (you)" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (you) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 2
                        )
                        Text(
                            text = userProfiles[uid]?.username?.takeIf { it.isNotBlank() }
                                ?: FirebaseUidMapping.shortLabel(uid),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                    Text(
                        text = "$pts pts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue
                    )
                }
            }
            if (uids.isEmpty()) {
                Text(
                    text = "No score data yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun GameStateBanner(
    room: GameRoomSnapshot,
    phase: GamePhase,
    phaseTimer: PhaseTimerDisplay?,
    voicePeers: Int
) {
    val accent = gamePhaseAccentColor(phase)
    val phaseTitle = gamePhaseTitleString(phase)
    val phaseBody = gamePhaseBodyString(phase)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accent),
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = phaseTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                )
                Text(
                    text = phaseBody,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = stringResource(R.string.game_room_status_line, room.status),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 10.dp),
                )
                if (room.status == "ENDED" || phase == GamePhase.GAME_OVER) {
                    val reason = room.endReason?.trim().orEmpty()
                    val msg = room.message?.trim().orEmpty()
                    if (reason.isNotEmpty()) {
                        Text(
                            text = "End reason: $reason",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                    if (msg.isNotEmpty() && msg != reason) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = if (reason.isNotEmpty()) 4.dp else 8.dp),
                        )
                    }
                    if (reason.isEmpty() && msg.isEmpty()) {
                        Text(
                            text = "End reason: —",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    when (val t = phaseTimer) {
                        is PhaseTimerDisplay.Countdown -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accent.copy(alpha = 0.12f),
                            ) {
                                Text(
                                    text = stringResource(R.string.game_timer_remaining, t.seconds.toInt()),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }
                        is PhaseTimerDisplay.ServerWait -> {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFFFF7ED),
                                ) {
                                    Text(
                                        text = stringResource(R.string.game_timer_server_wait, t.sinceExpirySec),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFC2410C),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                                Text(
                                    text = stringResource(R.string.game_timer_server_wait_hint),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                )
                            }
                        }
                        null -> {}
                    }
                    Text(
                        text = stringResource(R.string.game_voice_peers_line, voicePeers),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                    )
                }
            }
        }
    }
}

private fun gamePhaseAccentColor(phase: GamePhase): Color = when (phase) {
    GamePhase.RAJA_DECISION -> Color(0xFFD97706)
    GamePhase.MANTRI_CONFIRM -> Color(0xFF2563EB)
    GamePhase.VOICE_DISCUSSION -> Color(0xFF0D9488)
    GamePhase.MANTRI_GUESS -> Color(0xFFDB2777)
    GamePhase.GAME_OVER -> Color(0xFF64748B)
    GamePhase.UNKNOWN -> Color(0xFF94A3B8)
}

@Composable
private fun gamePhaseTitleString(phase: GamePhase): String = stringResource(
    when (phase) {
        GamePhase.RAJA_DECISION -> R.string.game_phase_raja_title
        GamePhase.MANTRI_CONFIRM -> R.string.game_phase_mantri_confirm_title
        GamePhase.VOICE_DISCUSSION -> R.string.game_phase_voice_discussion_title
        GamePhase.MANTRI_GUESS -> R.string.game_phase_mantri_guess_title
        GamePhase.GAME_OVER -> R.string.game_phase_game_over_title
        GamePhase.UNKNOWN -> R.string.game_phase_unknown_title
    },
)

@Composable
private fun gamePhaseBodyString(phase: GamePhase): String = stringResource(
    when (phase) {
        GamePhase.RAJA_DECISION -> R.string.game_phase_raja_body
        GamePhase.MANTRI_CONFIRM -> R.string.game_phase_mantri_confirm_body
        GamePhase.VOICE_DISCUSSION -> R.string.game_phase_voice_discussion_body
        GamePhase.MANTRI_GUESS -> R.string.game_phase_mantri_guess_body
        GamePhase.GAME_OVER -> R.string.game_phase_game_over_body
        GamePhase.UNKNOWN -> R.string.game_phase_unknown_body
    },
)

@Composable
private fun MantriGuessDialog(
    suspects: List<SuspectOption>,
    selectedUid: String,
    onSelectedChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pick Chor (by seat)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Choices use the same seat numbers as the table (your seat is not listed).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
                suspects.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = opt.firebaseUid == selectedUid,
                                onClick = { onSelectedChange(opt.firebaseUid) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = opt.firebaseUid == selectedUid,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = opt.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = opt.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun emojiSenderLabel(
    senderUid: String,
    myUid: String?,
    userProfiles: Map<String, VoiceUserProfile>,
): String {
    if (myUid != null && senderUid == myUid) {
        return stringResource(R.string.you)
    }
    return userProfiles[senderUid]?.username?.takeIf { it.isNotBlank() }
        ?: FirebaseUidMapping.shortLabel(senderUid)
}

@Composable
private fun FloatingRoomEmoji(
    emoji: String,
    horizontalFraction: Float,
    senderLabel: String,
) {
    val offsetY by animateFloatAsState(
        targetValue = -120f,
        animationSpec = tween(2600, easing = FastOutSlowInEasing),
        label = "roomEmojiY",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(
                    x = ((horizontalFraction - 0.5f) * 200).dp,
                    y = offsetY.dp,
                )
                .widthIn(max = 140.dp),
        ) {
            Text(
                text = emoji,
                fontSize = 40.sp,
            )
            Text(
                text = senderLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RoomEmojiPickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.room_emoji_picker_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RoomQuickReactionEmojis.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        row.forEach { em ->
                            TextButton(onClick = { onPick(em) }) {
                                Text(em, fontSize = 28.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

private fun centerActionLabel(
    inRoom: Boolean,
    room: GameRoomSnapshot?,
    canMantriGuess: Boolean,
    makeGuessLabel: String,
): String = when {
    !inRoom -> "JOIN VOICE"
    canMantriGuess -> makeGuessLabel
    room?.status == "ENDED" -> "ENDED"
    else -> "IN VOICE"
}

@Composable
private fun LeaveGameDialog(
    room: GameRoomSnapshot?,
    onDismiss: () -> Unit,
    onConfirmLeave: () -> Unit
) {
    val ended = room?.status == "ENDED"
    val title = if (ended) "Match finished" else "Leave game?"
    val body = buildString {
        if (ended) {
            room?.message?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            room?.endReason?.takeIf { it.isNotBlank() }?.let {
                if (isNotEmpty()) appendLine()
                append("Reason: ")
                append(it)
            }
            if (isEmpty()) append("Returning home.")
        } else {
            append("You will leave voice, mark yourself offline, and return home. ")
            append("If the round is still active, the host may end it for others.")
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body.trim()) },
        confirmButton = {
            TextButton(onClick = onConfirmLeave) {
                Text(if (ended) "OK" else "Leave")
            }
        },
        dismissButton = {
            if (!ended) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun RoomTopBar(inRoom: Boolean, roomLabel: String, onExitClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            Text(
                text = "Room ${FirebaseUidMapping.shortLabel(roomLabel)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = roomLabel,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (inRoom) LiveGreen else Color(0xFFCBD5E1))
                )
                Text(
                    text = if (inRoom) "LIVE" else "LOBBY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (inRoom) LiveGreen else Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = Color(0xFF64748B))
            }
            Button(
                onClick = onExitClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                shape = RoundedCornerShape(10.dp),
                contentPadding = ButtonDefaults.ContentPadding
            ) {
                Text("EXIT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SpeakingWaveRings(
    speakingLevel: Float,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    val active = speakingLevel > 0.035f
    val transition = rememberInfiniteTransition(label = "speakRings")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringPhase"
    )
    if (!active) return
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        for (i in 2 downTo 0) {
            val stagger = i / 3f
            val wave = (sin((phase + stagger) * 2 * PI).toFloat() * 0.5f + 0.5f)
            val scale = 1f + 0.1f * wave + 0.12f * speakingLevel.coerceIn(0f, 1f)
            val alpha = (0.5f - i * 0.14f) * (0.4f + 0.6f * speakingLevel)
            Box(
                modifier = Modifier
                    .size((76 + (i + 1) * 12).dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .border(2.dp, ringColor.copy(alpha = 0.7f), CircleShape)
            )
        }
    }
}

@Composable
private fun PlayerSlotCard(
    slot: PlayerSlotUi,
    speakingLevel: Float,
    onSlotClick: (() -> Unit)? = null,
) {
    val ringWidth = if (slot.isEmpty) 2.dp else 3.5.dp
    val offline = !slot.isEmpty && !slot.isOnline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onSlotClick != null && !slot.isEmpty) {
                    Modifier.clickable(onClick = onSlotClick)
                } else {
                    Modifier
                }
            )
            .graphicsLayer { alpha = if (offline) 0.48f else 1f },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!slot.isEmpty && slot.isOnline) {
                        SpeakingWaveRings(
                            speakingLevel = speakingLevel,
                            ringColor = slot.ringColor,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .border(ringWidth, slot.ringColor, CircleShape)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(
                                if (slot.isEmpty) Color(0xFFF1F5F9)
                                else slot.ringColor.copy(alpha = 0.25f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (slot.isEmpty) {
                            Text(
                                text = "?",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        } else {
                            val imageUrl = slot.profilePictureUrl.orEmpty()
                            if (imageUrl.isBlank()) {
                                Image(
                                    painter = painterResource(R.drawable.ic_mascot_hero),
                                    contentDescription = "Profile photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(R.drawable.ic_mascot_hero),
                                    error = painterResource(R.drawable.ic_mascot_hero),
                                )
                            }
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 10.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = slot.ringColor
                ) {
                    Text(
                        text = if (slot.isEmpty) "—" else "Seat ${slot.seatIndex + 1}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (slot.isEmpty) "Open seat"
                else slot.displayName?.takeIf { it.isNotBlank() } ?: "Player",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            if (offline) {
                Text(
                    text = "Offline",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (!slot.isEmpty && slot.roleLabel != null) {
                Text(
                    text = slot.roleLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = slot.ringColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (slot.roundPoints != null) {
                Text(
                    text = "${slot.roundPoints} pts",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun VoiceActionBar(
    modifier: Modifier = Modifier,
    inRoom: Boolean,
    isMuted: Boolean,
    onMicClick: () -> Unit,
    onGiftClick: () -> Unit,
    onEmojiClick: () -> Unit,
    emojiContentDescription: String,
    onCenterClick: () -> Unit,
    centerEnabled: Boolean,
    centerLabel: String
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        color = Color.White.copy(alpha = 0.96f)
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp, top = 10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onMicClick,
                    enabled = inRoom,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = AccentBlue.copy(alpha = 0.12f))
                ) {
                    Icon(
                        Icons.Filled.Mic,
                        contentDescription = "Mic",
                        tint = when {
                            !inRoom -> Color(0xFFCBD5E1)
                            isMuted -> Color(0xFF94A3B8)
                            else -> AccentBlue
                        }
                    )
                }
                Button(
                    onClick = onCenterClick,
                    enabled = centerEnabled,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = centerLabel,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onGiftClick, enabled = inRoom) {
                    Icon(
                        Icons.Filled.CardGiftcard,
                        contentDescription = "Gift",
                        tint = if (inRoom) AccentPink else Color(0xFFCBD5E1)
                    )
                }
                IconButton(onClick = onEmojiClick, enabled = inRoom) {
                    Icon(
                        Icons.Filled.Mood,
                        contentDescription = emojiContentDescription,
                        tint = if (inRoom) Color(0xFF64748B) else Color(0xFFCBD5E1)
                    )
                }
            }
        }
    }
}

private fun buildPlayerSlots(
    room: GameRoomSnapshot?,
    myUid: String?,
    userProfiles: Map<String, VoiceUserProfile>,
): List<PlayerSlotUi> {
    val uids = room?.players?.keys?.sorted().orEmpty()
    val ended = isGameEnded(room)
    val scores = room?.scores ?: emptyMap()
    return (0 until 4).map { index ->
        val firebaseUid = uids.getOrNull(index)
        val isEmpty = firebaseUid == null
        val agoraUid = firebaseUid?.let { FirebaseUidMapping.agoraUidFromFirebaseUid(it) }
        val roleRaw = firebaseUid?.let { room?.roles?.get(it) }
        val isMe = myUid != null && firebaseUid == myUid
        val roundPoints =
            if (ended && firebaseUid != null) scores[firebaseUid] else null
        val isOnline = if (isEmpty) {
            true
        } else {
            room?.players?.get(firebaseUid)?.online == true
        }
        PlayerSlotUi(
            seatIndex = index,
            firebaseUid = firebaseUid,
            agoraUid = agoraUid,
            isEmpty = isEmpty,
            ringColor = when {
                isEmpty -> SeatNeutralRing
                isMe -> roleRingColor(roleRaw)
                else -> SeatNeutralRing
            },
            roleLabel = when {
                isEmpty -> null
                isMe -> formatRoleLabel(roleRaw)
                else -> null
            },
            roundPoints = roundPoints,
            isOnline = isOnline,
            displayName = firebaseUid?.let { uid ->
                userProfiles[uid]?.username?.takeIf { it.isNotBlank() }
            },
            profilePictureUrl = firebaseUid?.let { uid ->
                userProfiles[uid]?.profilePictureUrl?.takeIf { it.isNotBlank() }
            },
        )
    }
}
