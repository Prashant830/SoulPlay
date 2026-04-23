package com.souljoy.soulmasti.ui

import android.widget.Toast
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.souljoy.soulmasti.ui.chat.ChatScreen
import com.souljoy.soulmasti.ui.chat.PendingRoomInvite
import com.souljoy.soulmasti.ui.chat.ChatThreadScreen
import com.souljoy.soulmasti.ui.chat.ChatThreadViewModel
import com.souljoy.soulmasti.ui.chat.ChatViewModel
import com.souljoy.soulmasti.ui.home.HomeMatchUiState
import com.souljoy.soulmasti.ui.home.HomeScreen
import com.souljoy.soulmasti.ui.home.HomeViewModel
import com.souljoy.soulmasti.ui.league.LeagueScreen
import com.souljoy.soulmasti.ui.league.LeagueViewModel
import com.souljoy.soulmasti.ui.league.RewardInboxScreen
import com.souljoy.soulmasti.ui.league.RewardInboxViewModel
import com.souljoy.soulmasti.ui.auth.AuthGateScreen
import com.souljoy.soulmasti.ui.auth.CreateProfileScreen
import com.souljoy.soulmasti.ui.auth.LoginScreen
import com.souljoy.soulmasti.ui.navigation.SoulplayDestinations
import com.souljoy.soulmasti.ui.rewards.DailyRewardsScreen
import com.souljoy.soulmasti.ui.settings.SettingsScreen
import com.souljoy.soulmasti.ui.settings.UserProfileScreen
import com.souljoy.soulmasti.ui.shop.GoldShopScreen
import com.souljoy.soulmasti.ui.soulmasti.shopping.SoulMastiShoppingScreen
import com.souljoy.soulmasti.ui.voice.game.VoiceRoomNeedMatchScreen
import com.souljoy.soulmasti.ui.voice.game.VoiceRoomScreen
import com.souljoy.soulmasti.ui.voice.game.VoiceRoomViewModel
import com.souljoy.soulmasti.ui.voice.social.SocialVoiceRoomScreen
import com.souljoy.soulmasti.ui.voice.social.SocialVoiceRoomViewModel
import com.souljoy.soulmasti.ui.voice.social.SocialVoiceRoomsScreen
import com.souljoy.soulmasti.domain.repository.SocialVoiceRoomRepository
import com.souljoy.soulmasti.ui.voice.social.SocialVoiceRoomsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import com.souljoy.soulmasti.data.billing.CoinPurchaseCoordinator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.souljoy.soulmasti.domain.repository.SocialRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(SoulplayDestinations.Home, "Home", Icons.Filled.Home),
    BottomTab(SoulplayDestinations.League, "League", Icons.Filled.EmojiEvents),
    BottomTab(SoulplayDestinations.SocialVoiceHub, "Voice Room", Icons.Filled.Mic),
    BottomTab(SoulplayDestinations.Chat, "Chat", Icons.Filled.ChatBubbleOutline),
    BottomTab(SoulplayDestinations.Settings, "Profile", Icons.Filled.Person)
)

private data class LeaveRoomRequest(
    val currentRoomId: String,
    val targetRoomId: String? = null,
    val exitCurrentScreen: Boolean = false,
    val applyJoinFee: Boolean = false,
)

private data class JoinFeeRequest(
    val targetRoomId: String,
    val currentRoomId: String,
)

