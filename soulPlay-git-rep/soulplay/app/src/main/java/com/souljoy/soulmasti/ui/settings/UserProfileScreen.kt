package com.souljoy.soulmasti.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.domain.gift.GiftCatalog
import com.souljoy.soulmasti.domain.gift.GiftSendContext
import com.souljoy.soulmasti.domain.model.GameHistoryEntry
import com.souljoy.soulmasti.domain.model.MatchOutcome
import com.souljoy.soulmasti.domain.repository.GiftRepository
import com.souljoy.soulmasti.domain.repository.SocialRepository
import com.souljoy.soulmasti.ui.common.gift.GiftCelebrationOverlayHost
import com.souljoy.soulmasti.ui.common.gift.rememberGiftCelebrationQueue
import com.souljoy.soulmasti.ui.voice.game.GiftWallDialog
import com.souljoy.soulmasti.ui.voice.game.defaultGiftWallItems
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.compose.koinInject
import java.util.Locale

private data class RemoteProfileData(
    val username: String,
    val profilePictureUrl: String?,
    val signature: String?,
    val soul: Long?,
    val friendUids: Set<String>,
    val receivedGiftHistory: List<ReceivedGiftSummary>,
    val gameHistory: List<GameHistoryEntry>,
    val historyUsernames: Map<String, String>,
    val userProfilePhotos: Map<String, String>,
)

