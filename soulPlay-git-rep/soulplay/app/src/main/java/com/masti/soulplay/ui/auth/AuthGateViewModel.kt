package com.masti.soulplay.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthGateUiState {
    data object Loading : AuthGateUiState()
    data object GoLogin : AuthGateUiState()
    data object GoCreateProfile : AuthGateUiState()
    data object GoHome : AuthGateUiState()
}

class AuthGateViewModel(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthGateUiState>(AuthGateUiState.Loading)
    val uiState: StateFlow<AuthGateUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                _uiState.value = AuthGateUiState.GoLogin
                return@launch
            }

            val created = runCatching { isProfileCreated(uid) }.getOrDefault(false)
            _uiState.value = if (created) {
                AuthGateUiState.GoHome
            } else {
                AuthGateUiState.GoCreateProfile
            }
        }
    }

    private suspend fun isProfileCreated(uid: String): Boolean {
        val snap = database.reference
            .child("users")
            .child(uid)
            .child("profileCreated")
            .get()
            .await()
        return snap.getValue(Boolean::class.java) == true
    }
}

