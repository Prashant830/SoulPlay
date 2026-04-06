package com.masti.soulplay.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LoginUiState {
    data object Idle : LoginUiState()
    data object Loading : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}

class LoginViewModel(
    private val auth: FirebaseAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInWithGoogleIdToken(idToken: String?, onDone: () -> Unit) {
        if (_uiState.value == LoginUiState.Loading) return
        if (idToken.isNullOrBlank()) {
            _uiState.value = LoginUiState.Error(
                "Google sign-in failed: missing id token. " +
                    "Check `default_web_client_id` (Web OAuth client id) and that google-services.json has non-empty `oauth_client`. " +
                    "If oauth_client is correct, also verify SHA-1 certificate fingerprints in Firebase and reinstall the app."
            )
            return
        }
        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, /* accessToken = */ null)
                auth.signInWithCredential(credential).await()
            }.onSuccess {
                _uiState.value = LoginUiState.Idle
                onDone()
            }.onFailure { e ->
                _uiState.value = LoginUiState.Error(e.message ?: "Login failed")
            }
        }
    }
}

