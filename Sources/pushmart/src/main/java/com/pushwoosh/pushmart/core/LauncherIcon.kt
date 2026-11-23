package com.pushwoosh.pushmart.core

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.pushwoosh.internal.utils.PWLog

/**
 * PushOn's changeIcon: switches the launcher icon between the manifest activity-aliases, as iOS
 * `setAlternateIconName` does. `xmas_sale` is the sale icon, `"nil"` restores the main one.
 *
 * Disabling the alias that launched a visible task can close the app, so a requested switch waits
 * until no activity is started and is applied from [setForeground]. iOS defers too, until the app
 * is active. Both methods run on the main thread.
 */
class LauncherIcon(context: Context) {
    private val context = context.applicationContext
    private val aliases = ALIAS_NAMES.mapValues { "${this.context.packageName}.${it.value}" }
    private var foreground = false
    private var pending: String? = null

    fun request(value: String) {
        val target = aliases[value]
        if (target == null) {
            PWLog.warn(TAG, "Change icon failed: unknown icon $value")
            return
        }
        pending = target
        if (!foreground) applyPending()
    }

    fun setForeground(foreground: Boolean) {
        this.foreground = foreground
        if (!foreground) applyPending()
    }

    private fun applyPending() {
        val target = pending ?: return
        val packageManager = context.packageManager
        packageManager.setComponentEnabledSetting(
            ComponentName(context, target),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP)
        aliases.values
            .filter { it != target }
            .forEach {
                packageManager.setComponentEnabledSetting(
                    ComponentName(context, it),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP)
            }
        pending = null
        PWLog.debug(TAG, "launcher icon: $target")
    }

    companion object {
        private const val TAG = "PushMartLauncherIcon"
        private val ALIAS_NAMES =
            mapOf("xmas_sale" to "LauncherXmasSale", "nil" to "LauncherDefault")
    }
}
