package com.melonet.app.feature.player.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.ui.LocalNavAnimatedVisibilityScope
import com.melonet.app.core.ui.LocalSharedTransitionScope

/**
 * A vinyl-style rotating cover. The rotation angle is accumulated manually so
 * that pausing freezes the disc in place (and resuming continues from there)
 * instead of snapping back to 0deg.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun RotatingCover(
    coverUrl: String?,
    title: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    rotationDurationMs: Int = 16000,
    sharedTransitionKey: String? = null,
) {
    val dimensions = MeloNetTheme.dimensions
    val colors = MeloNetTheme.colors

    val angle = remember { mutableFloatStateOf(0f) }
    val degreesPerNano = 360f / (rotationDurationMs * 1_000_000f)

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

    val scale by animateFloatAsState(targetValue = if (isPlaying) 1f else 0.94f, label = "cover_scale")
    val sharedScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val sharedModifier = if (
        sharedTransitionKey != null &&
        sharedScope != null &&
        animatedVisibilityScope != null
    ) {
        with(sharedScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = sharedTransitionKey),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier.size(dimensions.playerCoverSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(dimensions.playerCoverSize)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(colors.vinylInner, colors.vinylOuter),
                    ),
                ),
        )
        MeloImage(
            imageUrl = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(dimensions.playerCoverSize)
                .then(sharedModifier)
                .padding(18.dp)
                .graphicsLayer {
                    rotationZ = angle.floatValue
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .border(width = 2.dp, color = colors.vinylRim, shape = CircleShape),
        )
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(colors.vinylSpindle)
                .border(width = 2.dp, color = colors.vinylRim.copy(alpha = 0.25f), shape = CircleShape),
        )
    }
}
