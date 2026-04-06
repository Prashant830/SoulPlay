package com.masti.soulplay.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.masti.soulplay.ui.home.HomeMatchUiState
import com.masti.soulplay.ui.home.HomeScreen
import com.masti.soulplay.ui.home.HomeViewModel
import com.masti.soulplay.ui.auth.AuthGateScreen
import com.masti.soulplay.ui.auth.CreateProfileScreen
import com.masti.soulplay.ui.auth.LoginScreen
import com.masti.soulplay.ui.navigation.SoulplayDestinations
import com.masti.soulplay.ui.settings.SettingsScreen
import com.masti.soulplay.ui.voiceroom.VoiceRoomNeedMatchScreen
import com.masti.soulplay.ui.voiceroom.VoiceRoomScreen
import com.masti.soulplay.ui.voiceroom.VoiceRoomViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab(SoulplayDestinations.Home, "Home", Icons.Filled.Home),
    BottomTab(SoulplayDestinations.VoiceRoom, "Voice Room", Icons.Filled.Mic),
    BottomTab(SoulplayDestinations.Settings, "Profile", Icons.Filled.Person)
)

@Composable
fun SoulplayApp(
    hasVoicePermission: () -> Boolean,
    requestVoicePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val route = currentDestination?.route.orEmpty()
    val hideBottomBar =
        route.contains("voice_room") || route.contains("login") || route.contains("create_profile") || route.contains("auth_gate")
    val showBottomBar = !hideBottomBar

    // Completely consume system back inside Compose so it doesn't pop the nav stack.
    BackHandler(enabled = true) {
        // No-op: back does nothing anywhere in the app.
    }

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
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SoulplayDestinations.AuthGate,
            modifier = Modifier.padding(innerPadding)
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
                    }
                )
            }

            composable(SoulplayDestinations.CreateProfile) {
                CreateProfileScreen(
                    onCreated = {
                        navController.navigate(SoulplayDestinations.Home) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
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
                    onDismissError = { homeVm.dismissError() }
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
            }
            composable(SoulplayDestinations.Settings) {
                SettingsScreen(
                    onLogout = {
                        navController.navigate(SoulplayDestinations.Login) {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
