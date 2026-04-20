package com.souljoy.soulmasti.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Man
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Woman
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Shop
import androidx.compose.material.icons.outlined.Signpost
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.souljoy.soulmasti.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.souljoy.soulmasti.data.firebase.FirebaseUidMapping
import com.souljoy.soulmasti.domain.gift.GiftCatalog
import com.souljoy.soulmasti.domain.gift.GiftFxResources
import com.souljoy.soulmasti.domain.model.GameHistoryEntry
import com.souljoy.soulmasti.domain.model.MatchOutcome
import com.souljoy.soulmasti.domain.repository.SocialRepository
import com.souljoy.soulmasti.ui.common.SoulBadgeTier
import com.souljoy.soulmasti.ui.common.SoulBadgeTiers
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// 0xFFFFF8F8 profile colour
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
    onLogout: () -> Unit = {},
    onOpenUserProfile: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val socialRepository: SocialRepository = koinInject()
    val friends by socialRepository.friends.collectAsStateWithLifecycle()
    val totalWinnings by viewModel.totalWinnings.collectAsStateWithLifecycle()
    val giftReceivedCoins by viewModel.giftReceivedCoins.collectAsStateWithLifecycle()
    val giftReceivedCount by viewModel.giftReceivedCount.collectAsStateWithLifecycle()
    val receivedGiftHistory by viewModel.receivedGiftHistory.collectAsStateWithLifecycle()
    val gameHistory by viewModel.gameHistory.collectAsStateWithLifecycle()
    val currentUsername by viewModel.username.collectAsStateWithLifecycle()
    val profilePictureUrl by viewModel.profilePictureUrl.collectAsStateWithLifecycle()
    val gender by viewModel.gender.collectAsStateWithLifecycle()
    val signature by viewModel.signature.collectAsStateWithLifecycle()
    val historyUsernames by viewModel.historyUsernames.collectAsStateWithLifecycle()
    val userProfilePhotos by viewModel.userProfilePhotos.collectAsStateWithLifecycle()
    val helpCenterToastText = stringResource(R.string.settings_help_center_toast)
    val securityCenterToastText = stringResource(R.string.settings_security_center_toast)
    val inviteComingSoonToastText = stringResource(R.string.settings_invite_coming_soon)

    var usernameDraft by remember { mutableStateOf(currentUsername.orEmpty()) }
    var signatureDraft by remember { mutableStateOf(signature.orEmpty()) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var showGiftHistoryDialog by remember { mutableStateOf(false) }
    var showGiftHistoryListDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showNicknameEditor by remember { mutableStateOf(false) }
    var showSignatureEditor by remember { mutableStateOf(false) }
    var showSoulLevelDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    val topGiftBreakdown = remember(receivedGiftHistory) { buildTopGiftBreakdown(receivedGiftHistory) }

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
    LaunchedEffect(signature) {
        signatureDraft = signature.orEmpty()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(Color(0xFFF6F6F6))
            .padding(bottom = 10.dp),
    ) {
        item {
            ProfileHeroSection(
                displayName = currentUsername.orEmpty().ifBlank { stringResource(R.string.profile_default_name) },
                imageUrl = profilePictureUrl,
                onAvatarClick = {
                    FirebaseAuth.getInstance().currentUser?.uid?.takeIf { it.isNotBlank() }?.let(onOpenUserProfile)
                },
                onHeaderClick = { showEditProfileDialog = true }
            )
        }
        item {
            ShortcutRow(
                onShopClick = {},
                onStatsClick = { showStatsDialog = true },
                onGiftClick = { showGiftHistoryDialog = true }
            )
        }


        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsNavRow(stringResource(R.string.settings_stats), stringResource(R.string.settings_rounds_count, gameHistory.size), leftIcon = Icons.Outlined.Leaderboard) { showStatsDialog = true }
                    SettingsNavRow(stringResource(R.string.settings_help_center), "", leftIcon = Icons.Outlined.HelpOutline) {
                        Toast.makeText(context, helpCenterToastText, Toast.LENGTH_LONG).show()
                    }
                    SettingsNavRow(stringResource(R.string.settings_privacy_policy), "", leftIcon = Icons.Outlined.Policy) { showPrivacyPolicyDialog = true }
                     }
            }
        }
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp)
            ) {
                SettingsNavRow(stringResource(R.string.settings_terms_conditions), "", leftIcon = Icons.Outlined.Description) { showTermsDialog = true }
                SettingsNavRow(stringResource(R.string.settings_language), "English", leftIcon = Icons.Outlined.Language) { }
                SettingsNavRow(stringResource(R.string.settings_region), "India", leftIcon = Icons.Outlined.Map) { }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp)
            ) {
                SettingsNavRow(
                    stringResource(R.string.settings_soul_level),
                    "",
                    leftIcon = Icons.Outlined.Leaderboard
                ) { showSoulLevelDialog = true }
                SettingsNavRow(stringResource(R.string.settings_security_center), "", leftIcon = Icons.Outlined.Security) {
                    Toast.makeText(context, securityCenterToastText, Toast.LENGTH_LONG).show()
                }
                SettingsNavRow(stringResource(R.string.settings_invite_friends), "", leftIcon = Icons.Outlined.Person) {
                    Toast.makeText(context, inviteComingSoonToastText, Toast.LENGTH_SHORT).show()
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(0.dp)
            ) {
                SettingsNavRow(stringResource(R.string.settings_logout), "", leftIcon = Icons.Outlined.Shop) {
                    showLogoutConfirmDialog = true
                }
            }
        }
    }

    if (showPrivacyPolicyDialog) {
        PolicyLikePageDialog(
            title = stringResource(R.string.settings_privacy_policy),
            body = stringResource(R.string.settings_privacy_policy_body),
            onClose = { showPrivacyPolicyDialog = false }
        )
    }
    if (showTermsDialog) {
        PolicyLikePageDialog(
            title = stringResource(R.string.settings_terms_conditions),
            body = stringResource(R.string.settings_terms_body),
            onClose = { showTermsDialog = false }
        )
    }
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text(stringResource(R.string.settings_logout_confirm_title)) },
            text = { Text(stringResource(R.string.settings_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirmDialog = false
                        viewModel.logout()
                        onLogout()
                    }
                ) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) { Text(stringResource(R.string.no)) }
            }
        )
    }

    if (showGiftHistoryDialog) {
        GiftWallShowcaseDialog(
            gifts = receivedGiftHistory,
            userProfilePhotos = userProfilePhotos,
            onOpenHistory = { showGiftHistoryListDialog = true },
            onClose = { showGiftHistoryDialog = false },
            onUserAvatarClick = { uid ->
                if (uid.isNotBlank()) {
                    onOpenUserProfile(uid)
                }
            }
        )
    }
    if (showGiftHistoryListDialog) {
        GiftHistoryListDialog(
            gifts = receivedGiftHistory,
            userProfilePhotos = userProfilePhotos,
            onClose = { showGiftHistoryListDialog = false },
            onUserAvatarClick = { uid ->
                if (uid.isNotBlank()) {
                    onOpenUserProfile(uid)
                }
            }
        )
    }

    if (showEditProfileDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showEditProfileDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.16f))
            ) {
                Column(modifier = Modifier.wrapContentHeight()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f))
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable { showEditProfileDialog = false }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Edit Profile",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp)
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.padding(horizontal = 10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)),
                                border = BorderStroke(0.4.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Column {
                                    EditProfileRow(
                                        label = "Avatar",
                                        value = "",
                                        customTrailing = {
                                            ProfileAvatarOnly(imageUrl = profilePictureUrl) {
                                                pickProfilePhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                            }
                                        }
                                    ) {
                                        pickProfilePhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                    HorizontalDivider(color = Color(0xFFEDEDED))
                                    EditProfileRow(label = "Nickname", value = usernameDraft.ifBlank { "Set nickname" }) { showNicknameEditor = true }
                                    HorizontalDivider(color = Color(0xFFEDEDED))
                                    EditProfileRow(label = "User ID", value = "—") {}
                                }
                            }
                        }
                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.padding(horizontal = 10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f)),
                                border = BorderStroke(0.4.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            ) {
                                Column {
                                    EditProfileRow(label = "Gender", value = gender ?: "Not set") {}
                                    HorizontalDivider(color = Color(0xFFEDEDED))
                                    EditProfileRow(label = "Region", value = "India") {}
                                    HorizontalDivider(color = Color(0xFFEDEDED))
                                    EditProfileRow(
                                        label = stringResource(R.string.edit_profile_signature),
                                        value = signature.orEmpty().ifBlank { stringResource(R.string.signature_default) }
                                    ) { showSignatureEditor = true }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showNicknameEditor) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showNicknameEditor = false }) {
            Card(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(min = 280.dp, max = 360.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Edit nickname", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = usernameDraft,
                        onValueChange = { usernameDraft = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showNicknameEditor = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        Button(
                            onClick = {
                                scope.launch { viewModel.updateUsername(usernameDraft) }
                                showNicknameEditor = false
                            },
                            enabled = usernameDraft.trim().isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) { Text("Save") }
                    }
                }
            }
        }
    }
    if (showSignatureEditor) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showSignatureEditor = false }) {
            Card(
                modifier = Modifier
                    .padding(20.dp)
                    .widthIn(min = 280.dp, max = 360.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.edit_signature_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = signatureDraft,
                        onValueChange = { signatureDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showSignatureEditor = false },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.cancel)) }
                        Button(
                            onClick = {
                                scope.launch { viewModel.updateSignature(signatureDraft) }
                                showSignatureEditor = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.save)) }
                    }
                }
            }
        }
    }

    if (showStatsDialog) {
        StatsShowcaseDialog(
            imageUrl = profilePictureUrl,
            history = gameHistory,
            usernamesByUid = historyUsernames,
            userProfilePhotos = userProfilePhotos,
            onClose = { showStatsDialog = false },
            onUserAvatarClick = { uid ->
                if (uid.isNotBlank()) {
                    onOpenUserProfile(uid)
                }
            }
        )
    }
    if (showSoulLevelDialog) {
        SoulLevelDialog(
            onClose = { showSoulLevelDialog = false }
        )
    }
}

