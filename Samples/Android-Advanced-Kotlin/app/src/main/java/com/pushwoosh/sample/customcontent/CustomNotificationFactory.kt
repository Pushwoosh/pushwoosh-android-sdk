package com.pushwoosh.sample.customcontent

import android.annotation.SuppressLint
import android.app.Notification
import android.widget.RemoteViews
import com.pushwoosh.notification.PushMessage
import com.pushwoosh.notification.PushwooshNotificationFactory
import com.pushwoosh.sample.R
import com.pushwoosh.sample.customcontent.builder.NotificationBuilderManager
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SimpleDateFormat")
val dateFormat = SimpleDateFormat("hh:mma")

class CustomNotificationFactory : PushwooshNotificationFactory() {

    private fun buildContentView(pushData: PushMessage): RemoteViews {
        val context = applicationContext
        val contentView = RemoteViews(context?.packageName, R.layout.notification)

        contentView.setImageViewBitmap(R.id.image, getLargeIcon(pushData))

        contentView.setTextViewText(R.id.messageText, pushData.message)
        contentView.setTextViewText(R.id.dateText, dateFormat.format(Date()))

        return contentView
    }

    override fun onGenerateNotification(pushData: PushMessage): Notification? {

        val channelId = addChannel(pushData)
        if (applicationContext == null) {
            return null
        }

        val notificationBuilder = NotificationBuilderManager.createNotificationBuilder(applicationContext!!, channelId)
        notificationBuilder.setContentTitle(getContentFromHtml(pushData.header))
                .setContentText(getContentFromHtml(pushData.message))

                .setSmallIcon(pushData.smallIcon)

                .setColor(pushData.iconBackgroundColor)

                .setPriority(pushData.priority)
                .setVisibility(pushData.visibility)

                .setTicker(getContentFromHtml(pushData.ticker))
                .setWhen(System.currentTimeMillis())
                .setCustomContentView(buildContentView(pushData))

        val notification = notificationBuilder.build()

        addLED(notification, pushData.led, pushData.ledOnMS, pushData.ledOffMS)
        addSound(notification, pushData.sound)
        addVibration(notification, pushData.vibration)
        addCancel(notification)

        return notification
    }
}