@Composable
fun UserProfileScreen(
    uid: String,
    onBack: () -> Unit,
    onOpenChatThread: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val giftCelebration = rememberGiftCelebrationQueue()
    val social: SocialRepository = koinInject()
    val giftsRepo: GiftRepository = koinInject()
    val myUid = FirebaseAuth.getInstance().currentUser?.uid
    val incomingRequests by social.incomingFriendRequests.collectAsStateWithLifecycle()
    val friends by social.friends.collectAsStateWithLifecycle()
    var currentUid by remember(uid) { mutableStateOf(uid) }
    var state by remember(currentUid) { mutableStateOf<RemoteProfileData?>(null) }
    var showGiftWall by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showGiftHistory by remember { mutableStateOf(false) }
    var showSendGiftPicker by remember { mutableStateOf(false) }
    var giftSending by remember { mutableStateOf(false) }
    var outgoingPending by remember(currentUid) { mutableStateOf(false) }
    var relationError by remember { mutableStateOf<String?>(null) }
    var refreshNonce by remember(currentUid) { mutableIntStateOf(0) }
    var isFetching by remember(currentUid) { mutableStateOf(true) }
    var myCoins by remember(myUid) { mutableStateOf<Long?>(null) }
    var showProfileMenu by remember(currentUid) { mutableStateOf(false) }

    val isSelfProfile = myUid != null && currentUid == myUid
    val isFriend = friends.contains(currentUid)
    val hasIncomingRequest = incomingRequests.any { it.fromUid == currentUid }
    val copyUidAndToast: (String) -> Unit = { uid ->
        if (uid.isNotBlank()) {
            val short = uid.take(6).uppercase(Locale.US)
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard?.setPrimaryClip(ClipData.newPlainText("ID", short))
            Toast.makeText(context, "Copied successfully", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentUid, myUid, isSelfProfile) {
        val viewerUid = myUid ?: return@LaunchedEffect
        if (isSelfProfile) return@LaunchedEffect
        val db = FirebaseDatabase.getInstance().reference
        val viewerSnap = runCatching { db.child("users").child(viewerUid).get().await() }.getOrNull()
        val viewerName = viewerSnap?.child("username")?.getValue(String::class.java)?.trim()
            ?.takeIf { it.isNotBlank() } ?: FirebaseUidMapping.shortLabel(viewerUid)
        val viewerPhoto = viewerSnap?.child("profilePictureUrl")?.getValue(String::class.java)?.trim().orEmpty()
        val viewerGender = viewerSnap?.child("gender")?.getValue(String::class.java)?.trim().orEmpty()
        runCatching {
            db.child("users")
                .child(currentUid)
                .child("visitorDictionaryV1")
                .child("views")
                .child(viewerUid)
                .setValue(
                    mapOf(
                        "uid" to viewerUid,
                        "username" to viewerName,
                        "profilePictureUrl" to viewerPhoto,
                        "gender" to viewerGender,
                        "viewedAt" to System.currentTimeMillis(),
                    ),
                )
                .await()
        }
    }

    LaunchedEffect(currentUid, myUid) {
        if (myUid == null || currentUid == myUid) {
            outgoingPending = false
        } else {
            outgoingPending = runCatching {
                FirebaseFirestore.getInstance()
                    .collection("friendRequests")
                    .document("${myUid}_${currentUid}")
                    .get()
                    .await()
                    .let { it.exists() && it.getString("status") == "pending" }
            }.getOrDefault(false)
        }
    }
    LaunchedEffect(myUid, showSendGiftPicker) {
        if (!showSendGiftPicker) return@LaunchedEffect
        val senderUid = myUid ?: return@LaunchedEffect
        val value = runCatching {
            FirebaseDatabase.getInstance()
                .reference
                .child("users")
                .child(senderUid)
                .child("totalWinnings")
                .get()
                .await()
                .value
        }.getOrNull()
        myCoins = when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Double -> value.toLong()
            is Float -> value.toLong()
            is String -> value.trim().toLongOrNull()
            is Number -> value.toLong()
            else -> myCoins
        }
    }

    DisposableEffect(currentUid) {
        val root = FirebaseDatabase.getInstance().reference
        val bumpListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                refreshNonce += 1
            }
            override fun onCancelled(error: DatabaseError) = Unit
        }
        val userRef = root.child("users").child(currentUid)
        val giftsRef = root.child("users").child(currentUid).child("giftsReceived")
        val roomsRef = root.child("rooms")
        userRef.addValueEventListener(bumpListener)
        giftsRef.addValueEventListener(bumpListener)
        roomsRef.addValueEventListener(bumpListener)
        val friendsReg: ListenerRegistration = FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUid)
            .collection("friends")
            .addSnapshotListener { _, _ -> refreshNonce += 1 }

        onDispose {
            userRef.removeEventListener(bumpListener)
            giftsRef.removeEventListener(bumpListener)
            roomsRef.removeEventListener(bumpListener)
            friendsReg.remove()
        }
    }

    LaunchedEffect(currentUid, refreshNonce) {
        isFetching = true
        val db = FirebaseDatabase.getInstance().reference
        val userSnap = runCatching { db.child("users").child(currentUid).get().await() }.getOrNull()
        val username = userSnap?.child("username")?.getValue(String::class.java)?.takeIf { it.isNotBlank() }
            ?: FirebaseUidMapping.shortLabel(currentUid)
        val profilePictureUrl = userSnap?.child("profilePictureUrl")?.getValue(String::class.java)?.takeIf { it.isNotBlank() }
        val signature = userSnap?.child("signature")?.getValue(String::class.java)?.takeIf { it.isNotBlank() }
            ?: userSnap?.child("bio")?.getValue(String::class.java)?.takeIf { it.isNotBlank() }
            ?: userSnap?.child("status")?.getValue(String::class.java)?.takeIf { it.isNotBlank() }
        val soul = userSnap.readLongFromSchema("soul", "soulValue", "charms")
        val friendUids = runCatching {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .collection("friends")
                .get()
                .await()
                .documents
                .map { it.id }
                .filter { it.isNotBlank() }
                .toSet()
        }.getOrDefault(emptySet())

        val senderNames = mutableMapOf<String, String>()
        val giftSnap = runCatching { db.child("users").child(currentUid).child("giftsReceived").get().await() }.getOrNull()
        val gifts = giftSnap?.children?.mapNotNull { child ->
            val coins = child.child("coins").getValue(Long::class.java) ?: return@mapNotNull null
            val soul = child.child("soul").getValue(Long::class.java)
                ?: child.child("receiverSoul").getValue(Long::class.java)
                ?: 0L
            val selectedCount = child.child("selectedCount").getValue(Int::class.java)
                ?: child.child("selectedCount").getValue(Long::class.java)?.toInt()
                ?: 1
            val fromUserId = child.child("fromUserId").getValue(String::class.java)
            val fromDisplayName = fromUserId?.let { senderUid ->
                senderNames[senderUid] ?: run {
                    val senderSnap = runCatching { db.child("users").child(senderUid).get().await() }.getOrNull()
                    val name = senderSnap?.child("username")?.getValue(String::class.java)?.trim()?.takeIf { it.isNotBlank() }
                    if (name != null) senderNames[senderUid] = name
                    name
                }
            }
            ReceivedGiftSummary(
                fromUserId = fromUserId,
                fromDisplayName = fromDisplayName,
                giftId = child.child("giftId").getValue(String::class.java),
                coins = coins,
                soul = soul,
                selectedCount = selectedCount.coerceAtLeast(1),
                createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L,
            )
        }?.sortedByDescending { it.createdAt } ?: emptyList()

        val roomsSnap = runCatching { db.child("rooms").get().await() }.getOrNull()
        val history = mutableListOf<GameHistoryEntry>()
        val profileNames = mutableMapOf<String, String>()
        val profilePhotos = mutableMapOf<String, String>()
        profilePictureUrl?.let { profilePhotos[currentUid] = it }
        profileNames[currentUid] = username
        roomsSnap?.children?.forEach { room ->
            if (!room.child("players").hasChild(currentUid)) return@forEach
            val status = room.child("status").getValue(String::class.java)
            if (status != "ENDED") return@forEach
            val roomId = room.key.orEmpty()
            val endedAt = room.child("timerEndAt").getValue(Long::class.java)
                ?: room.child("createdAt").getValue(Long::class.java)
                ?: 0L
            val role = room.child("gameState").child("roles").child(currentUid).getValue(String::class.java)
            val points = room.child("gameState").child("scores").child(currentUid).getValue(Long::class.java)
            val scores = room.child("gameState").child("scores").children
                .mapNotNull { scoreNode ->
                    val scoreUid = scoreNode.key ?: return@mapNotNull null
                    val score = scoreNode.getValue(Long::class.java) ?: return@mapNotNull null
                    scoreUid to score
                }
                .toMap()
            val outcome = when (points) {
                null -> MatchOutcome.UNKNOWN
                0L -> MatchOutcome.LOST
                else -> MatchOutcome.WIN
            }
            history += GameHistoryEntry(
                id = roomId,
                roomId = roomId,
                endedAt = endedAt,
                userId = currentUid,
                role = role,
                gamePoints = points,
                outcome = outcome,
                result = room.child("endReason").getValue(String::class.java),
                summary = room.child("message").getValue(String::class.java),
                scoresByUser = scores,
            )
            scores.keys.forEach { scoreUid ->
                if (profileNames.containsKey(scoreUid)) return@forEach
                val peerSnap = runCatching { db.child("users").child(scoreUid).get().await() }.getOrNull()
                peerSnap?.child("username")?.getValue(String::class.java)?.takeIf { it.isNotBlank() }?.let {
                    profileNames[scoreUid] = it
                }
                peerSnap?.child("profilePictureUrl")?.getValue(String::class.java)?.takeIf { it.isNotBlank() }?.let {
                    profilePhotos[scoreUid] = it
                }
            }
        }

        state = RemoteProfileData(
            username = username,
            profilePictureUrl = profilePictureUrl,
            signature = signature,
            soul = soul,
            friendUids = friendUids,
            receivedGiftHistory = gifts,
            gameHistory = history.sortedByDescending { it.endedAt },
            historyUsernames = profileNames,
            userProfilePhotos = profilePhotos,
        )
        isFetching = false
    }

    val loaded = state
    if (loaded == null) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF6EFEF)) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Color(0xFF9CA3AF).copy(alpha = 0.82f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(34.dp),
                            strokeWidth = 2.6.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Loading...", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            key(currentUid) {
                ProfilePreviewFullPage(
                    profilePictureUrl = loaded.profilePictureUrl,
                    currentUsername = loaded.username,
                    signature = loaded.signature,
                    displayedUserId = currentUid,
                    onCopyUserId = { copyUidAndToast(currentUid) },
                    soul = loaded.soul,
                    receivedGiftHistory = loaded.receivedGiftHistory,
                    friendUids = loaded.friendUids,
                    historyUsernames = loaded.historyUsernames,
                    userProfilePhotos = loaded.userProfilePhotos,
                    gameHistory = loaded.gameHistory,
                    onBack = onBack,
                    onOpenGiftWall = { showGiftWall = true },
                    onOpenStats = { showStats = true },
                    contentBottomPadding = 0.dp,
                    onUserAvatarClick = { tappedUid ->
                        if (tappedUid.isNotBlank()) {
                            currentUid = tappedUid
                        }
                        showGiftWall = false
                        showGiftHistory = false
                        showStats = false
                    },
                )
            }
            if (isFriend) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 2.dp, end = 2.dp)
                ) {
                    IconButton(onClick = { showProfileMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "Profile options",
                            tint = Color(0xFF4B5563)
                        )
                    }
                    DropdownMenu(
                        expanded = showProfileMenu,
                        onDismissRequest = { showProfileMenu = false }
                    ) {
                        if (isFriend) {
                            DropdownMenuItem(
                                text = { Text("Remove Friend") },
                                onClick = {
                                    showProfileMenu = false
                                    scope.launch {
                                        val result = social.removeFriend(currentUid)
                                        result.onFailure { relationError = it.message ?: "Could not remove friend" }
                                        result.onSuccess { outgoingPending = false }
                                    }
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("No actions") },
                                onClick = { showProfileMenu = false }
                            )
                        }
                    }
                }
            }
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            if (!showGiftWall && !showGiftHistory && !showStats) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(start = 14.dp, end = 14.dp, bottom = bottomInset + 14.dp)
                        .fillMaxWidth()
                ) {
                    if (isSelfProfile || isFriend) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFFF85D8A).copy(alpha = 0.80f),
                                            Color(0xFFF58AB8).copy(alpha = 0.80f)
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                                )
                                .clickable { showSendGiftPicker = true }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Gift", color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF57AEE8).copy(alpha = 0.80f),
                                            Color(0xFF33C8F0).copy(alpha = 0.80f)
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                                )
                                .clickable { onOpenChatThread(currentUid) }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Chat", color = Color.White)
                            }
                        }
                    } else {
                        val buttonText = if (hasIncomingRequest) "Accept Request" else if (outgoingPending) "Request Sent" else "Add Friend"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color(0xFF57AEE8).copy(alpha = 0.80f),
                                            Color(0xFF33C8F0).copy(alpha = 0.80f)
                                        )
                                    ),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp)
                                )
                                .clickable(enabled = !outgoingPending) {
                                    scope.launch {
                                        relationError = null
                                        val result = if (hasIncomingRequest) {
                                            social.acceptFriendRequest(currentUid)
                                        } else {
                                            social.sendFriendRequest(currentUid)
                                        }
                                        result.onFailure { relationError = it.message ?: "Action failed" }
                                        result.onSuccess { if (!hasIncomingRequest) outgoingPending = true }
                                    }
                                }
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (hasIncomingRequest) Icons.Filled.Person else Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(buttonText, color = Color.White)
                            }
                        }
                    }
                }
            }
            if (isFetching) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(
                            Color(0xFF9CA3AF).copy(alpha = 0.78f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(30.dp),
                            strokeWidth = 2.4.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading...", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                    }
                }
            }
        }
        relationError?.let { err ->
            LaunchedEffect(err) { Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
        }
        if (showGiftWall) {
            GiftWallShowcaseDialog(
                gifts = loaded.receivedGiftHistory,
                userProfilePhotos = loaded.userProfilePhotos,
                onOpenHistory = { showGiftHistory = true },
                onClose = { showGiftWall = false },
                onUserAvatarClick = { tappedUid ->
                    if (tappedUid.isNotBlank()) {
                        currentUid = tappedUid
                    }
                    showGiftWall = false
                    showGiftHistory = false
                    showStats = false
                }
            )
        }
        if (showGiftHistory) {
            GiftHistoryListDialog(
                gifts = loaded.receivedGiftHistory,
                userProfilePhotos = loaded.userProfilePhotos,
                onClose = { showGiftHistory = false },
                onUserAvatarClick = { tappedUid ->
                    if (tappedUid.isNotBlank()) {
                        currentUid = tappedUid
                    }
                    showGiftWall = false
                    showGiftHistory = false
                    showStats = false
                }
            )
        }
        if (showStats) {
            StatsShowcaseDialog(
                imageUrl = loaded.profilePictureUrl,
                history = loaded.gameHistory,
                usernamesByUid = loaded.historyUsernames,
                userProfilePhotos = loaded.userProfilePhotos,
                onClose = { showStats = false },
                onUserAvatarClick = { tappedUid ->
                    if (tappedUid.isNotBlank()) {
                        currentUid = tappedUid
                    }
                    showGiftWall = false
                    showGiftHistory = false
                    showStats = false
                }
            )
        }
        GiftWallDialog(
            visible = showSendGiftPicker,
            recipientDisplayName = loaded.username,
            availableCoins = myCoins,
            items = remember { defaultGiftWallItems() },
            sending = giftSending,
            errorMessage = relationError,
            onDismiss = {
                showSendGiftPicker = false
                relationError = null
            },
            onSend = { giftId, selectedCount ->
                scope.launch {
                    giftSending = true
                    relationError = null
                    val sender = myUid
                    if (sender.isNullOrBlank()) {
                        relationError = "Not signed in"
                        giftSending = false
                        return@launch
                    }
                    val chatId = listOf(sender, currentUid).sorted().joinToString("_")
                    val sendResult = giftsRepo.sendGift(
                        context = GiftSendContext.Chat(chatId),
                        giftId = giftId,
                        recipientUserId = currentUid,
                        selectedCount = selectedCount,
                    )
                    sendResult
                        .onSuccess { r ->
                            val giftLabel = GiftCatalog.displayLabel(giftId)
                            val unitCoins = GiftCatalog.priceCoinsOrNull(giftId)?.toLong() ?: 0L
                            val giftCoins = unitCoins * selectedCount.toLong()
                            val giftMsg = "GIFT|$giftId|$giftLabel|$giftCoins|${r.receiverCoins}|${r.receiverSoul}|$selectedCount"
                            social.sendChatMessage(currentUid, giftMsg)
                            giftCelebration.enqueueIfFx(context, giftId)
                            showSendGiftPicker = false
                        }
                        .onFailure { e -> relationError = e.message ?: "Gift send failed" }
                    giftSending = false
                }
            }
        )
        GiftCelebrationOverlayHost(queue = giftCelebration)
    }
}

private fun DataSnapshot?.readLongFromSchema(vararg keys: String): Long? {
    val root = this ?: return null
    for (key in keys) {
        val child = root.child(key)
        child.getValue(Long::class.java)?.let { return it }
        child.getValue(Int::class.java)?.toLong()?.let { return it }
        child.getValue(Double::class.java)?.toLong()?.let { return it }
        child.getValue(String::class.java)?.trim()?.toLongOrNull()?.let { return it }
    }
    return null
}
