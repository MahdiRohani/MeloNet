package com.melonet.app.core.designsystem

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
    val layoutDirection = when (language) {
        "fa", "ar" -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }

    // Overriding LocalContext with a config-wrapped context hides the Activity, so
    // owners that resolve through LocalContext (e.g. ActivityResultRegistryOwner) must
    // be provided explicitly from the original Activity to keep permission launchers working.
    val activityResultRegistryOwner = remember(context) {
        context.findActivityResultRegistryOwner()
    }

    val providedValues = buildList {
        add(LocalContext provides localizedContext)
        add(LocalLayoutDirection provides layoutDirection)
        if (activityResultRegistryOwner != null) {
            add(LocalActivityResultRegistryOwner provides activityResultRegistryOwner)
        }
    }.toTypedArray()

    CompositionLocalProvider(
        values = providedValues,
        content = content,
    )
}

private fun Context.findActivityResultRegistryOwner(): ActivityResultRegistryOwner? {
    var current: Context? = this
    while (current != null) {
        if (current is ActivityResultRegistryOwner) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
