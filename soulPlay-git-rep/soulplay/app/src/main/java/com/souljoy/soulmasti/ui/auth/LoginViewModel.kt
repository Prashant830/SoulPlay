package com.souljoy.soulmasti.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.common.api.CommonStatusCodes
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

    fun signInWithGoogleIdToken(
        idToken: String?,
        onDone: () -> Unit,
        googlePlayServicesStatusCode: Int? = null,
        accountResolvedButNoIdToken: Boolean = false,
    ) {
        if (_uiState.value == LoginUiState.Loading) return
        if (idToken.isNullOrBlank()) {
            val statusHint = googlePlayServicesStatusCode?.let { code ->
                " (Play services status $code: ${CommonStatusCodes.getStatusCodeString(code)})"
            }.orEmpty()
            val detail = when {
                accountResolvedButNoIdToken ->
                    "Account returned but no ID token — confirm `default_web_client_id` is the OAuth 2.0 Web client id " +
                        "from Firebase / Google Cloud (not the Android client id), then add SHA-1 for this build’s signing key in Firebase."
                else ->
                    "Use the Web client id for `default_web_client_id`. " +
                        "In Firebase → Project settings → Your Android app, add SHA-1/SHA-256 for the keystore you use (debug and/or release), " +
                        "download google-services.json again, reinstall. " +
                        "Status 10 usually means SHA-1 / OAuth mismatch."
            }
            _uiState.value = LoginUiState.Error(
                "Google sign-in failed: missing id token.$statusHint $detail"
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

