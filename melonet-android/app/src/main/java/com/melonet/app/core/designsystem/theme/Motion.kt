package com.melonet.app.core.designsystem.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/** Shared motion tokens for shell / list polish. */
object MeloMotion {
    const val shortMs = 180
    const val mediumMs = 280
    const val longMs = 420

    const val splashLogoFadeMs = 700
    const val splashLogoScaleMs = 900
    const val splashTitleMs = 650
    const val splashRingMs = 1100
    const val splashHoldMs = 1400
    const val splashTitleDelayMs = 220
    const val splashPulseMs = 1600
    const val splashWaveMs = 4200

    const val playerBgDriftAMs = 18_000
    const val playerBgDriftBMs = 24_000
    const val playerBgOrbitMs = 40_000

    const val coverPausedScale = 0.96f
    const val splashLogoStartScale = 0.78f

    val fadeTween = tween<Float>(durationMillis = mediumMs, easing = FastOutSlowInEasing)
    val slideTween = tween<IntOffset>(durationMillis = mediumMs, easing = FastOutSlowInEasing)
    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )
    val coverSpring = spring<Float>(
        dampingRatio = 0.85f,
        stiffness = 280f,
    )
}
