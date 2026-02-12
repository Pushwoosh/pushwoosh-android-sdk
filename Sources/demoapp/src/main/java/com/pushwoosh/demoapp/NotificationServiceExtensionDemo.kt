package com.pushwoosh.demoapp

import android.os.Handler
import android.util.Log
import androidx.annotation.MainThread
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.notification.NotificationServiceExtension
import com.pushwoosh.notification.PushMessage

class NotificationServiceExtensionDemo : NotificationServiceExtension() {
    public override fun onMessageReceived(message: PushMessage): Boolean {
        super.onMessageReceived(message)
        PWLog.debug(TAG, "PushMessage received: " + message.toJson().toString())
        return false
    }

    override fun startActivityForPushMessage(message: PushMessage) {
        super.startActivityForPushMessage(message)
        handlePush(message)
    }

    @MainThread
    private fun handlePush(message: PushMessage) {
        PWLog.debug(TAG, "PushMessage accepted: " + message.toJson().toString())
    }

    companion object {
        private const val TAG = "NotificationServiceExtensionDemo"
    }
}
