package com.pushwoosh.pushmart

import android.content.Intent
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.notification.NotificationServiceExtension
import com.pushwoosh.notification.PushMessage
import com.pushwoosh.pushmart.routing.PushRouting
import com.pushwoosh.pushmart.routing.RouteExtras

class PushMartNotificationServiceExtension : NotificationServiceExtension() {
    override fun startActivityForPushMessage(message: PushMessage) {
        val route = PushRouting.routeFor(message)
        val context = applicationContext
        PWLog.debug(TAG, "push opened, custom data: " + message.customData + ", route: " + route)
        if (route == null || context == null) {
            super.startActivityForPushMessage(message)
            return
        }
        val intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        context.startActivity(RouteExtras.put(intent, route))
    }

    companion object {
        private const val TAG = "PushMartNotificationServiceExtension"
    }
}
