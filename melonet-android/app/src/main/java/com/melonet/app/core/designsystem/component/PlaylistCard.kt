package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun PlaylistCard(
    title: String,
    songCount: Int,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions

    Column(
        modifier = modifier
            .width(dimensions.songCardSize)
            .clickable(onClick = onClick)
    ) {
        if (imageUrl.isNullOrBlank()) {
            ShimmerBox(
                modifier = Modifier.size(dimensions.songCardSize),
                shape = MaterialTheme.shapes.medium,
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(dimensions.songCardSize)
                    .clip(MaterialTheme.shapes.medium),
            )
        }
        Spacer(modifier = Modifier.height(spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = songCount.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
