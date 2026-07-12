package com.melonet.app.feature.player.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun RotatingCover(
    coverUrl: String?,
    title: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimensions = MeloNetTheme.dimensions
    val transition = rememberInfiniteTransition(label = "cover_rotation")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
        ),
        label = "rotation",
    )

    MeloImage(
        imageUrl = coverUrl,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(dimensions.playerCoverSize)
            .clip(CircleShape)
            .rotate(if (isPlaying) rotation else 0f),
    )
}