@Composable
private fun ShortcutRow(
    onShopClick: () -> Unit,
    onStatsClick: () -> Unit,
    onGiftClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ShortcutItem("Shop", Icons.Outlined.Shop, onShopClick)
        ShortcutItem("Stats", Icons.Outlined.Leaderboard, onStatsClick)
        ShortcutItem("Gift Wall", Icons.Outlined.EmojiEvents, onGiftClick)
    }
}

@Composable
private fun SoulLevelDialog(
    onClose: () -> Unit,
) {
    val conqueror = soulRankTiers.last()
    val regularBadges = soulRankTiers.dropLast(1)
    var selectedBadge by remember { mutableStateOf<SoulBadgeTier?>(null) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(25.dp)
                                .clickable { onClose() },
                            tint = Color(0xFF2D2D2D)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.settings_soul_level),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2D2D2D)
                        )
                    }
                }

                val badgeRows = remember(regularBadges) { regularBadges.chunked(3) }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(badgeRows.size) { rowIndex ->
                        val row = badgeRows[rowIndex]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                row.forEach { badge ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedBadge = badge },
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Image(
                                                painter = painterResource(badge.iconRes),
                                                contentDescription = null,
                                                modifier = Modifier.size(55.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                            Text(
                                                text = badge.soulLabel,
                                                color = Color(0xFF5B4B71),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                repeat(3 - row.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                            HorizontalDivider(
                                color = Color(0xFFE8DEF8),
                                thickness = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedBadge = conqueror },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Final Level", color = Color(0xFF9A6B2F), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                            Image(
                                painter = painterResource(conqueror.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = conqueror.soulLabel,
                                color = Color(0xFF8A5A24),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FB))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.how_to_increase_soul),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1F1F1F)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFE7FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.EmojiEvents,
                                            contentDescription = null,
                                            tint = Color(0xFF8D5BEB)
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = stringResource(R.string.receive_gifts),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF242424)
                                        )
                                        Text(
                                            text = stringResource(R.string.soul_hint_line),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color(0xFF8A8A96)
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

    selectedBadge?.let { badge ->
        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedBadge = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
                border = BorderStroke(1.dp, Color(0xFF4C3F66))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = badge.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Image(
                        painter = painterResource(badge.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(if (badge.iconRes == R.drawable.rank_conqueror) 116.dp else 84.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = "Required Souls: ${badge.soulLabel}",
                        color = Color(0xFFD7C9F2),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextButton(
                        onClick = { selectedBadge = null },
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF7C3AED), Color(0xFF9F67FF))),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(stringResource(R.string.ok), color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF315289)
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF1F2937))
    }
}

