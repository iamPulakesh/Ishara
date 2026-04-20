package com.isharaai.isl.feature.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Manages the user's language preference (English or Bengali).
 * Uses Android's per-app language API (AppCompat) for seamless switching.
 */
object LanguageManager {

    private const val PREF_NAME = "ishara_settings"
    private const val KEY_LANGUAGE = "app_language"

    const val LANG_ENGLISH = "en"
    const val LANG_BENGALI = "bn"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getCurrentLanguage(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE, LANG_BENGALI) ?: LANG_BENGALI

    fun setLanguage(context: Context, langCode: String) {
        prefs(context).edit().putString(KEY_LANGUAGE, langCode).apply()
        val localeList = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /** Call from Application.onCreate() to restore saved language */
    fun applyStoredLanguage(context: Context) {
        val lang = getCurrentLanguage(context)
        val localeList = LocaleListCompat.forLanguageTags(lang)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
