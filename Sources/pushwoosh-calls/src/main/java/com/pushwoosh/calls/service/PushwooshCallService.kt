package com.pushwoosh.calls.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.pushwoosh.calls.util.Constants
import com.pushwoosh.calls.util.PushwooshCallUtils
import com.pushwoosh.internal.utils.PWLog

class PushwooshCallService : Service() {
    companion object {
        private const val TAG = "PushwooshCallService"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        PWLog.noise(TAG, "onStartCommand() action=${intent?.action}")
        val notification : Notification
        if (Constants.PW_POST_INCOMING_CALL_ACTION == intent?.action) {
            PWLog.debug(TAG, "Building INCOMING call notification")
            notification = PushwooshCallUtils.buildIncomingCallNotification(intent.extras)
            startForegroundNotification(notification, Constants.PW_NOTIFICATION_ID_INCOMING)
            PWLog.info(TAG, "INCOMING call notification shown (id=${Constants.PW_NOTIFICATION_ID_INCOMING})")
        } else if (Constants.PW_POST_ONGOING_CALL_ACTION == intent?.action) {
            PWLog.debug(TAG, "Building ONGOING call notification")
            notification = PushwooshCallUtils.buildOngoingCallNotification(intent.extras)
            startForegroundNotification(notification, Constants.PW_NOTIFICATION_ID_ONGOING)
            PWLog.info(TAG, "ONGOING call notification shown (id=${Constants.PW_NOTIFICATION_ID_ONGOING})")
        } else {
            PWLog.warn(TAG, "Unknown action: ${intent?.action}, not showing notification")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopAndRemoveNotification()
        PWLog.debug(TAG, "Service destroyed, notification removed")
    }

    private fun startForegroundNotification(notification: Notification, id: Int) {
        PWLog.noise(TAG, "startForegroundNotification() id=$id, API=${Build.VERSION.SDK_INT}")
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL)
            PWLog.debug(TAG, "Called startForeground() with FOREGROUND_SERVICE_TYPE_PHONE_CALL")
        } else {
            startForeground(Constants.PW_NOTIFICATION_ID_INCOMING, notification)
            PWLog.debug(TAG, "Called startForeground() (legacy)")
        }
    }

    private fun stopAndRemoveNotification() {
        PWLog.noise(TAG, "stopAndRemoveNotification()")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
}
