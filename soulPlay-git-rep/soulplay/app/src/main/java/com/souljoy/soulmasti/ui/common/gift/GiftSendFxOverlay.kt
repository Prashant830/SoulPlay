package com.souljoy.soulmasti.ui.common.gift

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.souljoy.soulmasti.domain.gift.GiftCatalog
import com.souljoy.soulmasti.domain.gift.GiftFxResources
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

/**
 * Full-screen layer: plays the gift Lottie once, then invokes [onFinished] (caller runs normal send).
 */
@Composable
fun GiftSendFxOverlay(
    giftId: String,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val lottieRaw = remember(giftId) { GiftFxResources.lottieRawRes(context, giftId) }

    var finished by remember(giftId) { mutableStateOf(false) }
    fun finishOnce() {
        if (finished) return
        finished = true
        onFinished()
    }

    if (lottieRaw != 0) {
        val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(lottieRaw))
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = 1,
            restartOnPlay = false,
        )
        LaunchedEffect(composition, giftId) {
            snapshotFlow { progress }
                .filter { p -> p >= 0.99f }
                .first()
            finishOnce()
        }
        LaunchedEffect(giftId) {
            delay(18_000)
            finishOnce()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(16.dp),
            )
        }
    } else {
        LaunchedEffect(giftId) {
            delay(700L)
            finishOnce()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = GiftCatalog.displayLabel(giftId),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}
