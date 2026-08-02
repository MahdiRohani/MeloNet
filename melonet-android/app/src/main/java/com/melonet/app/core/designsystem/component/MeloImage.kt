package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import com.melonet.app.R
import com.melonet.app.core.network.MediaUrl

enum class MeloImageFallback {
    Album,
    Person,
}

@Composable
fun MeloImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    targetSize: Dp? = null,
    fallback: MeloImageFallback = MeloImageFallback.Album,
    /** Prefer false in dense lists (Home rows) to avoid decode + fade jank. */
    crossfade: Boolean = true,
) {
    val resolvedUrl = MediaUrl.resolve(imageUrl)
    if (resolvedUrl.isNullOrBlank()) {
        MeloImagePlaceholder(modifier = modifier, fallback = fallback)
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val pixelSize = targetSize?.let { with(density) { it.roundToPx() } }
    val request = remember(resolvedUrl, pixelSize, crossfade) {
        ImageRequest.Builder(context)
            .data(resolvedUrl)
            .crossfade(crossfade)
            .apply {
                if (pixelSize != null) {
                    size(pixelSize)
                    scale(Scale.FILL)
                    precision(Precision.INEXACT)
                }
            }
            .build()
    }

    SubcomposeAsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
        loading = { MeloImagePlaceholder(modifier = Modifier.fillMaxSize(), fallback = fallback) },
        error = { MeloImagePlaceholder(modifier = Modifier.fillMaxSize(), fallback = fallback) },
    )
}

@Composable
private fun MeloImagePlaceholder(
    modifier: Modifier = Modifier,
    fallback: MeloImageFallback,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        when (fallback) {
            MeloImageFallback.Album -> {
                Icon(
                    painter = painterResource(R.drawable.ic_album_placeholder),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            MeloImageFallback.Person -> {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
