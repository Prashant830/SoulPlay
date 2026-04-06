package com.masti.soulplay.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Man
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Woman
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.masti.soulplay.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.masti.soulplay.data.firebase.FirebaseUidMapping
import com.masti.soulplay.domain.gift.GiftCatalog
import com.masti.soulplay.domain.model.GameHistoryEntry
import com.masti.soulplay.domain.model.MatchOutcome
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    onLogout: () -> Unit = {},
) {
    val totalWinnings by viewModel.totalWinnings.collectAsStateWithLifecycle()
    val giftReceivedCoins by viewModel.giftReceivedCoins.collectAsStateWithLifecycle()
    val giftReceivedCount by viewModel.giftReceivedCount.collectAsStateWithLifecycle()
    val receivedGiftHistory by viewModel.receivedGiftHistory.collectAsStateWithLifecycle()
    val gameHistory by viewModel.gameHistory.collectAsStateWithLifecycle()
    val currentUsername by viewModel.username.collectAsStateWithLifecycle()
    val profilePictureUrl by viewModel.profilePictureUrl.collectAsStateWithLifecycle()
    val gender by viewModel.gender.collectAsStateWithLifecycle()
    val historyUsernames by viewModel.historyUsernames.collectAsStateWithLifecycle()

    var usernameDraft by remember { mutableStateOf(currentUsername.orEmpty()) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showGiftHistoryDialog by remember { mutableStateOf(false) }

    val pickProfilePhoto = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploadingPhoto = true
            photoError = null
            try {
                viewModel.updateProfilePicture(uri)
            } catch (e: Exception) {
                photoError = e.message ?: "Upload failed"
            } finally {
                uploadingPhoto = false
            }
        }
    }

    LaunchedEffect(currentUsername) {
        usernameDraft = currentUsername.orEmpty()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfilePictureDisplay(
                        imageUrl = profilePictureUrl,
                        displayName = currentUsername,
                        gender = gender,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                pickProfilePhoto.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !uploadingPhoto,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uploadingPhoto) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Change photo")
                        }
                    }
                    if (!profilePictureUrl.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    uploadingPhoto = true
                                    photoError = null
                                    try {
                                        viewModel.clearProfilePictureToDefault()
                                    } catch (e: Exception) {
                                        photoError = e.message
                                    } finally {
                                        uploadingPhoto = false
                                    }
                                }
                            },
                            enabled = !uploadingPhoto
                        ) {
                            Text("Use default mascot")
                        }
                    }
                    photoError?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Text(
                        text = "Username",
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = usernameDraft,
                        onValueChange = { usernameDraft = it },
                        label = { Text("Enter username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch { viewModel.updateUsername(usernameDraft) }
                        },
                        enabled = usernameDraft.trim().isNotBlank() && usernameDraft.trim() != currentUsername?.trim(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Logout")
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showGiftHistoryDialog = true },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    width = 0.4.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Total winnings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = totalWinnings?.let { "\uD83E\uDE99 $it" } ?: "—",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tap to see coin/gift history",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Charms system removed – gold only now.
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    width = 0.4.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.14f)
                ),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Gift wall",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = giftReceivedCoins?.let { "Gift coins received: 🧧 $it" } ?: "Gift coins received: —",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = giftReceivedCount?.let { "Total gifts: $it" } ?: "Total gifts: —",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Text(
                text = "Game history",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Finished rooms you played in. Tap Details on a row to expand.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }
        if (gameHistory.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Text(
                        text = "No games yet. Finish a match to see it here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            items(gameHistory, key = { it.id }) { entry ->
                GameHistoryRow(entry = entry, usernamesByUid = historyUsernames)
            }
        }
    }

    if (showGiftHistoryDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showGiftHistoryDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Gift history",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showGiftHistoryDialog = false }) {
                            Text("Close")
                        }
                    }
                    if (receivedGiftHistory.isEmpty()) {
                        Text(
                            text = "No gifts yet. When someone sends you a gift, it will appear here.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(receivedGiftHistory) { g ->
                                val senderLabel = g.fromDisplayName
                                    ?: g.fromUserId?.let { FirebaseUidMapping.shortLabel(it) }
                                    ?: "Someone"
                                val giftLabel = g.giftId?.let { GiftCatalog.displayLabel(it) } ?: "Gift"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$senderLabel · $giftLabel",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "+${g.coins} coins",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatHistoryDate(g.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfilePictureDisplay(
    imageUrl: String?,
    displayName: String?,
    gender: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNullOrBlank()) {
                Image(
                    painter = painterResource(R.drawable.ic_mascot_hero),
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.ic_mascot_hero),
                    error = painterResource(R.drawable.ic_mascot_hero),
                )
            }
        }
        displayName?.takeIf { it.isNotBlank() }?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        GenderWithSign(gender = gender)
    }
}

