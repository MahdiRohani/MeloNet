package com.melonet.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.shimmerEffect
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.HomeRow
import kotlinx.coroutines.delay

private const val CAROUSEL_AUTO_SCROLL_MS = 4_000L

/** A single carousel slide that represents a whole category (home row). */
data class CarouselCategory(
    val title: String,
    val coverUrl: String,
    val row: HomeRow,
)

@Composable
fun HomeCarousel(
    categories: List<CarouselCategory>,
    isLoading: Boolean,
    onCategoryClick: (HomeRow) -> Unit,
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
            categories.isEmpty() -> Unit
            else -> {
                val pagerState = rememberPagerState(pageCount = { categories.size })

                LaunchedEffect(categories.size) {
                    if (categories.size <= 1) return@LaunchedEffect
                    while (true) {
                        delay(CAROUSEL_AUTO_SCROLL_MS)
                        val nextPage = (pagerState.currentPage + 1) % categories.size
                        pagerState.animateScrollToPage(nextPage)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val category = categories[page]
                    CarouselSlide(
                        category = category,
                        onClick = { onCategoryClick(category.row) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CarouselSlide(
    category: CarouselCategory,
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
            imageUrl = category.coverUrl,
            contentDescription = category.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.home_carousel_explore),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
