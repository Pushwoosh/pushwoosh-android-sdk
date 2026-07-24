package com.pushwoosh.inapp.ui

import com.pushwoosh.inapp.ui.activity.InAppOverlayActivity
import com.pushwoosh.inapp.ui.image.InAppImageLoader
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.model.imageURLs
import com.pushwoosh.inapp.ui.overlay.InAppOverlayController
import com.pushwoosh.inapp.ui.parser.InAppConfigParser
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.BackgroundExecutor

/**
 * Public entry point of the native in-app UI module, reached as `PushwooshInAppUi`.
 *
 * Production in-apps are presented from a ZIP resource carrying `native-config.json`, routed
 * in through the core `NativeInAppPresenter` contract. [present] is the manual / testing entry
 * point — pass a raw config JSON to preview a layout without a server round-trip.
 */
object PushwooshInAppUi {

    /**
     * Optional callbacks for lifecycle / click events and per-message suppression.
     *
     * Held weakly — keep a strong reference to your delegate (e.g. a field on your Activity or
     * Application). An anonymous delegate assigned inline is garbage-collected and its callbacks
     * silently stop.
     */
    @JvmStatic
    var delegate: InAppMessageDelegate?
        get() = InAppModule.delegate
        set(value) {
            InAppModule.delegate = value
        }

    /**
     * `true` while a native in-app is really on screen — a live overlay **or** a live overlay
     * Activity. A dropped background launch (slot claimed, nothing visible) reads `false`, as
     * does the brief gap between the launch and the Activity's `onCreate`.
     */
    @JvmStatic
    val isPresenting: Boolean
        get() = InAppOverlayController.isActive || InAppOverlayActivity.current != null

    /**
     * Dismisses whatever native in-app is currently visible — the decorView overlay or the
     * overlay Activity (only one is ever on screen: all templates share the single queue
     * slot). The queue is untouched; the next message shows normally via the existing
     * `onDismissed` → advance path. No-op when nothing is visible.
     */
    @JvmStatic
    fun dismiss() {
        BackgroundExecutor.main {
            InAppOverlayController.dismissActive()
            InAppOverlayActivity.current?.dismissFromApi()
        }
    }

    /** When true, eligible messages are queued but not displayed. Default false. */
    @JvmStatic
    var isPaused: Boolean
        get() = queueManager()?.isPaused ?: false
        set(value) {
            // Written synchronously on the caller's thread (iOS parity); the queue manager
            // marshals only the resume/advance to main.
            queueManager()?.isPaused = value
        }

    /** Enables opt-in frequency caps (max displays / cooldown / expiry). Off by default. */
    @JvmStatic
    fun setFrequencyCapEnabled(enabled: Boolean) {
        val context = AndroidPlatformModule.getApplicationContext() ?: return
        InAppModule.frequencyStore(context).enabled = enabled
    }

    /** Presents a native in-app from a raw config JSON string (manual / testing entry point). */
    @JvmStatic
    fun present(configJson: String) {
        val message = InAppConfigParser.parse(configJson) ?: return
        route(message)
    }

    /** Routes a parsed message: every template goes through the single FIFO queue, which
     *  dispatches to the Activity or the decorView overlay by layout via its routing channel. */
    internal fun route(message: InAppMessage) {
        BackgroundExecutor.main {
            val context = AndroidPlatformModule.getApplicationContext() ?: return@main
            // Warm every image (incl. off-screen slides) at the single show funnel before the
            // message is queued, so no path flickers on first paint.
            InAppImageLoader.prefetch(context, message.imageURLs())
            InAppModule.queueManager(context).enqueueForeground(message)
        }
    }

    private fun queueManager() =
        AndroidPlatformModule.getApplicationContext()?.let { InAppModule.queueManager(it) }
}
