package com.melonet.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** Thin wrapper so scrub / like / tab / send share one haptic language. */
class MeloHaptics(private val haptics: HapticFeedback) {
    fun tick() = haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun select() = haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    fun confirm() = haptics.performHapticFeedback(HapticFeedbackType.LongPress)
}

@Composable
fun rememberMeloHaptics(): MeloHaptics {
    val haptics = LocalHapticFeedback.current
    return remember(haptics) { MeloHaptics(haptics) }
}
