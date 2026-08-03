package com.melonet.app.feature.player.component

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.melonet.app.core.designsystem.component.MeloImage
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
    val coverShape = RoundedCornerShape(20.dp)

    val scale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.96f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 280f),
        label = "cover_scale",
    )

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
                .shadow(elevation = 16.dp, shape = coverShape)
                .clip(coverShape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.12f),
                    shape = coverShape,
                ),
        )
    }
}
