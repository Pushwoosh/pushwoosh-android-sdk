package com.pushwoosh.inapp.ui

import com.pushwoosh.inapp.event.ActivityBroughtOnTopEvent
import com.pushwoosh.inapp.nativeui.NativeInAppPresenterProvider
import com.pushwoosh.internal.Plugin
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.platform.ApplicationOpenDetector
import com.pushwoosh.internal.platform.prefs.PrefsProvider
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme
import com.pushwoosh.internal.utils.PWLog

/**
 * Auto-discovered [Plugin] that wires the native in-app UI module into the SDK. Discovered
 * at startup via the manifest meta-data {@code com.pushwoosh.plugin.inapp_ui}.
 *
 * Registers the native in-app presenter with core and subscribes to two lifecycle events:
 * the foreground event resumes the blocking queue (dropped-launch recovery + advance) for a
 * ZIP resource deferred while backgrounded; the Activity-resume event retries an overlay
 * present deferred while `topActivity` was null (screen handoff, permission dialog, PiP).
 */
class InAppUiPlugin : Plugin {

    override fun init() {
        NativeInAppPresenterProvider.set(NativeInAppPresenterImpl())

        EventBus.subscribe(ApplicationOpenDetector.ApplicationMovedToForegroundEvent::class.java) {
            AndroidPlatformModule.getApplicationContext()?.let {
                InAppModule.queueManager(it).onAppForegrounded()
            }
        }
        EventBus.subscribe(ActivityBroughtOnTopEvent::class.java) {
            AndroidPlatformModule.getApplicationContext()?.let {
                InAppModule.queueManager(it).onActivityBroughtOnTop()
            }
        }
        PWLog.info(TAG, "InAppUiPlugin initialized")
    }

    override fun getPrefsMigrationSchemes(prefsProvider: PrefsProvider): Collection<MigrationScheme> =
        emptyList()

    companion object {
        private const val TAG = "InAppUiPlugin"
    }
}
