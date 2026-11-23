package com.pushwoosh.demoapp

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.pushwoosh.demoapp.ui.inline.InlineQa
import com.pushwoosh.demoapp.utils.ThemePrefs
import com.pushwoosh.inline.PushwooshInline

class DemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Reapplied on cold start so a push-launched process keeps the forced theme.
        AppCompatDelegate.setDefaultNightMode(ThemePrefs.load(this))
        // Set here so callbacks that fire before the Inline screen opens are captured.
        PushwooshInline.listener = InlineQa.listener
    }
}
