package com.pushwoosh.inapp.ui

import androidx.annotation.VisibleForTesting
import com.pushwoosh.PushwooshPlatform
import com.pushwoosh.inapp.network.model.Resource
import com.pushwoosh.inapp.view.InAppViewEvent
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.repository.RepositoryModule

/**
 * Maps a routed native in-app back to its originating ZIP [Resource] so show/click/close
 * analytics fire on the actual display, not on route acceptance (frequency caps, pause or
 * the host delegate may still suppress the show after present() returned true). Keyed by
 * the raw config JSON — the only identifier that survives the Intent round-trip to
 * InAppOverlayActivity. The push message hash is captured at registration because the
 * InAppRepository show-subscriber nulls the global value right after the show request.
 * Entries of never-shown messages are evicted by size.
 */
internal object NativeInAppAnalytics {
    private const val TAG = "NativeInAppAnalytics"
    private const val MAX_PENDING = 8
    private const val ACTION_TYPE_CLICK = 1
    private const val ACTION_TYPE_CLOSE = 4

    private data class Pending(val resource: Resource, val messageHash: String?)

    private val pending = object : LinkedHashMap<String, Pending>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pending>): Boolean =
            size > MAX_PENDING
    }

    @Synchronized
    fun register(rawJson: String, resource: Resource) {
        val messageHash = RepositoryModule.getNotificationPreferences()?.messageHash()?.get()
        pending[rawJson] = Pending(resource, messageHash)
    }

    fun onShown(rawJson: String?) {
        val entry = peek(rawJson) ?: return
        EventBus.sendEvent(InAppViewEvent(entry.resource))
    }

    fun onClicked(rawJson: String?) {
        // peek, not remove: the click is followed by a dismiss that must still reach onClosed.
        val entry = peek(rawJson) ?: return
        sendRichMediaAction(entry, ACTION_TYPE_CLICK)
    }

    fun onClosed(rawJson: String?) {
        val entry = remove(rawJson) ?: return
        sendRichMediaAction(entry, ACTION_TYPE_CLOSE)
    }

    /** Fire-and-forget /richMediaAction, code mapping as in PushwooshJSInterface.onPageStarted. */
    private fun sendRichMediaAction(entry: Pending, actionType: Int) {
        try {
            val code = entry.resource.code
            val richMediaCode = if (entry.resource.isInApp) "" else code.substring(2)
            val inAppCode = if (entry.resource.isInApp) code else ""
            PushwooshPlatform.getInstance()
                .pushwooshInApp()
                .sendRichMediaAction(richMediaCode, inAppCode, entry.messageHash, null, actionType, null)
        } catch (e: Throwable) {
            // Throwable, not Exception: this runs on the main thread from a user tap/dismiss;
            // a linkage Error (mixed core/module versions in a Maven consumer) must not crash the app.
            PWLog.error(TAG, "failed to send /richMediaAction request", e)
        }
    }

    @VisibleForTesting
    @Synchronized
    fun reset() {
        pending.clear()
    }

    @Synchronized
    private fun peek(rawJson: String?): Pending? = rawJson?.let { pending[it] }

    @Synchronized
    private fun remove(rawJson: String?): Pending? = rawJson?.let { pending.remove(it) }
}
