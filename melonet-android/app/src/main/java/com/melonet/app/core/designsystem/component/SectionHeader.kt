package com.melonet.app.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.melonet.app.core.designsystem.theme.MeloNetTheme

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    val spacing = MeloNetTheme.spacing

    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(
            horizontal = spacing.md,
            vertical = spacing.sm
        )
    )
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
