package com.pushwoosh.demoapp

import android.os.Handler
import android.util.Log
import androidx.annotation.MainThread
import com.pushwoosh.notification.NotificationServiceExtension
import com.pushwoosh.notification.PushMessage

class NotificationServiceExtensionDemo : NotificationServiceExtension() {
    public override fun onMessageReceived(message: PushMessage): Boolean {
        super.onMessageReceived(message)
        Log.d(TAG, "PushMessage received: " + message.toJson().toString())

        if (isAppOnForeground && applicationContext != null) {
            val mainHandler = Handler(applicationContext!!.mainLooper)
            mainHandler.post { handlePush(message) }
            return true
        }

        return false
    }

    override fun startActivityForPushMessage(message: PushMessage) {
        super.startActivityForPushMessage(message)
        handlePush(message)
    }

    @MainThread
    private fun handlePush(message: PushMessage) {
        Log.d(TAG, "PushMessage accepted: " + message.toJson().toString())
    }

    companion object {
        private const val TAG = "NotificationServiceExtensionDemo"
    }
}