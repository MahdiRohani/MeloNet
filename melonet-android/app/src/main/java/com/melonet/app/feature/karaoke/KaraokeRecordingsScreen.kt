package com.melonet.app.feature.karaoke

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.repository.KaraokeRecording
import com.melonet.app.data.repository.KaraokeRecordingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KaraokeRecordingsViewModel(
    private val repository: KaraokeRecordingRepository,
) : ViewModel() {
    val recordings = repository.observeRecordings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KaraokeRecordingsScreen(
    viewModel: KaraokeRecordingsViewModel,
    onNavigateBack: () -> Unit,
    onPlayRecording: (Long) -> Unit,
) {
    val recordings by viewModel.recordings.collectAsState()
    val spacing = MeloNetTheme.spacing
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.karaoke_my_takes)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (recordings.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.karaoke_takes_empty_title),
                description = stringResource(R.string.karaoke_takes_empty_description),
                icon = Icons.Default.Mic,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = spacing.sm),
            ) {
                items(recordings, key = { it.id }) { recording ->
                    KaraokeTakeRow(
                        recording = recording,
                        onPlay = { onPlayRecording(recording.id) },
                        onDelete = { viewModel.delete(recording.id) },
                        onShare = {
                            shareRecording(context, recording)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun KaraokeTakeRow(
    recording: KaraokeRecording,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay),
        leadingContent = {
            MeloImage(
                imageUrl = recording.coverUrl.ifBlank { null },
                contentDescription = recording.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
        },
        headlineContent = {
            Text(recording.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column {
                Text(
                    text = recording.artistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatDate(recording.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete))
                }
            }
        },
    )
}

private fun formatDate(epochMs: Long): String {
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return fmt.format(Date(epochMs))
}

private fun shareRecording(context: android.content.Context, recording: KaraokeRecording) {
    val file = File(recording.vocalPath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file,
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "audio/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
            Intent.EXTRA_TEXT,
            context.getString(R.string.karaoke_share_text, recording.title, recording.artistName),
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(
        Intent.createChooser(intent, context.getString(R.string.player_more_share)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
    )
}