@Composable
internal fun ProfilePreviewFullPage(
    profilePictureUrl: String?,
    currentUsername: String?,
    signature: String?,
    soul: Long?,
    receivedGiftHistory: List<ReceivedGiftSummary>,
    friendUids: Set<String>,
    historyUsernames: Map<String, String>,
    userProfilePhotos: Map<String, String>,
    gameHistory: List<GameHistoryEntry>,
    onBack: () -> Unit,
    onOpenGiftWall: () -> Unit,
    onOpenStats: () -> Unit,
    onUserAvatarClick: (String) -> Unit,
    contentBottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val rankUi = remember(soul) { resolveSoulRankUi(soul) }
    val topGiftPreview = remember(receivedGiftHistory) {
        receivedGiftHistory
            .groupBy { it.giftId ?: "__unknown__" }
            .map { (giftId, rows) ->
                val senderCounts = rows.groupBy { it.fromUserId ?: "__unknown__" }.mapValues { entry ->
                    entry.value.sumOf { it.selectedCount.coerceAtLeast(1) }
                }
                val topSenderUid = senderCounts.maxByOrNull { it.value }?.key
                val topSenderCount = senderCounts[topSenderUid] ?: 0
                val topSenderName = rows.firstOrNull { it.fromUserId == topSenderUid }?.fromDisplayName
                    ?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                GiftWallCardUi(
                    giftId = giftId,
                    label = if (giftId == "__unknown__") "Gift" else GiftCatalog.displayLabel(giftId),
                    count = rows.sumOf { it.selectedCount.coerceAtLeast(1) },
                    coins = rows.sumOf { it.coins },
                    topSenderUid = topSenderUid,
                    topSenderName = topSenderName,
                    topSenderCount = topSenderCount,
                    priceCoins = GiftCatalog.priceCoinsOrNull(giftId)?.toLong() ?: 0L
                )
            }
            .sortedByDescending { it.coins }
            .take(4)
    }
    val totalGiftCount = remember(receivedGiftHistory) {
        receivedGiftHistory.sumOf { it.selectedCount.coerceAtLeast(1) }
    }
    val friendPreview = remember(friendUids, historyUsernames, userProfilePhotos) {
        friendUids
            .take(4)
            .map { uid ->
                FriendPreviewUi(
                    uid = uid,
                    name = historyUsernames[uid]?.takeIf { it.isNotBlank() } ?: FirebaseUidMapping.shortLabel(uid),
                    profileImageUrl = userProfilePhotos[uid]
                )
            }
    }
    val gamePreview = remember(gameHistory) { gameHistory.take(4) }
    val contributorsRanking = remember(receivedGiftHistory, historyUsernames, userProfilePhotos) {
        val realRanking = receivedGiftHistory
            .mapNotNull { it.fromUserId?.takeIf { uid -> uid.isNotBlank() } }
            .distinct()
            .map { uid ->
                val rows = receivedGiftHistory.filter { it.fromUserId == uid }
                ContributorRankUi(
                    uid = uid,
                    name = rows.firstOrNull { !it.fromDisplayName.isNullOrBlank() }?.fromDisplayName
                        ?: historyUsernames[uid]
                        ?: FirebaseUidMapping.shortLabel(uid),
                    coins = rows.sumOf { it.soul },
                    profileImageUrl = userProfilePhotos[uid]
                )
            }
            .sortedByDescending { it.coins }
        if (realRanking.size >= 4) {
            realRanking
        } else {
            val dummyUsers = (realRanking.size + 1..4).map { rank ->
                ContributorRankUi(
                    uid = "dummy_contributor_$rank",
                    name = "User $rank",
                    coins = 0L,
                    profileImageUrl = null
                )
            }
            realRanking + dummyUsers
        }
    }
    var selectedGift by remember(topGiftPreview) { mutableStateOf<GiftWallCardUi?>(null) }
    var showFriendsDialog by remember { mutableStateOf(false) }
    var showContributorsDialog by remember { mutableStateOf(false) }
    var showSoulLevelDialog by remember { mutableStateOf(false) }
    var fullscreenProfileImageUrl by remember { mutableStateOf<String?>(null) }
    val openSoulLevelDialog = { showSoulLevelDialog = true }
    val openUserProfileFromDialog: (String) -> Unit = { uid ->
        if (uid.isNotBlank() && isContributorProfileClickable(uid)) {
            // Close current overlays before opening next profile, so new profile is visible immediately.
            showFriendsDialog = false
            showContributorsDialog = false
            selectedGift = null
            onUserAvatarClick(uid)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .padding(bottom = contentBottomPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.clickable { onBack() }
                    )
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                  border = BorderStroke(0.6.dp, Color(0xFFE7D8D8))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(30.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SenderAvatarChip(
                                profileImageUrl = profilePictureUrl,
                                size = 130.dp,
                                onClick = {
                                    profilePictureUrl
                                        ?.trim()
                                        ?.takeIf { it.isNotBlank() }
                                        ?.let { fullscreenProfileImageUrl = it }
                                }
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = currentUsername.orEmpty().ifBlank { "Maya" },
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3F2E44)
                                    )
                                }
                                Text(
                                    signature.orEmpty().ifBlank { stringResource(R.string.signature_default) },
                                    color = Color(0xFF6B4E58)
                                )
                                Row(
                                    modifier = Modifier.clickable(onClick = openSoulLevelDialog),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rankUi.iconRes?.let { iconRes ->
                                        Image(
                                            painter = painterResource(iconRes),
                                            contentDescription = "${rankUi.name} rank",
                                            modifier = Modifier.size(34.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                    Text(
                                        rankUi.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF4B5563)
                                    )
                                }
                                Row(
                                    modifier = Modifier.clickable(onClick = openSoulLevelDialog),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.ic_soul_cute_ghost),
                                        contentDescription = "Soul",
                                        modifier = Modifier.size(20.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                    Text(
                                        text = formatSoulDisplay(soul),
                                        color = Color(0xFF4B5563),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            color = Color(0xFFEAEAEA),
                            thickness = 1.dp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    border = BorderStroke(0.6.dp, Color(0xFFE7D8D8))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Thanks for the gifts! 🏆",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5B4458)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (topGiftPreview.isEmpty()) {
                                GiftPreviewTile("🎁", "0", Modifier.weight(1f))
                            } else {
                                topGiftPreview.forEach { gift ->
                                    GiftPreviewTile(
                                        emoji = giftEmoji(gift.giftId),
                                        count = gift.count.toString(),
                                        modifier = Modifier.weight(1f),
                                        onClick = { selectedGift = gift }
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Gifts $totalGiftCount",
                                color = Color(0xFF6B7280),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "See all",
                                color = Color(0xFF7C3AED),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.clickable { onOpenGiftWall() }
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    border = BorderStroke(0.6.dp, Color(0xFFE7D8D8))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_signature),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = signature.orEmpty().ifBlank { stringResource(R.string.signature_default) },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF5E4B57)
                        )
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    border = BorderStroke(0.6.dp, Color(0xFFE7D8D8))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionEndArrowRow(
                            title = "Friends",
                            trailing = friendPreview.size.toString(),
                            onArrowClick = { showFriendsDialog = true }
                        )
                        if (friendPreview.isEmpty()) {
                            Text("No friends yet", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodyMedium)
                        } else {
                            friendPreview.take(4).chunked(2).forEach { rowItems ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    rowItems.forEach { friend ->
                                        FriendMiniClassicTile(
                                            name = friend.name,
                                            profileImageUrl = friend.profileImageUrl,
                                            onClick = { openUserProfileFromDialog(friend.uid) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .wrapContentHeight()
                                        )
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    border = BorderStroke(0.6.dp, Color(0xFFE7D8D8))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Contributors",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                contributorsRanking.take(3).forEach { g ->
                                    SenderAvatarChip(
                                        profileImageUrl = g.profileImageUrl,
                                        size = 42.dp,
                                        onClick = if (isContributorProfileClickable(g.uid)) {
                                            { openUserProfileFromDialog(g.uid) }
                                        } else {
                                            null
                                        }
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { showContributorsDialog = true },
                                    tint = Color(0xFF6B7280)
                                )
                            }
                        }
                        if (contributorsRanking.isEmpty()) {
                            Text("No contributors yet", color = Color(0xFF6B7280), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().padding(0.dp,0.dp,0.dp,70.dp)
//                    border = BorderStroke(0.6.dp, Color(0xFFE7D8D8))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionEndArrowRow(
                            title = "Stats",
                            trailing = "${gameHistory.size} Games",
                            onArrowClick = onOpenStats
                        )
                    }
                }
            }



        }
    }
    selectedGift?.let { gift ->
        GiftDetailDialog(
            gift = gift,
            userProfilePhotos = userProfilePhotos,
            onDismiss = { selectedGift = null },
            onUserAvatarClick = openUserProfileFromDialog
        )
    }
    if (showFriendsDialog) {
        FriendsShowcaseDialog(
            currentUsername = currentUsername.orEmpty().ifBlank { "You" },
            profilePictureUrl = profilePictureUrl,
            friendPreview = friendPreview,
            onClose = { showFriendsDialog = false },
            onUserAvatarClick = openUserProfileFromDialog
        )
    }
    if (showContributorsDialog) {
        ContributorsRankingDialog(
            currentUsername = currentUsername.orEmpty().ifBlank { "You" },
            profilePictureUrl = profilePictureUrl,
            ranking = contributorsRanking,
            onClose = { showContributorsDialog = false },
            onUserAvatarClick = openUserProfileFromDialog
        )
    }
    if (showSoulLevelDialog) {
        SoulLevelDialog(
            onClose = { showSoulLevelDialog = false }
        )
    }
    fullscreenProfileImageUrl?.let { imageUrl ->
        FullscreenProfileImageDialog(
            imageUrl = imageUrl,
            onClose = { fullscreenProfileImageUrl = null }
        )
    }
}

@Composable
private fun FullscreenProfileImageDialog(
    imageUrl: String,
    onClose: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClose() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Profile image preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentScale = ContentScale.Fit,
                placeholder = painterResource(R.drawable.ic_mascot_hero),
                error = painterResource(R.drawable.ic_mascot_hero),
            )
        }
    }
}


