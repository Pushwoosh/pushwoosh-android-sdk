package com.pushwoosh.demoapp.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePrefs {
    private const val PREFS = "demo_theme_prefs"
    private const val KEY_NIGHT_MODE = "night_mode"

    @JvmStatic
    fun save(context: Context, mode: Int) {
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_NIGHT_MODE, mode)
            .apply()
    }

    @JvmStatic
    fun load(context: Context): Int =
        context
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
}
