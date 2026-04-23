package com.souljoy.soulmasti.ui.rewards

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.TimeZone

data class DailyRewardTask(
    val id: String,
    val type: RewardType,
    val rewardAmount: Long,
    val requiredAds: Int,
)

enum class RewardType { COIN, SOUL }

data class DailyRewardTaskProgress(
    val watchedAds: Int = 0,
    val claimed: Boolean = false,
)

class DailyRewardsViewModel(
    application: Application,
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase,
) : AndroidViewModel(application) {

    private val _tasks = MutableStateFlow(defaultTasks())
    val tasks: StateFlow<List<DailyRewardTask>> = _tasks.asStateFlow()

    private val _progress = MutableStateFlow<Map<String, DailyRewardTaskProgress>>(emptyMap())
    val progress: StateFlow<Map<String, DailyRewardTaskProgress>> = _progress.asStateFlow()

    private val _coinBalance = MutableStateFlow<Long?>(null)
    val coinBalance: StateFlow<Long?> = _coinBalance.asStateFlow()

    private val _soulBalance = MutableStateFlow<Long?>(null)
    val soulBalance: StateFlow<Long?> = _soulBalance.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return@launch
            val dayKey = utcDayKey(System.currentTimeMillis()).toString()
            val tasksSnap = runCatching {
                database.reference.child("users").child(uid).child("dailyRewardsV1").child(dayKey).get().await()
            }.getOrNull()
            val map = mutableMapOf<String, DailyRewardTaskProgress>()
            _tasks.value.forEach { task ->
                val node = tasksSnap?.child(task.id)
                map[task.id] = DailyRewardTaskProgress(
                    watchedAds = (node?.child("watchedAds")?.getValue(Long::class.java) ?: 0L).toInt(),
                    claimed = node?.child("claimed")?.getValue(Boolean::class.java) == true,
                )
            }
            _progress.value = map

            val userSnap = runCatching {
                database.reference.child("users").child(uid).get().await()
            }.getOrNull()
            _coinBalance.value = userSnap?.child("totalWinnings")?.getValue(Long::class.java) ?: 0L
            _soulBalance.value = userSnap?.child("soul")?.getValue(Long::class.java) ?: 0L
        }
    }

    fun onAdWatched(taskId: String) {
        val task = _tasks.value.firstOrNull { it.id == taskId } ?: return
        val current = _progress.value[taskId] ?: DailyRewardTaskProgress()
        if (current.claimed) return
        val next = current.copy(watchedAds = (current.watchedAds + 1).coerceAtMost(task.requiredAds))
        _progress.value = _progress.value + (taskId to next)
        persistProgress(taskId, next)
    }

    fun claim(taskId: String) {
        val task = _tasks.value.firstOrNull { it.id == taskId } ?: return
        val current = _progress.value[taskId] ?: DailyRewardTaskProgress()
        if (current.claimed) {
            _message.value = "Already claimed"
            return
        }
        if (current.watchedAds < task.requiredAds) {
            _message.value = "Watch all required ads first"
            return
        }
        viewModelScope.launch {
            val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return@launch
            val dayKey = utcDayKey(System.currentTimeMillis()).toString()
            val userRef = database.reference.child("users").child(uid)
            val updates = mutableMapOf<String, Any>()
            if (task.type == RewardType.COIN) {
                val currentCoins = _coinBalance.value ?: 0L
                updates["totalWinnings"] = currentCoins + task.rewardAmount
            } else {
                val currentSoul = _soulBalance.value ?: 0L
                updates["soul"] = currentSoul + task.rewardAmount
            }
            updates["dailyRewardsV1/$dayKey/$taskId/watchedAds"] = current.watchedAds
            updates["dailyRewardsV1/$dayKey/$taskId/claimed"] = true
            updates["dailyRewardsV1/$dayKey/$taskId/updatedAt"] = System.currentTimeMillis()
            runCatching { userRef.updateChildren(updates).await() }
                .onSuccess {
                    _progress.value = _progress.value + (taskId to current.copy(claimed = true))
                    if (task.type == RewardType.COIN) {
                        _coinBalance.value = (_coinBalance.value ?: 0L) + task.rewardAmount
                        _message.value = "+${task.rewardAmount} coins claimed"
                    } else {
                        _soulBalance.value = (_soulBalance.value ?: 0L) + task.rewardAmount
                        _message.value = "+${task.rewardAmount} souls claimed"
                    }
                }
                .onFailure {
                    _message.value = it.message ?: "Claim failed"
                }
        }
    }

    fun dismissMessage() {
        _message.value = null
    }

    fun showMessage(text: String) {
        _message.value = text
    }

    private fun persistProgress(taskId: String, value: DailyRewardTaskProgress) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid?.takeIf { it.isNotBlank() } ?: return@launch
            val dayKey = utcDayKey(System.currentTimeMillis()).toString()
            val ref = database.reference
                .child("users")
                .child(uid)
                .child("dailyRewardsV1")
                .child(dayKey)
                .child(taskId)
            val payload = mapOf(
                "watchedAds" to value.watchedAds,
                "claimed" to value.claimed,
                "updatedAt" to System.currentTimeMillis(),
            )
            runCatching { ref.updateChildren(payload).await() }
        }
    }

    private fun utcDayKey(timestampMs: Long): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = timestampMs
        val year = cal.get(Calendar.YEAR).toLong()
        val day = cal.get(Calendar.DAY_OF_YEAR).toLong()
        return year * 1000L + day
    }

    private fun defaultTasks(): List<DailyRewardTask> = listOf(
        DailyRewardTask("coin_100", RewardType.COIN, 100L, 1),
        DailyRewardTask("coin_300", RewardType.COIN, 300L, 3),
        DailyRewardTask("coin_600", RewardType.COIN, 600L, 5),
        DailyRewardTask("soul_20", RewardType.SOUL, 20L, 1),
        DailyRewardTask("soul_60", RewardType.SOUL, 60L, 3),
        DailyRewardTask("soul_120", RewardType.SOUL, 120L, 5),
    )
}

