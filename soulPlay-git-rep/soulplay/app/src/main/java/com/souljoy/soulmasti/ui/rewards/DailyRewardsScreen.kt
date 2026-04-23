package com.souljoy.soulmasti.ui.rewards

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.souljoy.soulmasti.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun DailyRewardsScreen(
    onBack: () -> Unit,
    vm: DailyRewardsViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val coinBalance by vm.coinBalance.collectAsStateWithLifecycle()
    val soulBalance by vm.soulBalance.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var loadingTaskId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        vm.dismissMessage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0B1220), Color(0xFF131E35), Color(0xFF0F172A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = "Daily Free Rewards",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }

            Text(
                text = "Wallet: ${coinBalance ?: 0} coins  |  ${soulBalance ?: 0} souls",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD6E4FF),
            )

            RewardSection(
                title = "Get Coins",
                tasks = tasks.filter { it.type == RewardType.COIN },
                progress = progress,
                loadingTaskId = loadingTaskId,
                onWatchAd = { taskId ->
                    if (loadingTaskId != null) return@RewardSection
                    val activity = context.findActivity()
                    if (activity == null) {
                        vm.showMessage("Unable to open ad right now")
                        return@RewardSection
                    }
                    loadingTaskId = taskId
                    AdMobRewardedAds.loadAndShow(
                        activity = activity,
                        onEarnedReward = { vm.onAdWatched(taskId) },
                        onDone = { loadingTaskId = null },
                        onFailed = { error -> vm.showMessage(error) },
                    )
                },
                onClaim = vm::claim,
            )

            RewardSection(
                title = "Get Souls",
                tasks = tasks.filter { it.type == RewardType.SOUL },
                progress = progress,
                loadingTaskId = loadingTaskId,
                onWatchAd = { taskId ->
                    if (loadingTaskId != null) return@RewardSection
                    val activity = context.findActivity()
                    if (activity == null) {
                        vm.showMessage("Unable to open ad right now")
                        return@RewardSection
                    }
                    loadingTaskId = taskId
                    AdMobRewardedAds.loadAndShow(
                        activity = activity,
                        onEarnedReward = { vm.onAdWatched(taskId) },
                        onDone = { loadingTaskId = null },
                        onFailed = { error -> vm.showMessage(error) },
                    )
                },
                onClaim = vm::claim,
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun RewardSection(
    title: String,
    tasks: List<DailyRewardTask>,
    progress: Map<String, DailyRewardTaskProgress>,
    loadingTaskId: String?,
    onWatchAd: (String) -> Unit,
    onClaim: (String) -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = Color.White,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )

    tasks.forEach { task ->
        val p = progress[task.id] ?: DailyRewardTaskProgress()
        val ready = p.watchedAds >= task.requiredAds
        RewardTaskCard(
            task = task,
            progress = p,
            loading = loadingTaskId == task.id,
            canClaim = ready && !p.claimed,
            onWatchAd = { onWatchAd(task.id) },
            onClaim = { onClaim(task.id) },
        )
    }
}

@Composable
private fun RewardTaskCard(
    task: DailyRewardTask,
    progress: DailyRewardTaskProgress,
    loading: Boolean,
    canClaim: Boolean,
    onWatchAd: () -> Unit,
    onClaim: () -> Unit,
) {
    val accent = if (task.type == RewardType.COIN) Color(0xFFFFC857) else Color(0xFF7EE787)
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.09f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (task.type == RewardType.COIN) {
                    Text(
                        text = "🪙 ${task.rewardAmount} Coins",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = accent,
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_soul_cute_ghost),
                            contentDescription = "Soul",
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "${task.rewardAmount} Souls",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = accent,
                        )
                    }
                }
                Text(
                    text = if (progress.claimed) "Claimed" else "${progress.watchedAds}/${task.requiredAds} ads",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onWatchAd,
                    enabled = !loading && !progress.claimed && progress.watchedAds < task.requiredAds,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = 0.55f),
                    ),
                ) {
                    Text(if (loading) "Loading..." else "Watch Ad")
                }
                Button(onClick = onClaim, enabled = canClaim) {
                    Text("Claim")
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

