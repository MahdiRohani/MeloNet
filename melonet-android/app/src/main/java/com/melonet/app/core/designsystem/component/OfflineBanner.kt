package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloNetTheme
import com.melonet.app.data.model.ChatConnectionState

@Composable
fun OfflineBanner(
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(vertical = spacing.sm, horizontal = spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.offline_banner_message),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ChatConnectionBanner(
    state: ChatConnectionState,
    modifier: Modifier = Modifier,
    onRetryConnect: (() -> Unit)? = null,
) {
    if (state == ChatConnectionState.Connected) return

    val spacing = MeloNetTheme.spacing
    val (background, foreground, labelRes) = when (state) {
        ChatConnectionState.Offline -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            R.string.chat_status_offline,
        )
        ChatConnectionState.Reconnecting -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            R.string.chat_status_reconnecting,
        )
        ChatConnectionState.Connected -> return
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .then(
                if (onRetryConnect != null && state == ChatConnectionState.Offline) {
                    Modifier.clickable(onClick = onRetryConnect)
                } else {
                    Modifier
                },
            )
            .padding(vertical = spacing.sm, horizontal = spacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = foreground,
            textAlign = TextAlign.Center,
        )
    }
}
