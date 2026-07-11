package com.melonet.app.core.common

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {

    fun applyLocale(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun getLayoutDirection(languageCode: String): Int {
        return when (languageCode) {
            "fa", "ar" -> Configuration.SCREENLAYOUT_LAYOUTDIR_RTL
            else -> Configuration.SCREENLAYOUT_LAYOUTDIR_LTR
        }
    }
}
