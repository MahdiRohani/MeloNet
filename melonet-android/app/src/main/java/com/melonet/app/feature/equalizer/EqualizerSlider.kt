package com.melonet.app.feature.equalizer

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
 * Equalizer band / effect slider styled like [com.melonet.app.feature.player.component.PlayerProgressBar].
 * Drag updates the thumb and [onValueChange] immediately (no tween lag).
 */
@Composable
fun EqualizerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color,
    trackColor: Color,
    thumbColor: Color = activeColor,
) {
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it != 0f } ?: 1f
    val actualFraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(actualFraction) }

    LaunchedEffect(value, dragging) {
        if (!dragging) dragFraction = actualFraction
    }

    val fraction = if (dragging) dragFraction else actualFraction

    val pulse by rememberInfiniteTransition(label = "eq_thumb_pulse").animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "eq_pulse",
    )
    val glow = if (enabled && !dragging) pulse else 0.65f

    fun fractionToValue(f: Float): Float =
        valueRange.start + f.coerceIn(0f, 1f) * span

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    dragFraction = f
                    onValueChange(fractionToValue(f))
                }
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragging = true
                        dragFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onValueChange(fractionToValue(dragFraction))
                    },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onValueChange(fractionToValue(dragFraction))
                    },
                    onDragEnd = { dragging = false },
                    onDragCancel = { dragging = false },
                )
            },
    ) {
        val centerY = size.height / 2f
        val trackHeight = 6.dp.toPx()
        val activeWidth = size.width * fraction
        val thumbRadius = if (dragging) 12.dp.toPx() else 9.dp.toPx()
        val drawActive = if (enabled) activeColor else activeColor.copy(alpha = 0.35f)
        val drawTrack = if (enabled) trackColor else trackColor.copy(alpha = 0.35f)
        val drawThumb = if (enabled) thumbColor else thumbColor.copy(alpha = 0.4f)

        drawRoundRect(
            color = drawTrack,
            topLeft = Offset(0f, centerY - trackHeight / 2f),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(trackHeight / 2f),
        )
        if (activeWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(drawActive.copy(alpha = 0.85f), drawActive),
                    endX = activeWidth.coerceAtLeast(1f),
                ),
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(activeWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
        }
        drawCircle(
            color = drawThumb.copy(alpha = 0.18f * glow),
            radius = thumbRadius * 2.1f,
            center = Offset(activeWidth, centerY),
        )
        drawCircle(
            color = drawThumb,
            radius = thumbRadius,
            center = Offset(activeWidth, centerY),
            style = Fill,
        )
    }
}
