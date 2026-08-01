package com.melonet.app.feature.player.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melonet.app.core.designsystem.theme.MeloNetTheme

/**
 * Waveform-driven visualizer. Bars grow from the bottom; on pause the canvas
 * fades out via alpha instead of collapsing bar heights to zero.
 */
@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    magnitudes: FloatArray,
    modifier: Modifier = Modifier,
    barCount: Int = 48,
    height: Dp? = null,
) {
    val dimensions = MeloNetTheme.dimensions
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val alpha by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(durationMillis = if (isPlaying) 280 else 450),
        label = "visualizer_alpha",
    )

    // contentHashCode forces Canvas invalidation when amplitude frames change.
    val frameKey = remember(magnitudes) { magnitudes.contentHashCode() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height ?: dimensions.visualizerHeight)
            .alpha(alpha),
    ) {
        @Suppress("UNUSED_EXPRESSION")
        frameKey
        if (alpha <= 0.01f) return@Canvas
        val gap = size.width * 0.018f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val maxHeight = size.height
        val brush = Brush.verticalGradient(
            listOf(primaryColor.copy(alpha = 0.55f), primaryColor, secondaryColor),
        )
        val corner = CornerRadius(barWidth / 2f, barWidth / 2f)
        for (index in 0 until barCount) {
            val sample = if (magnitudes.isNotEmpty()) {
                magnitudes[(index * magnitudes.size) / barCount]
            } else {
                0.05f
            }
            // Emphasize contrast between quiet/loud bars for clearer motion.
            val h = (maxHeight * sample.coerceIn(0.02f, 1f).let { it * it * (3f - 2f * it) })
                .coerceAtLeast(3f)
            val x = index * (barWidth + gap)
            val y = maxHeight - h
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = corner,
            )
        }
    }
}
