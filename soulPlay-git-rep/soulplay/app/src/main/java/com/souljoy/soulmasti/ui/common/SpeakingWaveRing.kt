package com.souljoy.soulmasti.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SpeakingWaveRings(
    speakingLevel: Float,
    ringColor: Color,
    modifier: Modifier = Modifier
) {
    val active = speakingLevel > 0.035f
    val transition = rememberInfiniteTransition(label = "speakRings")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringPhase"
    )
    if (!active) return
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        for (i in 2 downTo 0) {
            val stagger = i / 3f
            val wave = (sin((phase + stagger) * 2 * PI).toFloat() * 0.5f + 0.5f)
            val scale = 1f + 0.1f * wave + 0.12f * speakingLevel.coerceIn(0f, 1f)
            val alpha = (0.5f - i * 0.14f) * (0.4f + 0.6f * speakingLevel)
            Box(
                modifier = Modifier
                    .size((76 + (i + 1) * 12).dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .border(2.dp, ringColor.copy(alpha = 0.7f), CircleShape)
            )
        }
    }
}