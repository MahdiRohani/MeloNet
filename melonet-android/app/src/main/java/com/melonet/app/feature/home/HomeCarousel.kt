package com.melonet.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.shimmerEffect
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Song
import kotlinx.coroutines.delay

private const val CAROUSEL_AUTO_SCROLL_MS = 4_000L

@Composable
fun HomeCarousel(
    songs: List<Song>,
    isLoading: Boolean,
    onSongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.carouselHeight)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
    ) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.large)
                        .shimmerEffect(),
                )
            }
            songs.isEmpty() -> Unit
            else -> {
                val pagerState = rememberPagerState(pageCount = { songs.size })

                LaunchedEffect(songs.size) {
                    if (songs.size <= 1) return@LaunchedEffect
                    while (true) {
                        delay(CAROUSEL_AUTO_SCROLL_MS)
                        val nextPage = (pagerState.currentPage + 1) % songs.size
                        pagerState.animateScrollToPage(nextPage)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val song = songs[page]
                    CarouselSlide(
                        song = song,
                        onClick = { onSongClick(song) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CarouselSlide(
    song: Song,
    onClick: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick),
    ) {
        MeloImage(
            imageUrl = song.coverUrl,
            contentDescription = song.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(spacing.md),
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier // A6: sharedElement(PlayerSharedKeys.songCover(song.id))
            )
            Text(
                text = song.artistName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
