package com.pushwoosh.pushmart.core

import com.pushwoosh.pushmart.sdk.PushwooshGateway

/**
 * App settings > Show alerts in-app. The SDK has no getter for its foreground-alert flag, so the
 * app keeps the toggle's state itself.
 */
class ForegroundAlerts(private val prefs: KeyValueStore, private val gateway: PushwooshGateway) {
    val enabled: Boolean
        get() = prefs.getBoolean(KEY, true)

    fun set(on: Boolean) {
        prefs.putBoolean(KEY, on)
        gateway.setShowForegroundAlerts(on)
    }

    companion object {
        const val KEY = "pushmart.showForegroundAlerts"
    }
}
