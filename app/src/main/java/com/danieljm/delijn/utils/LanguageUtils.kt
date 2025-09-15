package com.danieljm.delijn.utils

import android.content.Context
import java.util.Locale

object LanguageUtils {
    fun setLocale(context: Context, locale: Locale) {
        val resources = context.resources
        val configuration = resources.configuration
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    fun currentLocale(context: Context): Locale = context.resources.configuration.locales[0]
}