@Composable
private fun GenderWithSign(gender: String?) {
    val raw = gender?.trim()?.takeIf { it.isNotBlank() } ?: return
    val key = raw.lowercase(Locale.getDefault())
    val (icon, sign, label) = when (key) {
        "male" -> Triple(Icons.Outlined.Man, "♂", "Male")
        "female" -> Triple(Icons.Outlined.Woman, "♀", "Female")
        "other" -> Triple(Icons.Outlined.Person, "○", "Other")
        else -> Triple(Icons.Outlined.Person, "○", raw)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$sign $label",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private val historyDateFormat =
    SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())

@Composable
private fun GameHistoryRow(
    entry: GameHistoryEntry,
    usernamesByUid: Map<String, String>,
) {
    var expanded by remember(entry.id) { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(
            width = 0.35.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        )
    ) {
        val toggleInteraction = remember(entry.id) { MutableInteractionSource() }
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Room ${FirebaseUidMapping.shortLabel(entry.roomId)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatHistoryDate(entry.endedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutcomeChip(outcome = entry.outcome)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your role: ${formatHistoryRole(entry.role)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (expanded) "Hide details" else "Details",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable(
                            interactionSource = toggleInteraction,
                            indication = null
                        ) { expanded = !expanded }
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 2.dp)) {
                    GameHistoryDetails(entry = entry, usernamesByUid = usernamesByUid)
                }
            }
        }
    }
}

@Composable
private fun OutcomeChip(outcome: MatchOutcome) {
    val (label, bg, fg) = when (outcome) {
        MatchOutcome.WIN -> Triple("Win", Color(0xFFDCFCE7), Color(0xFF166534))
        MatchOutcome.LOST -> Triple("Lost", Color(0xFFFEE2E2), Color(0xFF991B1B))
        MatchOutcome.UNKNOWN -> Triple("—", Color(0xFFF1F5F9), Color(0xFF64748B))
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = fg,
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun GameHistoryDetails(
    entry: GameHistoryEntry,
    usernamesByUid: Map<String, String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Game points",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            entry.gamePoints?.let { pts ->
                Text(
                    text = "$pts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } ?: Text(text = "—", style = MaterialTheme.typography.bodyMedium)
        }
        entry.userId?.takeIf { it.isNotBlank() }?.let { uid ->
            Text(
                text = "User: ${displayNameForUid(uid, usernamesByUid)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Room id: ${entry.roomId}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "Reason: ${entry.result ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        if (entry.scoresByUser.isNotEmpty()) {
            Text(
                text = "Room scores (all players)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp)
            )
            entry.scoresByUser.entries
                .sortedBy { it.key }
                .forEach { (playerUid, pts) ->
                    val isYou = entry.userId != null && playerUid == entry.userId
                    Text(
                        text = buildString {
                            append(displayNameForUid(playerUid, usernamesByUid))
                            append(": ")
                            append(pts)
                            append(" pts")
                            if (isYou) append(" (you)")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }
        entry.summary?.takeIf { it.isNotBlank() }?.let { s ->
            Text(
                text = s,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun displayNameForUid(uid: String, usernamesByUid: Map<String, String>): String {
    return usernamesByUid[uid]?.takeIf { it.isNotBlank() } ?: FirebaseUidMapping.shortLabel(uid)
}

private fun formatHistoryDate(endedAt: Long): String {
    if (endedAt <= 0L) return "—"
    return historyDateFormat.format(Date(endedAt))
}

private fun formatHistoryRole(role: String?): String = when (role?.uppercase(Locale.US)) {
    "RAJA" -> "Raja"
    "MANTRI" -> "Mantri"
    "CHOR" -> "Chor"
    "SIPAHI" -> "Sipahi"
    null, "" -> "—"
    else -> role
}