@Composable
fun SoulplayApp(
    hasVoicePermission: () -> Boolean,
    requestVoicePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val coinPurchaseSync: CoinPurchaseCoordinator = koinInject()
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            runCatching { coinPurchaseSync.reconcilePendingPurchases() }
        }
    }
    val socialRepository: SocialRepository = koinInject<SocialRepository>()
    val socialVoiceRoomRepository: SocialVoiceRoomRepository = koinInject()
    val firebaseAuth: FirebaseAuth = koinInject<FirebaseAuth>()
    val firebaseDatabase: FirebaseDatabase = koinInject<FirebaseDatabase>()
    val scope = rememberCoroutineScope()
    var leaveRoomRequest by remember { mutableStateOf<LeaveRoomRequest?>(null) }
    var joinFeeRequest by remember { mutableStateOf<JoinFeeRequest?>(null) }
    var pendingRoomInviteForChat by remember { mutableStateOf<PendingRoomInvite?>(null) }
    val incomingFriendRequests by socialRepository.incomingFriendRequests.collectAsStateWithLifecycle()
    val unreadByPeer by socialRepository.unreadMessageCounts.collectAsStateWithLifecycle()
    val friendRequestBadgeCount = incomingFriendRequests.size
    val unreadMessageBadgeCount = unreadByPeer.values.sum()
    val chatBadgeCount = friendRequestBadgeCount + unreadMessageBadgeCount
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val route = currentDestination?.route.orEmpty()
    val hideBottomBar =
        route.contains("voice_room") ||
            route.startsWith("chat_thread") ||
            route.contains("login") ||
            route.contains("create_profile") ||
            route.contains("auth_gate") ||
            route.contains("user_profile") ||
            route.contains(SoulplayDestinations.League) ||
            route.contains(SoulplayDestinations.GoldShop) ||
            route.contains(SoulplayDestinations.SoulMastiShopping) ||
            route.contains(SoulplayDestinations.DailyRewards)
    val showBottomBar = !hideBottomBar

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (tab.route == SoulplayDestinations.Chat && chatBadgeCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(
                                                    chatBadgeCount.coerceAtMost(99).toString()
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        // Avoid applying the scaffold’s top safe-area padding here: screens with a Material
        // [TopAppBar] already consume status-bar insets; padding NavHost at the top as well doubled the gap on cutout devices.
        val layoutDirection = LocalLayoutDirection.current
        NavHost(
            navController = navController,
            startDestination = SoulplayDestinations.AuthGate,
            modifier = Modifier.padding(
                PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = 0.dp,
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = innerPadding.calculateBottomPadding()
                )
            )
        ) {
            composable(SoulplayDestinations.AuthGate) {
                AuthGateScreen(
                    onGoLogin = {
                        navController.navigate(SoulplayDestinations.Login) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onGoCreateProfile = {
                        navController.navigate(SoulplayDestinations.CreateProfile) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onGoHome = {
                        navController.navigate(SoulplayDestinations.Home) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }

            composable(SoulplayDestinations.Login) {
                LoginScreen(
                    onRegistered = {
                        // Route decision is handled by AuthGate re-check.
                        navController.navigate(SoulplayDestinations.AuthGate) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(SoulplayDestinations.AuthGate) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            composable(SoulplayDestinations.CreateProfile) {
                CreateProfileScreen(
                    onCreated = {
                        navController.navigate(SoulplayDestinations.Home) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(SoulplayDestinations.AuthGate) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            composable(SoulplayDestinations.Home) {
                val homeVm: HomeViewModel = koinViewModel()
                val matchState by homeVm.uiState.collectAsStateWithLifecycle()
                val coinBalance by homeVm.coinBalance.collectAsStateWithLifecycle()

                LaunchedEffect(matchState) {
                    when (val s = matchState) {
                        is HomeMatchUiState.Matched -> {
                            navController.navigate(SoulplayDestinations.voiceRoom(s.roomId)) {
                                launchSingleTop = true
                            }
                            homeVm.consumeMatchedNavigation()
                        }
                        else -> Unit
                    }
                }

                HomeScreen(
                    matchState = matchState,
                    coinBalance = coinBalance,
                    notificationCount = chatBadgeCount,
                    onFindMatch = { homeVm.startFindMatch() },
                    onCancelSearch = { homeVm.cancelSearch() },
                    onOpenVoiceTab = {
                        navController.navigate(SoulplayDestinations.VoiceRoom) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenNotifications = {
                        navController.navigate(SoulplayDestinations.Chat) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenGoldShop = {
                        navController.navigate(SoulplayDestinations.GoldShop)
                    },
                    onOpenDailyRewards = {
                        navController.navigate(SoulplayDestinations.DailyRewards) { launchSingleTop = true }
                    },
                    onDismissError = { homeVm.dismissError() }
                )
            }
            composable(SoulplayDestinations.GoldShop) {
                GoldShopScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(SoulplayDestinations.SoulMastiShopping) {
                SoulMastiShoppingScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAdsTab = {
                        navController.navigate(SoulplayDestinations.DailyRewards) { launchSingleTop = true }
                    },
                )
            }
            composable(SoulplayDestinations.DailyRewards) {
                DailyRewardsScreen(onBack = { navController.popBackStack() })
            }
            composable(SoulplayDestinations.League) {
                val leagueVm: LeagueViewModel = koinViewModel()
                LeagueScreen(
                    viewModel = leagueVm,
                    onOpenUserProfile = { uid ->
                        if (uid.isNotBlank()) {
                            navController.navigate(SoulplayDestinations.userProfile(uid)) { launchSingleTop = true }
                        }
                    },
                    onOpenRoom = { roomId ->
                        if (roomId.isNotBlank()) {
                            scope.launch {
                                val uid = firebaseAuth.currentUser?.uid.orEmpty()
                                if (uid.isBlank()) {
                                    navController.navigate(SoulplayDestinations.socialVoiceRoom(roomId)) { launchSingleTop = true }
                                    return@launch
                                }
                                val currentRoomId = runCatching {
                                    firebaseDatabase.reference
                                        .child("users")
                                        .child(uid)
                                        .child("currentSocialRoomId")
                                        .get()
                                        .await()
                                        .getValue(String::class.java)
                                        .orEmpty()
                                }.getOrDefault("")
                                val isOwnRoom = roomId == uid
                                if (isOwnRoom || currentRoomId == roomId) {
                                    navController.navigate(SoulplayDestinations.socialVoiceRoom(roomId)) { launchSingleTop = true }
                                } else {
                                    joinFeeRequest = JoinFeeRequest(
                                        targetRoomId = roomId,
                                        currentRoomId = currentRoomId,
                                    )
                                }
                            }
                        }
                    },
                    onOpenRewardInbox = {
                        navController.navigate(SoulplayDestinations.RewardInbox) { launchSingleTop = true }
                    },
                    onBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(SoulplayDestinations.Home) { launchSingleTop = true }
                        }
                    },
                )
            }
            composable(SoulplayDestinations.RewardInbox) {
                val rewardVm: RewardInboxViewModel = koinViewModel()
                RewardInboxScreen(
                    viewModel = rewardVm,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(SoulplayDestinations.SocialVoiceHub) {
                val socialVm: SocialVoiceRoomsViewModel = koinViewModel()
                SocialVoiceRoomsScreen(
                    viewModel = socialVm,
                    onOpenRoom = { roomId ->
                        if (roomId.isNotBlank()) {
                            navController.navigate(SoulplayDestinations.socialVoiceRoom(roomId)) {
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }
            composable(SoulplayDestinations.VoiceRoom) {
                VoiceRoomNeedMatchScreen(
                    onGoHome = {
                        navController.navigate(SoulplayDestinations.Home) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(
                route = SoulplayDestinations.VoiceRoomWithId,
                arguments = listOf(
                    navArgument("roomId") { type = NavType.StringType }
                )
            ) { entry ->
                val roomId = entry.arguments?.getString("roomId").orEmpty()
                if (roomId.isBlank()) {
                    VoiceRoomNeedMatchScreen(
                        onGoHome = {
                            navController.navigate(SoulplayDestinations.Home) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                } else {
                    val voiceVm: VoiceRoomViewModel =
                        koinViewModel(parameters = { parametersOf(roomId) })
                    VoiceRoomScreen(
                        viewModel = voiceVm,
                        hasVoicePermission = hasVoicePermission,
                        requestVoicePermission = requestVoicePermission,
                        onRoomClosed = {
                            // Pop the active game off the stack so Back does not return to an ended room.
                            if (!navController.popBackStack()) {
                                navController.navigate(SoulplayDestinations.Home) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }
            }
            composable(
                route = SoulplayDestinations.SocialVoiceRoomWithId,
                arguments = listOf(navArgument("roomId") { type = NavType.StringType }),
            ) { entry ->
                val roomId = entry.arguments?.getString("roomId").orEmpty()
                LaunchedEffect(roomId) {
                    if (roomId.isBlank()) navController.popBackStack()
                }
                if (roomId.isNotBlank()) {
                    val vm: SocialVoiceRoomViewModel = koinViewModel(parameters = { parametersOf(roomId) })
                    SocialVoiceRoomScreen(
                        viewModel = vm,
                        hasVoicePermission = hasVoicePermission,
                        requestVoicePermission = requestVoicePermission,
                        onBack = { navController.popBackStack() },
                        onInviteFriends = { roomId, roomName, roomPhotoUrl ->
                            pendingRoomInviteForChat = PendingRoomInvite(
                                roomId = roomId,
                                roomName = roomName,
                                roomPhotoUrl = roomPhotoUrl,
                            )
                            navController.navigate(SoulplayDestinations.Chat) {
                                launchSingleTop = true
                            }
                        },
                        onRequestExitRoom = { currentRoomId ->
                            leaveRoomRequest = LeaveRoomRequest(
                                currentRoomId = currentRoomId,
                                exitCurrentScreen = true,
                            )
                        },
                        onOpenUserProfile = { uid ->
                            if (uid.isNotBlank()) {
                                navController.navigate(SoulplayDestinations.userProfile(uid)) { launchSingleTop = true }
                            }
                        },
                        onOpenSoulLevel = { uid ->
                            if (uid.isNotBlank()) {
                                navController.navigate(SoulplayDestinations.userProfile(uid)) { launchSingleTop = true }
                            }
                        },
                    )
                }
            }
            composable(SoulplayDestinations.Chat) {
                val chatVm: ChatViewModel = koinViewModel()
                ChatScreen(
                    viewModel = chatVm,
                    onOpenThread = { peerUid ->
                        navController.navigate(SoulplayDestinations.chatThread(peerUid)) {
                            launchSingleTop = false
                        }
                    },
                    onOpenUserProfile = { uid ->
                        if (uid.isNotBlank()) {
                            navController.navigate(SoulplayDestinations.userProfile(uid)) { launchSingleTop = true }
                        }
                    },
                )
            }
            composable(
                route = SoulplayDestinations.ChatThread,
                arguments = listOf(
                    navArgument("peerUid") { type = NavType.StringType }
                )
            ) { entry ->
                val peerUid = entry.arguments?.getString("peerUid").orEmpty()
                LaunchedEffect(peerUid) {
                    if (peerUid.isBlank()) navController.popBackStack()
                }
                if (peerUid.isNotBlank()) {
                    val threadVm: ChatThreadViewModel =
                        koinViewModel(parameters = { parametersOf(peerUid) })
                    ChatThreadScreen(
                        viewModel = threadVm,
                        onBack = { navController.popBackStack() },
                        pendingRoomInvite = pendingRoomInviteForChat,
                        onPendingRoomInviteConsumed = { pendingRoomInviteForChat = null },
                        onJoinVoiceRoom = { roomId ->
                            if (roomId.isBlank()) return@ChatThreadScreen
                            scope.launch {
                                val uid = firebaseAuth.currentUser?.uid.orEmpty()
                                if (uid.isBlank()) {
                                    navController.navigate(SoulplayDestinations.socialVoiceRoom(roomId)) { launchSingleTop = true }
                                    return@launch
                                }
                                val currentRoomId = runCatching {
                                    firebaseDatabase.reference
                                        .child("users")
                                        .child(uid)
                                        .child("currentSocialRoomId")
                                        .get()
                                        .await()
                                        .getValue(String::class.java)
                                        .orEmpty()
                                }.getOrDefault("")
                                val isOwnRoom = roomId == uid
                                if (isOwnRoom || currentRoomId == roomId) {
                                    navController.navigate(SoulplayDestinations.socialVoiceRoom(roomId)) { launchSingleTop = true }
                                } else {
                                    joinFeeRequest = JoinFeeRequest(
                                        targetRoomId = roomId,
                                        currentRoomId = currentRoomId,
                                    )
                                }
                            }
                        },
                        onOpenPeerProfile = { uid ->
                            if (uid.isNotBlank()) {
                                navController.navigate(SoulplayDestinations.userProfile(uid)) { launchSingleTop = true }
                            }
                        }
                    )
                }
            }
            composable(SoulplayDestinations.Settings) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(SoulplayDestinations.Login) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    },
                    onOpenUserProfile = { uid ->
                        if (uid.isNotBlank()) {
                            navController.navigate(SoulplayDestinations.userProfile(uid)) { launchSingleTop = true }
                        }
                    },
                    onOpenShop = {
                        navController.navigate(SoulplayDestinations.SoulMastiShopping) { launchSingleTop = true }
                    },
                )
            }
            composable(
                route = SoulplayDestinations.UserProfile,
                arguments = listOf(navArgument("uid") { type = NavType.StringType })
            ) { entry ->
                val uid = entry.arguments?.getString("uid").orEmpty()
                LaunchedEffect(uid) {
                    if (uid.isBlank()) navController.popBackStack()
                }
                if (uid.isNotBlank()) {
                    UserProfileScreen(
                        uid = uid,
                        onBack = { navController.popBackStack() },
                        onOpenChatThread = { peerUid ->
                            if (peerUid.isNotBlank()) {
                                navController.navigate(SoulplayDestinations.chatThread(peerUid)) {
                                    launchSingleTop = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    joinFeeRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { joinFeeRequest = null },
            title = { Text("Join room?") },
            text = { Text("Joining fee: 50 coins. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        joinFeeRequest = null
                        if (request.currentRoomId.isNotBlank()) {
                            leaveRoomRequest = LeaveRoomRequest(
                                currentRoomId = request.currentRoomId,
                                targetRoomId = request.targetRoomId,
                                applyJoinFee = true,
                            )
                        } else {
                            scope.launch {
                                val result = socialVoiceRoomRepository.enterFriendRoom(
                                    roomId = request.targetRoomId,
                                    friendEntryCostCoins = 50L,
                                )
                                result.onSuccess {
                                    navController.navigate(SoulplayDestinations.socialVoiceRoom(request.targetRoomId)) {
                                        launchSingleTop = true
                                    }
                                }
                                result.onFailure {
                                    Toast.makeText(context, it.message ?: "Could not enter room", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                ) { Text("Join") }
            },
            dismissButton = {
                TextButton(onClick = { joinFeeRequest = null }) { Text("Cancel") }
            },
        )
    }

    leaveRoomRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { leaveRoomRequest = null },
            title = { Text("Leave room?") },
            text = { Text("You need to leave this room first. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val currentRoomId = request.currentRoomId
                        val targetRoomId = request.targetRoomId
                        val exitCurrentScreen = request.exitCurrentScreen
                        val applyJoinFee = request.applyJoinFee
                        leaveRoomRequest = null
                        scope.launch {
                            val uid = firebaseAuth.currentUser?.uid.orEmpty()
                            if (uid.isNotBlank() && currentRoomId.isNotBlank()) {
                                val ownerUid = runCatching {
                                    firebaseDatabase.reference
                                        .child("voiceRooms")
                                        .child(currentRoomId)
                                        .child("ownerUid")
                                        .get()
                                        .await()
                                        .getValue(String::class.java)
                                        .orEmpty()
                                }.getOrDefault("")
                                val seatsSnap = runCatching {
                                    firebaseDatabase.reference
                                        .child("voiceRooms")
                                        .child(currentRoomId)
                                        .child("seats")
                                        .get()
                                        .await()
                                }.getOrNull()
                                val mySeatNo = seatsSnap?.children
                                    ?.firstOrNull {
                                        val seatNo = it.key?.toIntOrNull() ?: return@firstOrNull false
                                        val occupantUid = it.child("uid").getValue(String::class.java).orEmpty()
                                        seatNo != 1 && occupantUid == uid
                                    }
                                    ?.key
                                    ?.toIntOrNull()
                                if (mySeatNo != null) {
                                    runCatching { socialVoiceRoomRepository.leaveSeat(currentRoomId, mySeatNo) }
                                }
                                if (ownerUid == uid) {
                                    runCatching { socialVoiceRoomRepository.collapseRoomIfOwnerLeft(currentRoomId) }
                                }
                                runCatching { socialVoiceRoomRepository.dismissMyPendingSeatInvites(currentRoomId) }
                                runCatching { socialVoiceRoomRepository.markMyChatCleared(currentRoomId) }
                                runCatching { socialVoiceRoomRepository.setRoomPresence(currentRoomId, false) }
                            }
                            when {
                                !targetRoomId.isNullOrBlank() -> {
                                    val enterResult = if (applyJoinFee) {
                                        socialVoiceRoomRepository.enterFriendRoom(
                                            roomId = targetRoomId,
                                            friendEntryCostCoins = 50L,
                                        )
                                    } else Result.success(Unit)
                                    enterResult.onSuccess {
                                        navController.navigate(SoulplayDestinations.socialVoiceRoom(targetRoomId)) {
                                            launchSingleTop = true
                                        }
                                    }
                                    enterResult.onFailure {
                                        Toast.makeText(context, it.message ?: "Could not enter room", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                exitCurrentScreen -> {
                                    navController.popBackStack()
                                }
                            }
                        }
                    },
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { leaveRoomRequest = null }) { Text("No") }
            },
        )
    }
}