@Composable
private fun ProfilePictureDisplay(
    imageUrl: String?,
    displayName: String?,
    gender: String?,
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null,
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
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .let { base ->
                    if (onAvatarClick != null) base.clickable { onAvatarClick() } else base
                },
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
private fun ProfileTopBanner(
    displayName: String?,
    imageUrl: String?,
    gender: String?,
    onLeftClick: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onLeftClick() },
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Profile",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = displayName?.takeIf { it.isNotBlank() } ?: "Set your name",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to edit profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            ProfilePictureDisplay(
                imageUrl = imageUrl,
                displayName = null,
                gender = gender,
                onAvatarClick = onAvatarClick
            )
        }
    }
}

@Composable
private fun SettingsNavRow(
    label: String,
    value: String,
    leftIcon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = leftIcon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, style = MaterialTheme.typography.titleMedium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditProfileRow(
    label: String,
    value: String,
    customTrailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (customTrailing != null) {
                customTrailing()
                Spacer(modifier = Modifier.width(8.dp))
            } else if (value.isNotBlank()) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileHeroSection(
    displayName: String,
    imageUrl: String?,
    onAvatarClick: () -> Unit,
    onHeaderClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onHeaderClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ProfileAvatarOnly(
                    imageUrl = imageUrl,
                    onAvatarClick = onAvatarClick
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(displayName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Profile", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileAvatarOnly(
    imageUrl: String?,
    onAvatarClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(75.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .clickable { onAvatarClick() },
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
}

@Composable
private fun SectionNavRow(
    title: String,
    trailingText: String = "",
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (trailingText.isNotBlank()) {
                Text(trailingText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    color: Color,
) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.16f))
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun GiftPreviewTile(
    emoji: String,
    count: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4F4)),
//        border = BorderStroke(0.5.dp, Color(0xFFE7D8D8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Text("x $count", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4B5563), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionEndArrowRow(
    title: String,
    trailing: String,
    onArrowClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(trailing, color = Color(0xFF6B7280), style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                contentDescription = null,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onArrowClick() },
                tint = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
private fun FriendMiniTile(
    name: String,
    profileImageUrl: String?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(0.8.dp, Color(0xFFFBCFE8))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFBCFE8), Color(0xFFF9A8D4))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🛵",
                    style = MaterialTheme.typography.headlineSmall
                )
                SenderAvatarChip(profileImageUrl = profileImageUrl, size = 50.dp, onClick = onClick)
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    fontSize = 20.sp,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun FriendMiniClassicTile(
    name: String,
    profileImageUrl: String?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4F4)),
//        border = BorderStroke(0.5.dp, Color(0xFFE7D8D8))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SenderAvatarChip(profileImageUrl = profileImageUrl, size = 40.dp, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Recent", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
            }
        }
    }
}

