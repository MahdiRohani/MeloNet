package com.melonet.app.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import com.melonet.app.core.designsystem.theme.MeloNetTheme

fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val meloColors = MeloNetTheme.colors
    val primary = MaterialTheme.colorScheme.primary

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val startOffsetX by transition.animateFloat(
        initialValue = -2.4f * size.width.toFloat(),
        targetValue = 2.4f * size.width.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer_offset",
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.onBackground.copy(alpha = meloColors.shimmerBase),
        primary.copy(alpha = meloColors.shimmerHighlight * 0.55f),
        MaterialTheme.colorScheme.onBackground.copy(alpha = meloColors.shimmerHighlight),
        primary.copy(alpha = meloColors.shimmerHighlight * 0.55f),
        MaterialTheme.colorScheme.onBackground.copy(alpha = meloColors.shimmerBase),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + size.width.toFloat() * 1.15f, size.height.toFloat()),
        ),
    ).onGloballyPositioned {
        size = it.size
    }
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .shimmerEffect(),
    )
}
