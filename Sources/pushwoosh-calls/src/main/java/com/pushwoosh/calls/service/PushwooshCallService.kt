package com.pushwoosh.calls.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.pushwoosh.calls.util.Constants
import com.pushwoosh.calls.util.PushwooshCallUtils

class PushwooshCallService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification : Notification
        if (Constants.PW_POST_INCOMING_CALL_ACTION == intent?.action) {
            notification = PushwooshCallUtils.buildIncomingCallNotification(intent.extras)
            startForegroundNotification(notification, Constants.PW_NOTIFICATION_ID_INCOMING)
        } else if (Constants.PW_POST_ONGOING_CALL_ACTION == intent?.action) {
            notification = PushwooshCallUtils.buildOngoingCallNotification(intent.extras)
            startForegroundNotification(notification, Constants.PW_NOTIFICATION_ID_ONGOING)
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopAndRemoveNotification()
    }

    private fun startForegroundNotification(notification: Notification, id: Int) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
        } else {
            startForeground(Constants.PW_NOTIFICATION_ID_INCOMING, notification)
        }
    }

    private fun stopAndRemoveNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}