@Composable
private fun GameMiniChip(
) {
        Image(
            painter = painterResource(R.drawable.ic_game_role_pack),
            contentDescription = "Game logo",
            modifier = Modifier
                .size(34.dp)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentScale = ContentScale.Crop
        )
}

@Composable
private fun FriendsShowcaseDialog(
    currentUsername: String,
    profilePictureUrl: String?,
    friendPreview: List<FriendPreviewUi>,
    onClose: () -> Unit,
    onUserAvatarClick: (String) -> Unit = {},
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFFFBFF)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.clickable { onClose() }
                            )
                            Text("Friends", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                item {

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 220.dp, max = 420.dp)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val tiles = friendPreview.take(4)
                            items(tiles.size) { idx ->
                                val f = tiles[idx]
                                FriendMiniClassicTile(
                                    name = f.name,
                                    profileImageUrl = f.profileImageUrl,
                                    onClick = { onUserAvatarClick(f.uid) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

            }
        }
    }
}

@Composable
private fun ContributorsRankingDialog(
    currentUsername: String,
    profilePictureUrl: String?,
    ranking: List<ContributorRankUi>,
    onClose: () -> Unit,
    onUserAvatarClick: (String) -> Unit = {},
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFFFBFF)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.clickable { onClose() }
                        )
                        Text("Contributors", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SenderAvatarChip(profileImageUrl = profilePictureUrl, size = 58.dp)
                        Text(currentUsername, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("Contributors Ranking", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                val top = ranking.take(3)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    top.getOrNull(1)?.let { ContributorTopCard(it, style = "silver", onUserAvatarClick = onUserAvatarClick) }
                    top.getOrNull(0)?.let { ContributorTopCard(it, style = "gold", onUserAvatarClick = onUserAvatarClick) }
                    top.getOrNull(2)?.let { ContributorTopCard(it, style = "bronze", onUserAvatarClick = onUserAvatarClick) }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val rest = ranking.drop(3)
                    if (rest.isEmpty()) {
                        item {
                            Text(
                                "More contributors ranking will appear here",
                                color = Color(0xFF9CA3AF),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(rest.withIndex().toList(), key = { it.value.uid }) { indexed ->
                            val rank = indexed.index + 4
                            val item = indexed.value
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(rank.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    SenderAvatarChip(
                                        profileImageUrl = item.profileImageUrl,
                                        size = 36.dp,
                                        onClick = if (isContributorProfileClickable(item.uid)) {
                                            { onUserAvatarClick(item.uid) }
                                        } else {
                                            null
                                        }
                                    )
                                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(item.coins.toString().padStart(4, '0'), color = Color(0xFFBE185D), fontWeight = FontWeight.Bold)
                                    Text("Souls", color = Color(0xFF9CA3AF), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE5E7EB))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContributorTopCard(
    item: ContributorRankUi,
    style: String,
    onUserAvatarClick: (String) -> Unit = {},
) {
    val (bg, border) = when (style) {
        "gold" -> Color(0xFFFFF3D1) to Color(0xFFEAB308)
        "silver" -> Color(0xFFEFF3FA) to Color(0xFF94A3B8)
        "bronze" -> Color(0xFFFFE9DF) to Color(0xFFD6A58B)
        else -> Color(0xFFF8F1EC) to Color(0xFFE5D1C5)
    }
    Card(
        modifier = Modifier.width(if (style == "gold") 120.dp else 106.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SenderAvatarChip(
                profileImageUrl = item.profileImageUrl,
                size = if (style == "gold") 62.dp else 54.dp,
                onClick = if (isContributorProfileClickable(item.uid)) {
                    { onUserAvatarClick(item.uid) }
                } else {
                    null
                }
            )
            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(item.coins.toString().padStart(4, '0'), color = Color(0xFFBE185D), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

private data class FriendPreviewUi(
    val uid: String,
    val name: String,
    val profileImageUrl: String?,
)

private data class ContributorRankUi(
    val uid: String,
    val name: String,
    val coins: Long,
    val profileImageUrl: String?,
)

private fun isContributorProfileClickable(uid: String): Boolean =
    uid.isNotBlank() && !uid.startsWith("dummy_contributor_")

private data class SoulRankUi(
    val name: String,
    val iconRes: Int?
)

private val soulRankTiers: List<SoulBadgeTier> = SoulBadgeTiers

private fun resolveSoulRankUi(soul: Long?): SoulRankUi {
    val soulValue = soul ?: 0L
    val tier = soulRankTiers.lastOrNull { soulValue >= it.minSoul }
    return if (tier != null) {
        SoulRankUi(name = tier.name, iconRes = tier.iconRes)
    } else {
        SoulRankUi(name = "Unranked", iconRes = null)
    }
}

private fun formatSoulDisplay(soul: Long?): String {
    val soulValue = soul ?: 0L
    return NumberFormat.getIntegerInstance(Locale.US).format(soulValue)
}

@Composable
private fun PolicyLikePageDialog(
    title: String,
    body: String,
    onClose: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.clickable { onClose() }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}


@Composable
internal fun GiftWallShowcaseDialog(
    gifts: List<ReceivedGiftSummary>,
    userProfilePhotos: Map<String, String>,
    onOpenHistory: () -> Unit,
    onClose: () -> Unit,
    onUserAvatarClick: (String) -> Unit = {},
) {
    var selectedGift by remember(gifts) { mutableStateOf<GiftWallCardUi?>(null) }
    val grouped = remember(gifts) {
        gifts.groupBy { it.giftId ?: "__unknown__" }
            .map { (giftId, rows) ->
                val senderCounts = rows.groupBy { it.fromUserId ?: "__unknown__" }.mapValues { entry ->
                    entry.value.sumOf { it.selectedCount.coerceAtLeast(1) }
                }
                val topSenderUid = senderCounts.maxByOrNull { it.value }?.key
                val topSenderCount = senderCounts[topSenderUid] ?: 0
                val topSenderName = rows.firstOrNull { it.fromUserId == topSenderUid }?.fromDisplayName
                    ?.takeIf { it.isNotBlank() }
                    ?: "Unknown"
                val priceCoins = GiftCatalog.priceCoinsOrNull(giftId)?.toLong() ?: 0L
                GiftWallCardUi(
                    giftId = giftId,
                    label = if (giftId == "__unknown__") "Gift" else GiftCatalog.displayLabel(giftId),
                    count = rows.sumOf { it.selectedCount.coerceAtLeast(1) },
                    coins = rows.sumOf { it.coins },
                    topSenderUid = topSenderUid,
                    topSenderName = topSenderName,
                    topSenderCount = topSenderCount,
                    priceCoins = priceCoins
                )
            }
            .sortedByDescending { it.coins }
    }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF040A24))
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.clickable { onClose() }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Gift", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = onOpenHistory) { Text("History", color = Color.White) }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B1E1D)),
                    border = BorderStroke(0.8.dp, Color(0xFF8B6B3D))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${gifts.sumOf { it.selectedCount.coerceAtLeast(1) }} Gift received", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${gifts.sumOf { it.coins }} coin got", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    text = stringResource(R.string.gift_wall_tab),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(grouped.size) { index ->
                        val gift = grouped[index]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(172.dp)
                                .clickable { selectedGift = gift },
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, Color(0xFF5865F2))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFF273A85), Color(0xFF1A4A4F))
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        SenderAvatarChip(
                                            profileImageUrl = gift.topSenderUid?.let { userProfilePhotos[it] },
                                            size = 24.dp,
                                            onClick = { gift.topSenderUid?.let(onUserAvatarClick) }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    GiftArt(giftId = gift.giftId, modifier = Modifier.size(58.dp))
                                    Text(
                                        text = cleanGiftLabel(gift.label),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "x${gift.count}",
                                        color = Color(0xFFBFC8DC),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    selectedGift?.let { gift ->
        GiftDetailDialog(
            gift = gift,
            userProfilePhotos = userProfilePhotos,
            onDismiss = { selectedGift = null },
            onUserAvatarClick = onUserAvatarClick
        )
    }
}

@Composable
internal fun StatsShowcaseDialog(
    imageUrl: String?,
    history: List<GameHistoryEntry>,
    usernamesByUid: Map<String, String>,
    userProfilePhotos: Map<String, String>,
    onClose: () -> Unit,
    onUserAvatarClick: (String) -> Unit = {},
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF2C534))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.clickable { onClose() },
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Statistics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SenderAvatarChip(profileImageUrl = imageUrl, size = 75.dp)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${history.size}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color(0xFF6D4C00))
                            Text("Total Rounds", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6D4C00))
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD665))) {
                        Row(
                            modifier = Modifier.wrapContentWidth().padding(horizontal = 14.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("You are the soul players, Your game history showing is here!", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF7A5200))
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(  10.dp),
                ) {
                    Text("Recent Statistics", color = Color(0xFF22A3B3), fontWeight = FontWeight.SemiBold)
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.id }) { entry ->
                        StatsHistoryRow(
                            entry = entry,
                            usernamesByUid = usernamesByUid,
                            userProfilePhotos = userProfilePhotos,
                            onUserAvatarClick = onUserAvatarClick
                        )
                        HorizontalDivider(color = Color(0xFFE5E7EB))
                    }
                }
            }
        }
    }
}

