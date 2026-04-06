package com.masti.soulplay.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.masti.soulplay.ui.navigation.SoulplayDestinations
import org.koin.androidx.compose.koinViewModel

@Composable
fun AuthGateScreen(
    onGoLogin: () -> Unit,
    onGoCreateProfile: () -> Unit,
    onGoHome: () -> Unit,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val vm: AuthGateViewModel = koinViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    var navigated by remember { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (navigated) return@LaunchedEffect
        when (state) {
            AuthGateUiState.GoLogin -> {
                navigated = true
                onGoLogin()
            }
            AuthGateUiState.GoCreateProfile -> {
                navigated = true
                onGoCreateProfile()
            }
            AuthGateUiState.GoHome -> {
                navigated = true
                onGoHome()
            }
            AuthGateUiState.Loading -> Unit
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

