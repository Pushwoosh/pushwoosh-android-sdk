package com.pushwoosh.sample

import android.os.Handler
import android.support.annotation.MainThread
import android.util.Log

import com.pushwoosh.notification.NotificationServiceExtension
import com.pushwoosh.notification.PushMessage

class NotificationServiceSample : NotificationServiceExtension() {
    public override fun onMessageReceived(message: PushMessage?): Boolean {
        Log.d(PushwooshSampleApp.LTAG, "NotificationService.onMessageReceived: " + message?.toJson().toString())

        // automatic foreground push handling
        if (isAppOnForeground) {
            val mainHandler = Handler(applicationContext?.mainLooper)
            mainHandler.post { handlePush(message) }

            // this indicates that notification should not be displayed
            return true
        }

        return false
    }

    override fun onMessagesGroupOpened(pushMessagesList: MutableList<PushMessage>?) {
        Log.d(PushwooshSampleApp.LTAG, "NotificationService.onMessagesGroupOpened with: " + pushMessagesList!!.size + " messages")

        // TODO: handle push messages group
    }

    override fun startActivityForPushMessage(message: PushMessage) {
        super.startActivityForPushMessage(message)

        // TODO: start custom activity if necessary

        handlePush(message)
    }

    @MainThread
    private fun handlePush(message: PushMessage?) {
        Log.d(PushwooshSampleApp.LTAG, "NotificationService.handlePush: " + message?.toJson().toString())
        // TODO: handle push message
    }
}
