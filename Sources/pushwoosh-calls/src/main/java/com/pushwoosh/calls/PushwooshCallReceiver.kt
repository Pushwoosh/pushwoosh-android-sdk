package com.pushwoosh.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.pushwoosh.calls.service.PushwooshCallService
import com.pushwoosh.calls.service.PushwooshConnectionService
import com.pushwoosh.calls.util.Constants
import com.pushwoosh.internal.utils.PWLog

class PushwooshCallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PushwooshCallReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent) {
        PWLog.noise(TAG, "onReceive() action=${intent.action}")
        val extras = intent.extras
        when (intent.action) {
            "ACTION_ACCEPT_CALL" -> {
                PWLog.noise(TAG, "handleActionAcceptCall()")
                PWLog.debug(TAG, "CALL ACCEPTED")
                //stop service for incoming call and cancel notification
                val stopIntent = Intent(context, PushwooshCallService::class.java)
                if (extras != null) {
                    stopIntent.putExtras(extras)
                }
                context?.stopService(stopIntent)
                PWLog.debug(TAG, "Stopped incoming call service")
                //start a service for ongoing call and create ongoing call notification
                val ongoingCallIntent = Intent(context, PushwooshCallService::class.java).apply {
                    action = Constants.PW_POST_ONGOING_CALL_ACTION
                }
                if (extras != null) {
                    ongoingCallIntent.putExtras(extras)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context?.startForegroundService(ongoingCallIntent)
                    PWLog.debug(TAG, "Started ongoing call foreground service (API ${Build.VERSION.SDK_INT})")
                } else {
                    context?.startService(ongoingCallIntent)
                    PWLog.debug(TAG, "Started ongoing call service (legacy, API ${Build.VERSION.SDK_INT})")
                }

                // Delegate Connection lifecycle to Connection method
                // Connection handles: callEventListener.onAnswer() + setActive()
                val connection = PushwooshConnectionService.getActiveConnection()
                if (connection != null) {
                    connection.onAnswer(1)
                    PWLog.debug(TAG, "Delegated to Connection.onAnswer()")
                } else {
                    PWLog.warn(TAG, "Cannot answer: activeConnection is null")
                }
            }
            "ACTION_REJECT_CALL" -> {
                PWLog.noise(TAG, "handleActionRejectCall()")
                PWLog.debug(TAG, "CALL DECLINED")
                //stop service for incoming call and cancel notification
                val stopIntent = Intent(context, PushwooshCallService::class.java)
                if (extras != null) {
                    stopIntent.putExtras(extras)
                }
                context?.stopService(stopIntent)
                PWLog.debug(TAG, "Stopped incoming call service")

                // Delegate Connection lifecycle to Connection method
                // Connection handles: callEventListener.onReject() + setDisconnected() + destroy()
                val connection = PushwooshConnectionService.getActiveConnection()
                if (connection != null) {
                    connection.onReject()
                    PushwooshConnectionService.clearActiveConnectionIfEquals(connection)
                    PWLog.debug(TAG, "Delegated to Connection.onReject() and cleared connection")
                } else {
                    PWLog.warn(TAG, "Cannot reject: activeConnection is null")
                }
            }
            "ACTION_END_CALL" -> {
                PWLog.noise(TAG, "handleActionEndCall()")
                PWLog.debug(TAG, "CALL ENDED")
                //stop service for incoming call and cancel notification
                val stopIntent = Intent(context, PushwooshCallService::class.java)
                if (extras != null) {
                    stopIntent.putExtras(extras)
                }
                context?.stopService(stopIntent)
                PWLog.debug(TAG, "Stopped ongoing call service")

                // Delegate Connection lifecycle to Connection method
                // Connection handles: callEventListener.onDisconnect() + setDisconnected() + destroy()
                val connection = PushwooshConnectionService.getActiveConnection()
                if (connection != null) {
                    connection.onDisconnect()
                    PushwooshConnectionService.clearActiveConnectionIfEquals(connection)
                    PWLog.debug(TAG, "Delegated to Connection.onDisconnect() and cleared connection")
                } else {
                    PWLog.warn(TAG, "Cannot disconnect: activeConnection is null")
                }
            }
        }
    }
}
