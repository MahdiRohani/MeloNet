package com.melonet.app.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.melonet.app.core.designsystem.theme.MeloMotion
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun SongCard(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sharedTransitionKey: String? = null,
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = MeloMotion.pressSpring,
        label = "song_card_press",
    )

    Column(
        modifier = modifier
            .width(dimensions.songCardSize)
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick,
            ),
    ) {
        MeloImage(
            imageUrl = imageUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(dimensions.songCardSize)
                .clip(MaterialTheme.shapes.medium)
                .then(
                    if (sharedTransitionKey != null) {
                        Modifier
                    } else {
                        Modifier
                    },
                ),
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SongCardShimmer(modifier: Modifier = Modifier) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions

    Column(modifier = modifier.width(dimensions.songCardSize)) {
        ShimmerBox(
            modifier = Modifier.size(dimensions.songCardSize),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        ShimmerBox(
            modifier = Modifier
                .height(dimensions.shimmerTextHeight)
                .fillMaxWidth(dimensions.shimmerWidthFractionLg),
            shape = MaterialTheme.shapes.extraSmall,
        )
        Spacer(modifier = Modifier.height(spacing.xs))
        ShimmerBox(
            modifier = Modifier
                .height(dimensions.shimmerSubtextHeight)
                .fillMaxWidth(dimensions.shimmerWidthFractionMd),
            shape = MaterialTheme.shapes.extraSmall,
        )
    }
}
