package com.pushwoosh.pushmart.sdk

import com.pushwoosh.inapp.ui.InAppMessageDelegate
import com.pushwoosh.internal.utils.PWLog

/**
 * Logs every native in-app callback; e2e runs grep these lines to tell "shown" from "parser dropped
 * it".
 */
class LoggingInAppDelegate : InAppMessageDelegate {
    override fun shouldDisplay(messageId: String?): Boolean {
        PWLog.debug(TAG, "shouldDisplay $messageId")
        return true
    }

    override fun willPresent(messageId: String?) {
        PWLog.debug(TAG, "willPresent $messageId")
    }

    override fun didPresent(messageId: String?) {
        PWLog.debug(TAG, "didPresent $messageId")
    }

    override fun didClose(messageId: String?) {
        PWLog.debug(TAG, "didClose $messageId")
    }

    override fun clickedAction(url: String, messageId: String?) {
        PWLog.debug(TAG, "clickedAction $messageId url=$url")
    }

    companion object {
        const val TAG = "PushMartInApp"
    }
}
