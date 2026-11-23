package com.pushwoosh.inline.internal.view

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.WindowManager
import com.pushwoosh.inline.PushwooshInlineColorScheme
import com.pushwoosh.inline.internal.Cancelable

/**
 * Whether banners are drawn dark: [PushwooshInlineColorScheme.SYSTEM] follows the device's night
 * mode, [PushwooshInlineColorScheme.APP] the night mode of [context] (the view hosting the slot).
 */
internal fun isDark(scheme: PushwooshInlineColorScheme, context: Context): Boolean =
    when (scheme) {
        PushwooshInlineColorScheme.LIGHT -> false
        PushwooshInlineColorScheme.DARK -> true
        PushwooshInlineColorScheme.SYSTEM -> isNight(systemContext(context).resources.configuration)
        PushwooshInlineColorScheme.APP -> isNight(context.resources.configuration)
    }

private var deviceContext: Context? = null

internal fun observeSystemColorScheme(context: Context, onChange: () -> Unit): Cancelable? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val device = systemContext(context)
    val callbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) = onChange()

        override fun onLowMemory() = Unit
    }
    device.registerComponentCallbacks(callbacks)
    return Cancelable { device.unregisterComponentCallbacks(callbacks) }
}

private fun systemContext(context: Context): Context {
    val app = context.applicationContext
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return app
    // A window context follows its display area, without the application's night override.
    return deviceContext
        ?: app.createWindowContext(
                app.getSystemService(DisplayManager::class.java)
                    .getDisplay(Display.DEFAULT_DISPLAY),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                null,
            )
            .also { deviceContext = it }
}

private fun isNight(configuration: Configuration): Boolean =
    configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
