package com.pushwoosh.inapp.ui

/**
 * Optional callbacks for the host app to observe and gate native in-app messages.
 * Every method has a default, so integrators override only what they need. All
 * callbacks are invoked on the main thread.
 */
interface InAppMessageDelegate {
    /** Return false to suppress this specific message before it is shown. */
    fun shouldDisplay(messageId: String?): Boolean = true

    fun willPresent(messageId: String?) {}

    fun didPresent(messageId: String?) {}

    fun didClose(messageId: String?) {}

    fun clickedAction(url: String, messageId: String?) {}
}
