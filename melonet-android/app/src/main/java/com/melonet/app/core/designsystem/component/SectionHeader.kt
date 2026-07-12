package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.melonet.app.R
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val spacing = MeloNetTheme.spacing
    val label = actionLabel ?: stringResource(R.string.action_see_all)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (onActionClick != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onActionClick),
            )
        }
    }
}

@Composable
fun SectionHeaderShimmer(modifier: Modifier = Modifier) {
    val spacing = MeloNetTheme.spacing
    val dimensions = MeloNetTheme.dimensions

    Box(
        modifier = modifier
            .padding(horizontal = spacing.md, vertical = spacing.sm)
            .height(dimensions.shimmerTitleHeight)
            .fillMaxWidth(dimensions.shimmerWidthFractionSm)
    ) {
        ShimmerBox(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraSmall,
        )
    }
}
