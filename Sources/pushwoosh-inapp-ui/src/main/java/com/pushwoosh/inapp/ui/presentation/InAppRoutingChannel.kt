package com.pushwoosh.inapp.ui.presentation

import com.pushwoosh.inapp.ui.model.InAppLayout
import com.pushwoosh.inapp.ui.model.InAppMessage

/**
 * The queue's single [InAppPresentationChannel]: it keeps the queue UI-free by dispatching
 * each claimed message to the mechanism its layout needs. Non-blocking templates (banner,
 * floating modal) go to the decorView overlay; every blocking template goes to the shared
 * Activity. The blocking/non-blocking decision ([isNonBlocking]) lives here, not in
 * `PushwooshInAppUi`, because it is a presentation concern.
 */
internal class InAppRoutingChannel(
    private val activityChannel: InAppPresentationChannel,
    private val overlayChannel: InAppPresentationChannel
) : InAppPresentationChannel {

    override fun present(message: InAppMessage) {
        if (isNonBlocking(message)) {
            overlayChannel.present(message)
        } else {
            activityChannel.present(message)
        }
    }

    private fun isNonBlocking(message: InAppMessage): Boolean = when (val layout = message.layout) {
        is InAppLayout.Banner -> true
        is InAppLayout.Modal -> !layout.content.dimsBackground
        else -> false
    }
}
