package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(dimensions.iconLg),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(spacing.md))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(spacing.md))
            MeloButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                variant = MeloButtonVariant.Outlined,
            )
        }
    }
}
