package com.souljoy.soulmasti.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.souljoy.soulmasti.domain.RoomJoinEconomy
import com.souljoy.soulmasti.domain.repository.GameSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class HomeMatchUiState {
    data object Idle : HomeMatchUiState()
    data object Searching : HomeMatchUiState()
    data class Matched(val roomId: String) : HomeMatchUiState()
    data class Error(val message: String) : HomeMatchUiState()
}

class HomeViewModel(
    application: Application,
    private val game: GameSessionRepository,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<HomeMatchUiState>(HomeMatchUiState.Idle)
    val uiState: StateFlow<HomeMatchUiState> = _uiState.asStateFlow()
    val coinBalance: StateFlow<Long?> =
        game.observeUserTotalWinnings().stateIn(
            scope = viewModelScope,
            // Eagerly connect so first Home open isn’t stuck on null/0 while RTDB warms up.
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private var matchJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching {
                game.ensureSignedInAndPresence()
                // Prime RTDB read so first “Start match” isn’t racing a cold cache.
                game.getCoinBalance()
            }.onFailure { _uiState.value = HomeMatchUiState.Error(it.message ?: "Sign-in failed") }
        }
    }

    /**
     * First app launch: [getCoinBalance] can briefly return 0 before RTDB sync/auth settles.
     * If balance is 0, retry a few times before treating it as real.
     */
    private suspend fun getCoinBalanceForMatch(fee: Long): Long {
        var balance = game.getCoinBalance()
        if (balance >= fee) return balance
        if (balance > 0L) return balance
        repeat(4) { attempt ->
            delay(200L + attempt * 150L)
            balance = game.getCoinBalance()
            if (balance >= fee) return balance
            if (balance > 0L) return balance
        }
        return balance
    }

    /** Rare cold-start race: transaction sees 0 once; one retry after a short delay usually fixes it. */
    private suspend fun deductJoinRoomFeeWithColdStartRetry() {
        try {
            game.deductJoinRoomFee()
        } catch (e: Exception) {
            val msg = e.message.orEmpty()
            val looksLikeBalance =
                msg.contains("not enough", ignoreCase = true) ||
                    msg.contains("costs", ignoreCase = true) ||
                    msg.contains("joining a room", ignoreCase = true)
            if (looksLikeBalance) {
                delay(450)
                game.deductJoinRoomFee()
            } else {
                throw e
            }
        }
    }

    fun startFindMatch() {
        if (_uiState.value is HomeMatchUiState.Searching) return
        matchJob?.cancel()
        matchJob = viewModelScope.launch {
            _uiState.value = HomeMatchUiState.Searching
            try {
                game.ensureSignedInAndPresence()
                val fee = RoomJoinEconomy.JOIN_ROOM_FEE_COINS
                val balance = getCoinBalanceForMatch(fee)
                if (balance < fee) {
                    _uiState.value = HomeMatchUiState.Error(
                        "You need $fee coins to join a room. Your balance: $balance."
                    )
                    return@launch
                }
                game.addToWaitingQueue()
                val roomId = game.awaitMatchedRoom()
                deductJoinRoomFeeWithColdStartRetry()
                _uiState.value = HomeMatchUiState.Matched(roomId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = HomeMatchUiState.Error(e.message ?: "Matchmaking failed")
            } finally {
                game.removeFromWaitingQueue()
            }
        }
    }

    fun cancelSearch() {
        matchJob?.cancel()
        matchJob = null
        viewModelScope.launch {
            runCatching { game.removeFromWaitingQueue() }
        }
        if (_uiState.value is HomeMatchUiState.Searching) {
            _uiState.value = HomeMatchUiState.Idle
        }
    }

    fun consumeMatchedNavigation() {
        if (_uiState.value is HomeMatchUiState.Matched) {
            _uiState.value = HomeMatchUiState.Idle
        }
    }

    fun dismissError() {
        if (_uiState.value is HomeMatchUiState.Error) {
            _uiState.value = HomeMatchUiState.Idle
        }
    }
}
