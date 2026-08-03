package com.melonet.app.feature.player.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloMotion
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.core.ui.LocalNavAnimatedVisibilityScope
import com.melonet.app.core.ui.LocalSharedTransitionScope

/**
 * Simple static album cover — no spin, no halo, no poster effects.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SimpleCoverArt(
    coverUrl: String?,
    title: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    sharedTransitionKey: String? = null,
) {
    val dimensions = MeloNetTheme.dimensions
    val elevation = MeloNetTheme.elevation
    val colors = MeloNetTheme.colors
    val coverShape = MaterialTheme.shapes.extraLarge

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else MeloMotion.coverPausedScale,
        animationSpec = MeloMotion.coverSpring,
        label = "cover_scale",
    )

    val sharedScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    // Key must be non-null whenever rememberSharedContentState runs (song id can arrive late).
    val transitionKey = sharedTransitionKey ?: "player_cover_placeholder"
    val sharedModifier = if (sharedScope != null && animatedVisibilityScope != null) {
        with(sharedScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = transitionKey),
                animatedVisibilityScope = animatedVisibilityScope,
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(dimensions.playerCoverSize)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        MeloImage(
            imageUrl = coverUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(sharedModifier)
                .shadow(elevation = elevation.xxl, shape = coverShape)
                .clip(coverShape)
                .border(
                    width = dimensions.borderHairline,
                    color = colors.coverRim,
                    shape = coverShape,
                ),
        )
    }
}
