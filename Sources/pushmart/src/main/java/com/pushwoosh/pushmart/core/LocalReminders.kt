package com.pushwoosh.pushmart.core

import com.pushwoosh.pushmart.account.AccountManager
import com.pushwoosh.pushmart.sdk.PushwooshGateway

/**
 * Local reminders, as iOS `Notifications.showLocalNotification`: a shopper not asked about
 * notifications yet gets the Push Primer first, and the reminder is scheduled once push is on.
 */
class LocalReminders(
    private val gateway: PushwooshGateway,
    private val permission: PushPermission,
    private val account: AccountManager,
) {
    fun schedule(title: String, body: String, delaySeconds: Int) {
        if (!permission.isUndetermined) {
            gateway.scheduleLocalNotification(title, body, delaySeconds)
            return
        }
        account.setPushEnabled(true) { on ->
            if (on) gateway.scheduleLocalNotification(title, body, delaySeconds)
        }
    }
}
