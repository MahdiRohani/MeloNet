package com.melonet.app.feature.player.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * A lively audio-style visualizer. Each bar chases a moving target amplitude
 * (a blend of layered sine waves plus a little randomness) so the motion reads
 * as reacting to music rather than a static loop. When playback is paused the
 * bars smoothly decay to a flat line.
 */
@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 48,
) {
    val dimensions = MeloNetTheme.dimensions
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    val heights = remember { FloatArray(barCount) }
    val seeds = remember { FloatArray(barCount) { Random.nextFloat() * 6.28f } }
    // A ticking clock drives redraws every frame while animating.
    val clock = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    LaunchedEffect(isPlaying) {
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000f).coerceIn(0f, 64f)
            last = now
            val t = now / 1_000_000_000f
            var maxH = 0f
            for (i in 0 until barCount) {
                val target = if (isPlaying) {
                    val wave = 0.5f + 0.5f * sin(t * 6f + seeds[i])
                    val fast = 0.3f * sin(t * 13f + i * 0.35f)
                    val jitter = 0.12f * sin(t * 21f + seeds[i] * 2f)
                    // Center bars a touch taller for an equalizer arc feel.
                    val centerBoost = 1f - abs(i - barCount / 2f) / (barCount / 1.4f)
                    ((wave + fast + jitter) * (0.55f + 0.45f * centerBoost)).coerceIn(0.05f, 1f)
                } else {
                    0.04f
                }
                // Exponential smoothing toward the target.
                val speed = if (isPlaying) 0.018f else 0.006f
                heights[i] += (target - heights[i]) * (speed * dt)
                maxH += heights[i]
            }
            clock.floatValue = t
            if (!isPlaying && maxH < 0.06f * barCount) break
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.visualizerHeight),
    ) {
        // Reading the clock here makes the Canvas redraw every frame.
        val tick = clock.floatValue
        if (tick.isNaN()) return@Canvas
        val gap = size.width * 0.006f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val maxHeight = size.height
        val brush = Brush.verticalGradient(listOf(primaryColor, secondaryColor))
        for (index in 0 until barCount) {
            val h = (maxHeight * heights[index]).coerceAtLeast(barWidth)
            val x = index * (barWidth + gap)
            val y = (maxHeight - h) / 2f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}
