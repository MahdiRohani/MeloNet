package com.melonet.app.feature.player.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Seek bar: drag updates UI only; [onSeek] fires on drag end / tap.
 * While [isSeeking], [positionMs] is the optimistic user value (no jump-back).
 */
@Composable
fun PlayerProgressBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isSeeking: Boolean = false,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    trackColor: Color = Color.White.copy(alpha = 0.25f),
    thumbColor: Color = Color.White,
) {
    val duration = durationMs.coerceAtLeast(1L)
    val actualFraction = (positionMs.toFloat() / duration).coerceIn(0f, 1f)

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    // Keep drag thumb in sync when parent reports optimistic seeking position.
    LaunchedEffect(positionMs, isSeeking, dragging) {
        if (!dragging && isSeeking) {
            dragFraction = actualFraction
        }
    }

    val fraction = when {
        dragging -> dragFraction
        isSeeking -> actualFraction
        else -> actualFraction
    }

    val pulse by rememberInfiniteTransition(label = "thumb_pulse").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse",
    )
    val glow = if (isPlaying && !dragging) pulse else 0.7f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(duration) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek((f * duration).toLong())
                }
            }
            .pointerInput(duration) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        onSeek((dragFraction * duration).toLong())
                        dragging = false
                    },
                    onDragCancel = { dragging = false },
                )
            },
    ) {
        val centerY = size.height / 2f
        val trackHeight = 6.dp.toPx()
        val activeWidth = size.width * fraction
        val thumbRadius = if (dragging) 11.dp.toPx() else 8.dp.toPx()

        drawRoundRect(
            color = trackColor,
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )
        if (activeWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(activeColor.copy(alpha = 0.85f), activeColor),
                    endX = activeWidth.coerceAtLeast(1f),
                ),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(activeWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
        }
        drawCircle(
            color = thumbColor.copy(alpha = 0.18f * glow),
            radius = thumbRadius * 2.1f,
            center = Offset(activeWidth, centerY),
        )
        drawCircle(
            color = thumbColor,
            radius = thumbRadius,
            center = Offset(activeWidth, centerY),
            style = Fill,
        )
    }
}
