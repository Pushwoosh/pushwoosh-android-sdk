package com.pushwoosh.inapp.ui.presentation

import android.content.Context
import com.pushwoosh.inapp.ui.InAppModule
import com.pushwoosh.inapp.ui.activity.InAppOverlayActivity
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.internal.utils.PWLog

/**
 * Presentation channel for blocking templates: launches the shared translucent
 * [InAppOverlayActivity], handing it the message's raw JSON (re-parsed on the other
 * side so the hand-off survives process death / configuration changes).
 */
internal class InAppActivityChannel(private val context: Context) : InAppPresentationChannel {

    override fun present(message: InAppMessage) {
        try {
            context.startActivity(InAppOverlayActivity.intent(context, message.rawJson))
        } catch (e: Throwable) {
            // If the launch fails the Activity never reaches onDestroy, so the queue's
            // `showing` slot would stay set and stall every later message. Release it here.
            // Throwable, not Exception, so an Error strands the single slot no more than an
            // Exception does — the overlay channel releases on the same widened catch.
            PWLog.error(TAG, "Failed to present in-app overlay activity", e)
            InAppModule.queueManager(context).onDismissed(message.id)
        }
    }

    private companion object {
        const val TAG = "InAppActivityChannel"
    }
}
