package com.melonet.app.feature.player.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DynamicPlayerBackground(
    gradientColors: List<Long>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = remember(gradientColors) {
        if (gradientColors.size >= 2) {
            gradientColors.map { Color(it) }
        } else {
            listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
        }
    }

    val infinite = rememberInfiniteTransition(label = "player_bg")
    val driftA by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift_a",
    )
    val driftB by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(24_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "drift_b",
    )
    val orbit by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(40_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit",
    )

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Base wash from cover palette.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colors.first().copy(alpha = 0.95f),
                        colors.getOrElse(1) { colors.first() }.copy(alpha = 0.9f),
                        colors.last().copy(alpha = 1f),
                    ),
                ),
            )

            val c0 = colors[0]
            val c1 = colors.getOrElse(1) { colors[0] }
            val c2 = colors.getOrElse(2) { colors.last() }

            val w = size.width
            val h = size.height
            val rad = (orbit * PI / 180.0).toFloat()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c0.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(w * (0.22f + driftA * 0.18f), h * (0.18f + driftB * 0.12f)),
                    radius = w * 0.55f,
                ),
                radius = w * 0.55f,
                center = Offset(w * (0.22f + driftA * 0.18f), h * (0.18f + driftB * 0.12f)),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c1.copy(alpha = 0.34f), Color.Transparent),
                    center = Offset(
                        w * (0.78f + cos(rad) * 0.06f),
                        h * (0.42f + sin(rad) * 0.08f),
                    ),
                    radius = w * 0.48f,
                ),
                radius = w * 0.48f,
                center = Offset(
                    w * (0.78f + cos(rad) * 0.06f),
                    h * (0.42f + sin(rad) * 0.08f),
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(c2.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(w * (0.45f - driftB * 0.1f), h * (0.78f - driftA * 0.08f)),
                    radius = w * 0.6f,
                ),
                radius = w * 0.6f,
                center = Offset(w * (0.45f - driftB * 0.1f), h * (0.78f - driftA * 0.08f)),
            )

            // Subtle vignette for text readability.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.18f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.45f),
                    ),
                ),
            )
        }
        content()
    }
}
