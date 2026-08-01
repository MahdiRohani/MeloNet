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

    val fadeTween = tween<Float>(durationMillis = mediumMs, easing = FastOutSlowInEasing)
    val slideTween = tween<IntOffset>(durationMillis = mediumMs, easing = FastOutSlowInEasing)
    val pressSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium,
    )
}
