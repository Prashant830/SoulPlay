package com.souljoy.soulmasti.ui.common.gift

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.souljoy.soulmasti.domain.gift.GiftFxResources

/**
 * Queues full-screen Lottie celebrations (one at a time). Use [enqueueIfFx] only after a gift
 * actually succeeds (or when showing a remote peer’s gift in chat / voice room).
 */
class GiftCelebrationQueue {
    private data class Item(val giftId: String, val onFinished: (() -> Unit)?)

    private val queue = ArrayDeque<Item>()
    private var appContext: Context? = null
    private var pendingOnFinished: (() -> Unit)? = null

    var currentGiftId: String? by mutableStateOf(null)
        private set

    /**
     * Plays full-screen FX when [GiftFxResources] has a Lottie for [giftId].
     * [onAfterCelebration] runs when that segment ends (or immediately if there is no FX for this gift).
     */
    fun enqueueIfFx(context: Context, giftId: String, onAfterCelebration: (() -> Unit)? = null) {
        val app = context.applicationContext.also { appContext = it }
        if (!GiftFxResources.hasAnyFx(app, giftId)) {
            onAfterCelebration?.invoke()
            return
        }
        queue.addLast(Item(giftId, onAfterCelebration))
        if (currentGiftId == null) {
            playNext(app)
        }
    }

    private fun playNext(c: Context) {
        pendingOnFinished = null
        currentGiftId = null
        while (queue.isNotEmpty()) {
            val item = queue.removeFirst()
            if (GiftFxResources.hasAnyFx(c, item.giftId)) {
                currentGiftId = item.giftId
                pendingOnFinished = item.onFinished
                return
            }
            item.onFinished?.invoke()
        }
    }

    fun onOverlayFinished() {
        pendingOnFinished?.invoke()
        pendingOnFinished = null
        val c = appContext ?: return
        playNext(c)
    }
}

@Composable
fun rememberGiftCelebrationQueue(): GiftCelebrationQueue = remember { GiftCelebrationQueue() }

/** Full-screen overlay; place last in a [Box] so it draws above content. */
@Composable
fun GiftCelebrationOverlayHost(queue: GiftCelebrationQueue) {
    val id = queue.currentGiftId ?: return
    Box(Modifier.fillMaxSize()) {
        GiftSendFxOverlay(
            giftId = id,
            onFinished = { queue.onOverlayFinished() },
        )
    }
}
