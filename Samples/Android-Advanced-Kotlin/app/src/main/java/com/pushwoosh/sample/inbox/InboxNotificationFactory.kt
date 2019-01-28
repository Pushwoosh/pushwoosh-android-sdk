package com.pushwoosh.sample.inbox

import android.app.Notification
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.pushwoosh.notification.PushMessage
import com.pushwoosh.notification.PushwooshNotificationFactory

class InboxNotificationFactory : PushwooshNotificationFactory() {
    companion object {
        private const val GROUP_KEY = "PushwooshGroup"
    }

    private fun generateInboxStyle(pushData: PushMessage): NotificationCompat.Style? {
        if (applicationContext == null) {
            return null
        }

        MessageStorage.addMessage(applicationContext!!, Message(pushData.message, System.currentTimeMillis(), pushData.header))
        val messages = MessageStorage.getHistory(applicationContext!!)

        messages?. run {
            val style = NotificationCompat.InboxStyle().setBigContentTitle(pushData.header + " Details")

            for (message in messages) {
                style.addLine(getContentFromHtml("<b>" + message.sender + "</b> " + message.text))
            }

            if (messages.size > 7) {
                style.setSummaryText("+ " + (messages.size - 7) + " more")
            }

            return style
        }

        return null
    }

    private fun generateSummary(pushData: PushMessage) {
        val notificationBuilder = NotificationCompat.Builder(applicationContext)
                .setContentTitle(getContentFromHtml(pushData.header))
                .setContentText(getContentFromHtml(pushData.message))
                .setSmallIcon(pushData.smallIcon)
                .setTicker(getContentFromHtml(pushData.ticker))
                .setWhen(System.currentTimeMillis())
                .setStyle(generateInboxStyle(pushData))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getActivity(applicationContext, 0, getNotificationIntent(pushData), PendingIntent.FLAG_CANCEL_CURRENT))

        val notificationManager = NotificationManagerCompat.from(applicationContext!!)
        notificationManager.notify(GROUP_KEY, 0, notificationBuilder.build())
    }

    override fun onGenerateNotification(pushData: PushMessage): Notification? {
        // Summary will be displayed only on devices that do not support grouped notifications
        generateSummary(pushData)

        // This notification will be displayed only on devices that support grouped notifications
        val notification = NotificationCompat.Builder(applicationContext)
                .setContentTitle(getContentFromHtml(pushData.header))
                .setContentText(getContentFromHtml(pushData.message))
                .setSmallIcon(pushData.smallIcon)
                .setTicker(getContentFromHtml(pushData.ticker))
                .setWhen(System.currentTimeMillis())
                .setGroup(GROUP_KEY)
                .setAutoCancel(true)
                .build()

        addSound(notification, pushData.sound)

        return notification
    }

}
