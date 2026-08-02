package com.melonet.app.feature.player.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.Lyrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    lyrics: Lyrics,
    isLoading: Boolean,
    currentLineIndex: Int,
    lyricsOffsetMs: Long,
    synced: Boolean,
    onSeekToLine: (Int) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val listState = rememberLazyListState()

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0) {
            runCatching {
                listState.animateScrollToItem(currentLineIndex.coerceAtLeast(0))
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 320.dp, max = 560.dp)
                .padding(bottom = spacing.lg),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.lg),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.player_lyrics_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                when {
                    isLoading -> Unit
                    lyrics.isEmpty -> {
                        Text(
                            text = stringResource(R.string.player_lyrics_missing_badge),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    synced -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onAdjustOffset(-500L) }) {
                                Text("−0.5s")
                            }
                            Text(
                                text = if (lyricsOffsetMs == 0L) {
                                    stringResource(R.string.karaoke_synced_badge)
                                } else {
                                    stringResource(R.string.player_lyrics_offset_ms, lyricsOffsetMs)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            TextButton(onClick = { onAdjustOffset(500L) }) {
                                Text("+0.5s")
                            }
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.xl),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                lyrics.isEmpty -> {
                    EmptyState(
                        title = stringResource(R.string.player_lyrics_empty_title),
                        description = stringResource(R.string.player_lyrics_empty_description),
                        icon = Icons.Filled.MusicOff,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(spacing.lg),
                    )
                }
                else -> {
                    if (!synced) {
                        Text(
                            text = stringResource(R.string.karaoke_unsynced_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = spacing.lg, vertical = spacing.xs),
                        )
                    }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.md),
                        verticalArrangement = Arrangement.spacedBy(spacing.md),
                    ) {
                        itemsIndexed(lyrics.lines, key = { index, line -> "${line.timeMs}_$index" }) { index, line ->
                            val active = index == currentLineIndex
                            Text(
                                text = line.text.ifBlank { "♪" },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSeekToLine(index) },
                                textAlign = TextAlign.Center,
                                style = if (active) {
                                    MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 22.sp,
                                    )
                                } else {
                                    MaterialTheme.typography.titleMedium
                                },
                                color = if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
