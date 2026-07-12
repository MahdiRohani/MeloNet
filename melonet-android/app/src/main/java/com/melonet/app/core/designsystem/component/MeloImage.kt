package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.melonet.app.R

@Composable
fun MeloImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetSize: Dp? = null,
) {
    if (imageUrl.isNullOrBlank()) {
        MeloImagePlaceholder(modifier = modifier)
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val pixelSize = targetSize?.let { with(density) { it.roundToPx() } }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .apply {
                if (pixelSize != null) {
                    size(pixelSize)
                    scale(Scale.FILL)
                    precision(Precision.INEXACT)
                }
            }
            .build(),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { MeloImagePlaceholder(modifier = Modifier.fillMaxSize()) },
        error = { MeloImagePlaceholder(modifier = Modifier.fillMaxSize()) },
    )
}

@Composable
private fun MeloImagePlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_album_placeholder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
