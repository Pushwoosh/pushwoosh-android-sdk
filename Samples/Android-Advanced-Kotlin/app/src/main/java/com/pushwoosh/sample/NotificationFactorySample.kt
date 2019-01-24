package com.pushwoosh.sample

import android.app.Notification
import android.graphics.Bitmap
import android.util.Log

import com.pushwoosh.notification.PushMessage
import com.pushwoosh.notification.PushwooshNotificationFactory

class NotificationFactorySample : PushwooshNotificationFactory() {
    override fun onGenerateNotification(pushMessage: PushMessage): Notification? {
        Log.d(PushwooshSampleApp.LTAG, "onGenerateNotification: " + pushMessage.toJson().toString())

        val notification = super.onGenerateNotification(pushMessage)
        // TODO: customise notification content

        return notification
    }

    override fun getLargeIcon(pushMessage: PushMessage): Bitmap? {
        // TODO: set custom large icon for notification

        return super.getLargeIcon(pushMessage)
    }
}
