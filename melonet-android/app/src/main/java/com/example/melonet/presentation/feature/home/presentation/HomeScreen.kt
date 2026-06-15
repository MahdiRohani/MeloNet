package com.example.melonet.presentation.feature.home.presentation


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.melonet.data.model.SongDto
import com.example.melonet.core.ui.shimmerEffect


@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onSongClick: (Int) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        item {
            HomeSlider(isLoading = state.isLoading)
        }

        item {
            QuickActionsSection()
        }

        item {
            SongSection(
                title = "جدیدترین آهنگ‌ها",
                songs = state.newSongs,
                isLoading = state.isLoading,
                onSongClick = onSongClick
            )
        }

        item {
            SongSection(
                title = "محبوب‌ترین‌ها",
                songs = state.popularSongs,
                isLoading = state.isLoading,
                onSongClick = onSongClick
            )
        }

        item {
            SongSection(
                title = "موسیقی ایرانی",
                songs = state.iranianSongs,
                isLoading = state.isLoading,
                onSongClick = onSongClick
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun HomeSlider(isLoading: Boolean) {
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
                text = "🔥 برترین‌های هفته",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun QuickActionsSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionItem("لایک شده", Icons.Default.Favorite)
        QuickActionItem("اخیراً", Icons.Default.History)
        QuickActionItem("پلی‌لیست", Icons.Default.LibraryMusic)
        QuickActionItem("دنبال شده", Icons.Default.People)
    }
}

@Composable
fun QuickActionItem(title: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { /* Handle Click */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SongSection(
    title: String,
    songs: List<SongDto>,
    isLoading: Boolean,
    onSongClick: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                items(5) {
                    SongItemShimmerUi()
                }
            } else {
                items(songs) { song ->
                    SongCard(song = song, onClick = { onSongClick(song.id) })
                }
            }
        }
    }
}

@Composable
fun SongCard(song: SongDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.DarkGray) // بعداً با Coil جایگزین می‌شود تا عکس از نت لود شود
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun SongItemShimmerUi() {
    Column(modifier = Modifier.width(120.dp)) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.8f).clip(RoundedCornerShape(4.dp)).shimmerEffect())
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.height(10.dp).fillMaxWidth(0.5f).clip(RoundedCornerShape(4.dp)).shimmerEffect())
    }
}