package com.melonet.app.feature.downloads

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.melonet.app.R
import com.melonet.app.core.designsystem.component.EmptyState
import com.melonet.app.core.designsystem.component.MeloButton
import com.melonet.app.core.designsystem.component.MeloButtonVariant
import com.melonet.app.core.designsystem.component.MeloFilterChip
import com.melonet.app.core.designsystem.component.MeloImage
import com.melonet.app.core.designsystem.component.PremiumSubscriptionCard
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.DownloadItem
import com.melonet.app.data.model.DownloadSort
import com.melonet.app.data.model.DownloadStatus
import com.melonet.app.data.model.Song

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onPlaySong: (Song) -> Unit,
    onNavigateToProfile: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = MeloNetTheme.spacing

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DownloadsContract.Effect.PlaySong -> onPlaySong(effect.item.toSong())
                DownloadsContract.Effect.NavigateToProfile -> onNavigateToProfile()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.md),
    ) {
        Text(
            text = stringResource(R.string.downloads_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = spacing.md),
        )

        if (!state.isPremium) {
            PremiumSubscriptionCard(
                isPremium = false,
                onActionClick = { viewModel.handleEvent(DownloadsContract.Event.UpgradePremiumClicked) },
                modifier = Modifier.padding(bottom = spacing.md),
            )
            EmptyState(
                title = stringResource(R.string.downloads_premium_required_title),
                description = stringResource(R.string.downloads_premium_required_description),
                icon = Icons.Outlined.CloudDownload,
            )
            return@Column
        }

        DownloadSortRow(
            selectedSort = state.sort,
            onSortSelected = { sort ->
                viewModel.handleEvent(DownloadsContract.Event.SortChanged(sort))
            },
        )

        if (state.downloads.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.downloads_empty_title),
                description = stringResource(R.string.downloads_empty_description),
                icon = Icons.Outlined.CloudDownload,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                items(state.downloads, key = { it.songId }) { item ->
                    DownloadListItem(
                        item = item,
                        onClick = {
                            if (item.status == DownloadStatus.COMPLETED) {
                                viewModel.handleEvent(DownloadsContract.Event.PlayDownload(item))
                            }
                        },
                        onDismiss = {
                            viewModel.handleEvent(DownloadsContract.Event.DeleteDownload(item.songId))
                        },
                        onRetry = {
                            viewModel.handleEvent(DownloadsContract.Event.RetryDownload(item.songId))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadSortRow(
    selectedSort: DownloadSort,
    onSortSelected: (DownloadSort) -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        MeloFilterChip(
            label = stringResource(R.string.downloads_sort_newest),
            selected = selectedSort == DownloadSort.NEWEST,
            onClick = { onSortSelected(DownloadSort.NEWEST) },
        )
        MeloFilterChip(
            label = stringResource(R.string.downloads_sort_title),
            selected = selectedSort == DownloadSort.TITLE,
            onClick = { onSortSelected(DownloadSort.TITLE) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadListItem(
    item: DownloadItem,
    onClick: () -> Unit,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDismiss()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cd_delete_download),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(spacing.md),
                )
            }
        },
    ) {
        Column {
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (item.status == DownloadStatus.COMPLETED) {
                            Modifier.clickable(onClick = onClick)
                        } else {
                            Modifier
                        },
                    ),
                headlineContent = {
                    Text(
                        text = item.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                supportingContent = {
                    Column {
                        Text(
                            text = item.artistName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        when (item.status) {
                            DownloadStatus.DOWNLOADING, DownloadStatus.PENDING -> {
                                Text(
                                    text = stringResource(R.string.downloads_status_downloading, item.progress),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DownloadStatus.FAILED -> {
                                Text(
                                    text = stringResource(R.string.downloads_status_failed),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            DownloadStatus.COMPLETED -> {
                                Text(
                                    text = stringResource(R.string.downloads_status_completed),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                },
                leadingContent = {
                    MeloImage(
                        imageUrl = item.coverUrl.ifBlank { null },
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(dimensions.iconLg)
                            .clip(MaterialTheme.shapes.small),
                    )
                },
                trailingContent = {
                    when (item.status) {
                        DownloadStatus.FAILED -> {
                            MeloButton(
                                text = stringResource(R.string.downloads_retry),
                                onClick = onRetry,
                                variant = MeloButtonVariant.Outlined,
                            )
                        }
                        DownloadStatus.COMPLETED -> {
                            MeloButton(
                                text = stringResource(R.string.downloads_play),
                                onClick = onClick,
                            )
                        }
                        else -> Unit
                    }
                },
            )
            if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PENDING) {
                LinearProgressIndicator(
                    progress = { item.progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md),
                )
            }
        }
    }
}
