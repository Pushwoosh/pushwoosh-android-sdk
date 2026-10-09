package com.pushwoosh.demoapp

import com.google.firebase.messaging.RemoteMessage
import com.pushwoosh.firebase.PushwooshFcmRouterService
import com.pushwoosh.internal.utils.PWLog

/**
 * The demo's FCM entry point. It wins the MESSAGING_EVENT resolve over the SDK's own service, so
 * every push the demo receives walks through [PushwooshFcmRouterService] — the integration path a
 * host app with its own FCM routing is expected to use.
 */
class DemoFcmRouterService : PushwooshFcmRouterService() {
    override fun onAppMessageReceived(message: RemoteMessage) {
        PWLog.info(TAG, "Non-Pushwoosh FCM message from ${message.from}: ${message.data}")
    }

    override fun onAppTokenRefresh(token: String) {
        PWLog.info(TAG, "FCM token refreshed, already handed to Pushwoosh")
    }

    companion object {
        private const val TAG = "DemoFcmRouterService"
    }
}
