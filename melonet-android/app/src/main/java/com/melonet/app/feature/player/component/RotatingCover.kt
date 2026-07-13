package com.melonet.app.feature.player.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme

/**
 * A vinyl-style rotating cover. The rotation angle is accumulated manually so
 * that pausing freezes the disc in place (and resuming continues from there)
 * instead of snapping back to 0deg.
 */
@Composable
fun RotatingCover(
    coverUrl: String?,
    title: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    rotationDurationMs: Int = 16000,
) {
    val dimensions = MeloNetTheme.dimensions

    // Persist the current angle across recompositions and play/pause toggles.
    val angle = remember { mutableFloatStateOf(0f) }
    val degreesPerNano = 360f / (rotationDurationMs * 1_000_000f)

    // Advance the angle only while playing, driven by the frame clock so it
    // resumes smoothly from whatever value it currently holds.
    androidx.compose.runtime.LaunchedEffect(isPlaying) {
        if (!isPlaying) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            val delta = (now - last).coerceAtLeast(0)
            angle.floatValue = (angle.floatValue + delta * degreesPerNano) % 360f
            last = now
        }
    }

    // Gently scale down when paused for a subtle "settle" effect.
    val scale by animateFloatAsState(targetValue = if (isPlaying) 1f else 0.94f, label = "cover_scale")

    Box(
        modifier = modifier.size(dimensions.playerCoverSize),
        contentAlignment = Alignment.Center,
    ) {
        // Vinyl backing ring.
        Box(
            modifier = Modifier
                .size(dimensions.playerCoverSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF2A2A2A), Color(0xFF0D0D0D)),
                    ),
                ),
        )
        MeloImage(
            imageUrl = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(dimensions.playerCoverSize)
                .padding(18.dp)
                .graphicsLayer {
                    rotationZ = angle.floatValue
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .border(width = 2.dp, color = Color.White.copy(alpha = 0.08f), shape = CircleShape),
        )
        // Center spindle hole.
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color(0xFF111111))
                .border(width = 2.dp, color = Color.White.copy(alpha = 0.25f), shape = CircleShape),
        )
    }
}
