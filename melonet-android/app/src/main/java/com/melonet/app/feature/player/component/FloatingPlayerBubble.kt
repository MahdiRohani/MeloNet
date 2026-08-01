package com.melonet.app.feature.player.component

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.melonet.app.core.designsystem.component.MeloImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Draggable cover bubble. Tap opens the full player. Snaps to an edge only when
 * released within [snapThresholdDp] of a horizontal edge; otherwise stays put.
 */
@Composable
fun FloatingPlayerBubble(
    coverUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bubbleSize: androidx.compose.ui.unit.Dp = 64.dp,
    snapThresholdDp: androidx.compose.ui.unit.Dp = 28.dp,
    initialOffset: Pair<Float, Float>? = null,
    onPositionChanged: (Float, Float) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizePx = with(density) { bubbleSize.toPx() }
        val snapThresholdPx = with(density) { snapThresholdDp.toPx() }
        val maxX = (with(density) { maxWidth.toPx() } - sizePx).coerceAtLeast(0f)
        val maxY = (with(density) { maxHeight.toPx() } - sizePx).coerceAtLeast(0f)

        val defaultX = (maxX - with(density) { 12.dp.toPx() }).coerceAtLeast(0f)
        val defaultY = (maxY * 0.55f).coerceIn(0f, maxY)
        val startX = initialOffset?.first?.coerceIn(0f, maxX) ?: defaultX
        val startY = initialOffset?.second?.coerceIn(0f, maxY) ?: defaultY

        val offsetX = remember { Animatable(startX) }
        val offsetY = remember { Animatable(startY) }

        LaunchedEffect(initialOffset, maxX, maxY) {
            if (initialOffset != null) {
                offsetX.snapTo(initialOffset.first.coerceIn(0f, maxX))
                offsetY.snapTo(initialOffset.second.coerceIn(0f, maxY))
            } else {
                offsetX.snapTo(offsetX.value.coerceIn(0f, maxX))
                offsetY.snapTo(offsetY.value.coerceIn(0f, maxY))
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .size(bubbleSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .pointerInput(maxX, maxY, snapThresholdPx) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + drag.x).coerceIn(0f, maxX))
                                offsetY.snapTo((offsetY.value + drag.y).coerceIn(0f, maxY))
                            }
                        },
                        onDragEnd = {
                            val x = offsetX.value
                            val y = offsetY.value
                            val nearLeft = x < snapThresholdPx
                            val nearRight = abs(maxX - x) < snapThresholdPx
                            scope.launch {
                                when {
                                    nearLeft -> offsetX.animateTo(0f)
                                    nearRight -> offsetX.animateTo(maxX)
                                }
                                onPositionChanged(offsetX.value, offsetY.value.coerceIn(0f, maxY))
                            }
                        },
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                },
            contentAlignment = Alignment.Center,
        ) {
            MeloImage(
                imageUrl = coverUrl.ifBlank { null },
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(bubbleSize)
                    .clip(CircleShape),
            )
        }
    }
}
