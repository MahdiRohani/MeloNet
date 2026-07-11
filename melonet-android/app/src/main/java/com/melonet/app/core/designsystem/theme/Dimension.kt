package com.melonet.app.core.designsystem.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    val iconSm: Dp = 24.dp,
    val iconMd: Dp = 32.dp,
    val iconLg: Dp = 40.dp,
    val avatarSm: Dp = 40.dp,
    val avatarMd: Dp = 80.dp,
    val avatarLg: Dp = 130.dp,
    val avatarRing: Dp = 140.dp,
    val avatarBorder: Dp = 4.dp,
    val songCardSize: Dp = 120.dp,
    val quickActionSize: Dp = 60.dp,
    val quickActionCardHeight: Dp = 100.dp,
    val carouselHeight: Dp = 180.dp,
    val miniPlayerHeight: Dp = 64.dp,
    val bottomBarHeight: Dp = 80.dp,
    val topBarHeight: Dp = 56.dp,
    val shimmerLabelWidth: Dp = 48.dp,
    val shimmerLabelHeight: Dp = 12.dp,
    val shimmerTitleHeight: Dp = 20.dp,
    val shimmerTextHeight: Dp = 14.dp,
    val shimmerSubtextHeight: Dp = 10.dp,
    val shimmerWidthFractionSm: Float = 0.4f,
    val shimmerWidthFractionMd: Float = 0.5f,
    val shimmerWidthFractionLg: Float = 0.8f,
)

val LocalDimensions = staticCompositionLocalOf { Dimensions() }
