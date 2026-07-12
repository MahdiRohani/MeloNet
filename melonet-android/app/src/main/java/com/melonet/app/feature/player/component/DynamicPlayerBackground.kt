package com.melonet.app.feature.player.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun DynamicPlayerBackground(
    gradientColors: List<Long>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = if (gradientColors.size >= 2) {
        gradientColors.map { Color(it) }
    } else {
        listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = colors),
            ),
    ) {
        content()
    }
}
