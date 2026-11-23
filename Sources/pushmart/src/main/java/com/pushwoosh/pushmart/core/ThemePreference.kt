package com.pushwoosh.pushmart.core

import androidx.appcompat.app.AppCompatDelegate

enum class ThemePreference(val raw: String, val nightMode: Int) {
    SYSTEM("system", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    LIGHT("light", AppCompatDelegate.MODE_NIGHT_NO),
    DARK("dark", AppCompatDelegate.MODE_NIGHT_YES);

    companion object {
        const val KEY = "pushmart.theme"

        fun from(raw: String): ThemePreference = values().firstOrNull { it.raw == raw } ?: SYSTEM
    }
}

class ThemeStore(private val prefs: KeyValueStore) {
    var preference: ThemePreference
        get() = ThemePreference.from(prefs.getString(ThemePreference.KEY))
        set(value) = prefs.putString(ThemePreference.KEY, value.raw)

    fun apply() {
        AppCompatDelegate.setDefaultNightMode(preference.nightMode)
    }
}
