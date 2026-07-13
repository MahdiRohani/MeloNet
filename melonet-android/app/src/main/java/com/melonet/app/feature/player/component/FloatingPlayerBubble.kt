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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.melonet.app.core.designsystem.component.MeloImage
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A draggable circular "bubble" mini-player. It can be moved anywhere and snaps
 * to the nearest horizontal edge on release, tucking half of itself off-screen
 * (a half-disc). Tapping it re-opens the full player.
 */
@Composable
fun FloatingPlayerBubble(
    coverUrl: String,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    bubbleSize: androidx.compose.ui.unit.Dp = 64.dp,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val sizePx = with(density) { bubbleSize.toPx() }
        val maxX = with(density) { maxWidth.toPx() } - sizePx
        val maxY = (with(density) { maxHeight.toPx() } - sizePx).coerceAtLeast(0f)

        val offsetX = remember { Animatable(maxX - with(density) { 12.dp.toPx() }) }
        val offsetY = remember { Animatable(maxY * 0.55f) }

        androidx.compose.runtime.LaunchedEffect(maxX, maxY) {
            offsetX.snapTo(offsetX.value.coerceIn(-sizePx / 2f, maxX + sizePx / 2f))
            offsetY.snapTo(offsetY.value.coerceIn(0f, maxY))
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .size(bubbleSize)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + drag.x).coerceIn(-sizePx / 2f, maxX + sizePx / 2f))
                                offsetY.snapTo((offsetY.value + drag.y).coerceIn(0f, maxY))
                            }
                        },
                        onDragEnd = {
                            val center = offsetX.value + sizePx / 2f
                            // Snap to whichever edge is closer, tucking half off-screen.
                            val targetX = if (center < (maxX + sizePx) / 2f) {
                                -sizePx / 2f
                            } else {
                                maxX + sizePx / 2f
                            }
                            scope.launch { offsetX.animateTo(targetX) }
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
            Box(
                modifier = Modifier
                    .size(bubbleSize)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onTogglePlayPause() })
                        },
                )
            }
        }
    }
}
