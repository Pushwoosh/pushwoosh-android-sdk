package com.pushwoosh.demoapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.pushwoosh.demoapp.utils.ThemePrefs

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Reapplied on cold start so a push-launched process keeps the forced theme.
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.load(this))
    }
}
