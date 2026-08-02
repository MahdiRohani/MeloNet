package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun PlaylistCard(
    title: String,
    songCount: Int,
    imageUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    coverUrls: List<String> = emptyList(),
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val mosaicUrls = coverUrls.filter { it.isNotBlank() }.ifEmpty {
        listOfNotNull(imageUrl?.takeIf { it.isNotBlank() })
    }

    Column(
        modifier = modifier
            .then(if (large) Modifier.fillMaxWidth() else Modifier.width(dimensions.songCardSize))
            .clickable(onClick = onClick),
    ) {
        CoverMosaic(
            coverUrls = mosaicUrls,
            modifier = Modifier
                .then(
                    if (large) {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    } else {
                        Modifier.size(dimensions.songCardSize)
                    },
                )
                .clip(if (large) MaterialTheme.shapes.large else MaterialTheme.shapes.medium),
        )
        Spacer(modifier = Modifier.height(spacing.sm))
        Text(
            text = title,
            style = if (large) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (large) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.playlists_song_count, songCount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
