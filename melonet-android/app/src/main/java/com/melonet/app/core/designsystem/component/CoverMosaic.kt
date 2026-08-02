package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/** 1–4 song covers in a mosaic (Liked/Recent style). */
@Composable
fun CoverMosaic(
    coverUrls: List<String>,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val urls = coverUrls.map { it.trim() }.filter { it.isNotEmpty() }.take(4)
    Box(
        modifier = modifier.background(scheme.primary.copy(alpha = 0.22f)),
    ) {
        when {
            urls.isEmpty() -> Unit
            urls.size == 1 -> {
                MeloImage(
                    imageUrl = urls.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    targetSize = 420.dp,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            urls.size == 2 -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    MosaicTile(urls[0], Modifier.weight(1f))
                    MosaicTile(urls[1], Modifier.weight(1f))
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(1f)) {
                        MosaicTile(urls.getOrNull(0), Modifier.weight(1f))
                        MosaicTile(urls.getOrNull(1), Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.weight(1f)) {
                        MosaicTile(urls.getOrNull(2), Modifier.weight(1f))
                        if (urls.size > 3) {
                            MosaicTile(urls.getOrNull(3), Modifier.weight(1f))
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MosaicTile(url: String?, modifier: Modifier) {
    if (url == null) {
        Spacer(modifier = modifier.fillMaxSize())
        return
    }
    MeloImage(
        imageUrl = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize(),
    )
}
