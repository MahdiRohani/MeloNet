package com.melonet.app.feature.player.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 32,
) {
    val dimensions = MeloNetTheme.dimensions
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val transition = rememberInfiniteTransition(label = "visualizer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.visualizerHeight),
    ) {
        if (!isPlaying) return@Canvas
        val barWidth = size.width / (barCount * 2f)
        val maxHeight = size.height
        for (index in 0 until barCount) {
            val normalized = sin(Math.toRadians((phase + index * 12f).toDouble())).toFloat()
            val barHeight = maxHeight * (0.25f + 0.75f * ((normalized + 1f) / 2f))
            val x = index * (barWidth * 2f) + barWidth / 2f
            val y = (maxHeight - barHeight) / 2f
            drawRoundRect(
                color = if (index % 2 == 0) primaryColor else secondaryColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
