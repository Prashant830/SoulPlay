package com.souljoy.soulmasti.ui.voiceroom

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.ui.common.SpeakingWaveRings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SocialVoiceRoomScreen(
    viewModel: SocialVoiceRoomViewModel,
    hasVoicePermission: () -> Boolean,
    requestVoicePermission: () -> Unit,
    onBack: () -> Unit,
    onOpenUserProfile: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val room by viewModel.room.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val participants by viewModel.participants.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val audioLevels by viewModel.audioLevelsByUid.collectAsStateWithLifecycle()
    val incomingInvite by viewModel.incomingInvite.collectAsStateWithLifecycle()
    val userProfiles by viewModel.userProfiles.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val myUid = viewModel.myUid
    var showRoomMenu by remember { mutableStateOf(false) }
    var selectedSeatNo by remember { mutableStateOf<Int?>(null) }
    var showInvitePickerForSeat by remember { mutableStateOf<Int?>(null) }
    var joinTickerText by remember { mutableStateOf<String?>(null) }
    var showRenameRoomDialog by remember { mutableStateOf(false) }
    var renameDraft by remember { mutableStateOf("") }
    var profileDialogSeatNo by remember { mutableStateOf<Int?>(null) }

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
    val roomActive = room?.ownerOnline == true && room?.collapsed != true
    val seatByNo = remember(seats) { seats.associateBy { it.seatNo } }
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
                    colors = listOf(Color(0xFF080B2D), Color(0xFF090D3B), Color(0xFF101943)),
                ),
            ),
    ) {
        // Decorative neon glow blobs similar to reference.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x993B82F6), Color(0x664C1D95)),
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val canRenameRoom = myUid != null && myUid == room?.ownerUid
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Column {
                        Text(
                            text = room?.roomName ?: "music ♪",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = if (canRenameRoom) {
                                Modifier.clickable {
                                    renameDraft = room?.roomName ?: ""
                                    showRenameRoomDialog = true
                                }
                            } else Modifier,
                        )
                        Text(
                            text = "Room ${room?.roomId?.takeLast(6) ?: viewModel.currentRoomId.takeLast(6)} • ${room?.onlineCount ?: 0} online",
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { viewModel.toggleMute() }) {
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Mute",
                            tint = Color.White,
                        )
                    }
                    Box {
                        IconButton(onClick = { showRoomMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Room menu",
                                tint = Color.White,
                            )
                        }
                        DropdownMenu(expanded = showRoomMenu, onDismissRequest = { showRoomMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Leave Room") },
                                onClick = {
                                    showRoomMenu = false
                                    viewModel.leaveRoom(onDone = onBack)
                                },
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
                colors = CardDefaults.cardColors(containerColor = Color(0xB30F172A)),
                modifier = Modifier.weight(1f),
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
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(messages) { m ->
                            Text(
                                text = "${m.fromName}: ${m.text}",
                                color = Color(0xFFE2E8F0),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        ) {
                            Text("Send")
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
                                viewModel.showInfo("Gift feature coming soon")
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

    val inviteSeatNo = showInvitePickerForSeat
    if (inviteSeatNo != null) {
        val onlineUsers = room?.onlineUids.orEmpty().filter { it != myUid }.sorted()
        AlertDialog(
            onDismissRequest = { showInvitePickerForSeat = null },
            title = { Text("Invite to seat $inviteSeatNo") },
            text = {
                if (onlineUsers.isEmpty()) {
                    Text("No online users available in this room.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        onlineUsers.forEach { uid ->
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

    if (showRenameRoomDialog) {
        AlertDialog(
            onDismissRequest = { showRenameRoomDialog = false },
            title = { Text("Change room name") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it.take(40) },
                    singleLine = true,
                    placeholder = { Text("Enter room name") },
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
}

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
                if (seat.muted) {
                    Text(
                        "Muted",
                        color = Color(0xFFFCA5A5),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                if (idx != seatOrder.lastIndex) {
                    if (twoSeatRow) {
                        Spacer(modifier = Modifier.width(18.dp))
                    } else {
                        Text(
                            text = "〰",
                            color = Color(0xAA94A3B8),
                            modifier = Modifier.padding(top = if (isCompact) 14.dp else 18.dp),
                        )
                    }
                }
            }
        }
    }
}

