package com.melonet.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melonet.app.core.ui.shimmerEffect
import com.melonet.app.domain.model.QuickAction
import com.melonet.app.domain.model.Song

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongClick: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeContract.Effect.NavigateToPlayer -> onSongClick(effect.songId)
                is HomeContract.Effect.ShowError -> Unit
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            HomeSlider(
                isLoading = state.isLoading,
                carouselTitle = state.feed?.carousel?.firstOrNull()?.title
            )
        }

        item {
            QuickActionsSection(
                actions = state.feed?.quickActions.orEmpty(),
                isLoading = state.isLoading,
                onActionClick = { viewModel.handleEvent(HomeContract.Event.QuickActionClicked(it)) }
            )
        }

        if (state.isLoading) {
            items(3) {
                SongSection(
                    title = "",
                    songs = emptyList(),
                    isLoading = true,
                    onSongClick = {}
                )
            }
        } else {
            items(state.feed?.rows.orEmpty(), key = { it.id }) { row ->
                SongSection(
                    title = row.title,
                    songs = row.items,
                    isLoading = false,
                    onSongClick = { songId ->
                        viewModel.handleEvent(HomeContract.Event.SongClicked(songId))
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun HomeSlider(isLoading: Boolean, carouselTitle: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isLoading) Modifier.shimmerEffect()
                else Modifier.background(MaterialTheme.colorScheme.primary)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!isLoading) {
            Text(
                text = carouselTitle ?: "MeloNet",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    actions: List<QuickAction>,
    isLoading: Boolean,
    onActionClick: (QuickAction) -> Unit
) {
    val defaultActions = listOf(
        Triple("لایک شده", Icons.Default.Favorite, null),
        Triple("اخیراً", Icons.Default.History, null),
        Triple("پلی‌لیست", Icons.Default.LibraryMusic, null),
        Triple("دنبال شده", Icons.Default.People, null)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (isLoading) {
            repeat(4) {
                QuickActionItemShimmer()
            }
        } else if (actions.isNotEmpty()) {
            actions.take(4).forEach { action ->
                QuickActionItem(
                    title = action.title,
                    icon = Icons.Default.Favorite,
                    onClick = { onActionClick(action) }
                )
            }
        } else {
            defaultActions.forEach { (title, icon, _) ->
                QuickActionItem(title = title, icon = icon, onClick = {})
            }
        }
    }
}

@Composable
private fun QuickActionItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun QuickActionItemShimmer() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(12.dp)
                .width(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

@Composable
private fun SongSection(
    title: String,
    songs: List<Song>,
    isLoading: Boolean,
    onSongClick: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        if (title.isNotBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.onBackground
            )
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(20.dp)
                    .fillMaxWidth(0.4f)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                items(5) { SongItemShimmerUi() }
            } else {
                items(songs, key = { it.id }) { song ->
                    SongCard(song = song, onClick = { onSongClick(song.id) })
                }
            }
        }
    }
}

@Composable
private fun SongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = song.artistName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SongItemShimmerUi() {
    Column(modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .height(14.dp)
                .fillMaxWidth(0.8f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(10.dp)
                .fillMaxWidth(0.5f)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}