private data class GiftWallCardUi(
    val giftId: String,
    val label: String,
    val count: Int,
    val coins: Long,
    val topSenderUid: String?,
    val topSenderName: String,
    val topSenderCount: Int,
    val priceCoins: Long,
)

@Composable
private fun GiftDetailDialog(
    gift: GiftWallCardUi,
    userProfilePhotos: Map<String, String>,
    onDismiss: () -> Unit,
    onUserAvatarClick: (String) -> Unit = {},
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF223749)),
            border = BorderStroke(0.8.dp, Color(0xFF4D7690))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(
                            brush = Brush.verticalGradient(listOf(Color(0xFF36576F), Color(0xFF243B4D))),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GiftArt(giftId = gift.giftId, modifier = Modifier.size(90.dp))
                }
                Text(
                    cleanGiftLabel(gift.label),
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SenderAvatarChip(
                            profileImageUrl = gift.topSenderUid?.let { userProfilePhotos[it] },
                            size = 28.dp,
                            onClick = { gift.topSenderUid?.let(onUserAvatarClick) }
                        )
                        Text(
                            text = "With the most gifts",
                            color = Color(0xFFCBD5E1),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text("x${gift.topSenderCount}", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = gift.topSenderName,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color(0xFF5A7488), RoundedCornerShape(999.dp))
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Price", color = Color(0xFFCBD5E1))
                    Text("🪙 ${gift.priceCoins}", color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Amount (sends)", color = Color(0xFFCBD5E1))
                    Text("${gift.count}", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Max user sent", color = Color(0xFFCBD5E1))
                    Text("x${gift.topSenderCount}", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Received Coins", color = Color(0xFFCBD5E1))
                    Text("${gift.coins}", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(0.8.dp, Color(0xFF6F8CA1)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 2.dp)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun GiftArt(
    giftId: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lottieRaw = remember(giftId) { GiftFxResources.lottieRawRes(context, giftId) }
    val emoji = remember(giftId) { giftEmoji(giftId) }
    if (lottieRaw != 0) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRaw))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = 1,
            restartOnPlay = false
        )
        if (composition != null && progress >= 0.99f) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }
        } else {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = modifier
            )
        }
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(emoji, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private fun cleanGiftLabel(label: String): String {
    return label.replace(Regex("^[^A-Za-z0-9]+\\s*"), "").ifBlank { label }
}

private fun giftEmoji(giftId: String): String = when (giftId) {
    GiftCatalog.ROSE -> "\uD83C\uDF39"
    GiftCatalog.CAKE -> "\uD83C\uDF82"
    GiftCatalog.TEDDY -> "\uD83E\uDDF8"
    GiftCatalog.ROCKET -> "\uD83D\uDE80"
    GiftCatalog.KISS -> "\uD83D\uDC8B"
    GiftCatalog.LOVE -> "\uD83D\uDC96"
    GiftCatalog.RING -> "\uD83D\uDC8D"
    GiftCatalog.EROS -> "\uD83D\uDC98"
    GiftCatalog.CHAMPAGNE -> "\uD83C\uDF7E"
    GiftCatalog.FIRE_CRACKER -> "\uD83C\uDF86"
    GiftCatalog.CROWN -> "\uD83D\uDC51"
    GiftCatalog.SPARKLE -> "\u2728"
    GiftCatalog.DRAGON, "dream palace" -> "\uD83D\uDC09"
    else -> "\uD83C\uDF81"
}

@Composable
internal fun GiftHistoryListDialog(
    gifts: List<ReceivedGiftSummary>,
    userProfilePhotos: Map<String, String>,
    onClose: () -> Unit,
    onUserAvatarClick: (String) -> Unit = {},
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF050B24))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.clickable { onClose() }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Gift History", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(gifts) { g ->
                        val label = g.giftId?.let { GiftCatalog.displayLabel(it) } ?: "Gift"
                        val sender = g.fromDisplayName?.takeIf { it.isNotBlank() } ?: "Unknown"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF182246)),
                            border = BorderStroke(0.5.dp, Color(0xFF334155))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SenderAvatarChip(
                                        profileImageUrl = g.fromUserId?.let { userProfilePhotos[it] },
                                        size = 24.dp,
                                        onClick = { g.fromUserId?.let(onUserAvatarClick) }
                                    )
                                    Column {
                                        Text(sender, color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = giftEmoji(g.giftId ?: ""),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(cleanGiftLabel(label), color = Color(0xFFCBD5E1), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("🪙 ${g.coins}", color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_soul_cute_ghost),
                                            contentDescription = "Soul",
                                            modifier = Modifier.size(14.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text("+${g.soul}", color = Color(0xFFC4B5FD), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    }
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
private fun SenderAvatarChip(
    profileImageUrl: String?,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE5E7EB))
            .let { base -> if (onClick != null) base.clickable { onClick() } else base }
            .then(
                Modifier
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(10.dp))
            ),
        contentAlignment = Alignment.Center
    ) {
        if (profileImageUrl.isNullOrBlank()) {
            Image(
                painter = painterResource(R.drawable.ic_mascot_hero),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            AsyncImage(
                model = profileImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_mascot_hero),
                error = painterResource(R.drawable.ic_mascot_hero),
            )
        }
    }
}

