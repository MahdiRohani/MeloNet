package com.melonet.app.core.designsystem

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.melonet.app.core.common.LocaleManager

@Composable
fun ProvideAppLocale(
    language: String,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val localizedContext = remember(language) {
        LocaleManager.applyLocale(context, language)
    }
    val configuration = localizedContext.resources.configuration
    val layoutDirection = when (language) {
        "fa", "ar" -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides Configuration(configuration),
        LocalLayoutDirection provides layoutDirection,
        content = content
    )
}