@Composable
private fun StatsHistoryRow(
    entry: GameHistoryEntry,
    usernamesByUid: Map<String, String>,
    userProfilePhotos: Map<String, String>,
    onUserAvatarClick: (String) -> Unit = {},
) {
    var expanded by remember(entry.id) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = if (entry.outcome == MatchOutcome.WIN) "Win" else "Loss",
            color = if (entry.outcome == MatchOutcome.WIN) Color(0xFF16A34A) else Color(0xFFB91C1C),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(48.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Room ${FirebaseUidMapping.shortLabel(entry.roomId)}", style = MaterialTheme.typography.titleMedium)
            Text(formatHistoryDate(entry.endedAt), style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            val players = entry.scoresByUser.keys.take(6)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(players) { uid ->
                    displayNameForUid(uid, usernamesByUid)
                    SenderAvatarChip(
                        profileImageUrl = userProfilePhotos[uid],
                        size = 28.dp,
                        onClick = { onUserAvatarClick(uid) }
                    )
                }
            }
            Text(
                text = if (expanded) "Less details" else "Details",
                color = Color(0xFF22A3B3),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { expanded = !expanded }
            )
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Role: ${formatHistoryRole(entry.role)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                    if (entry.gamePoints != null) {
                        Text("Points: ${entry.gamePoints}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
                    }
                }
            }
        }
    }
}

@Composable
private fun GameStatsRow(
    totalGames: Int,
    wins: Int,
    losses: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileStatChip(title = "Games", value = totalGames.toString(), modifier = Modifier.weight(1f))
        ProfileStatChip(title = "Wins", value = wins.toString(), modifier = Modifier.weight(1f))
        ProfileStatChip(title = "Losses", value = losses.toString(), modifier = Modifier.weight(1f))
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

@Composable
private fun ProfileStatChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 0.4.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private data class GiftBreakdownStat(
    val label: String,
    val count: Int,
    val coins: Long,
)

private fun buildTopGiftBreakdown(history: List<ReceivedGiftSummary>): List<GiftBreakdownStat> {
    return history
        .groupBy { it.giftId ?: "__unknown__" }
        .map { (giftId, items) ->
            GiftBreakdownStat(
                label = if (giftId == "__unknown__") "Gift" else GiftCatalog.displayLabel(giftId),
                count = items.sumOf { it.selectedCount.coerceAtLeast(1) },
                coins = items.sumOf { it.coins }
            )
        }
        .sortedWith(compareByDescending<GiftBreakdownStat> { it.coins }.thenByDescending { it.count })
        .take(3)
}
