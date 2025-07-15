package com.pushwoosh.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.DisconnectCause
import com.pushwoosh.calls.service.PushwooshCallService
import com.pushwoosh.calls.service.PushwooshConnectionService
import com.pushwoosh.calls.util.Constants
import com.pushwoosh.internal.utils.PWLog

class PushwooshCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val extras = intent.extras
        when (intent.action) {
            "ACTION_ACCEPT_CALL" -> {
                PWLog.debug("CALL ACCEPTED")
                //stop service for incoming call and cancel notification
                val stopIntent = Intent(context, PushwooshCallService::class.java)
                if (extras != null) {
                    stopIntent.putExtras(extras)
                }
                context?.stopService(stopIntent)
                //start a service for ongoing call and create ongoing call notification
                val ongoingCallIntent = Intent(context, PushwooshCallService::class.java).apply {
                    action = Constants.PW_POST_ONGOING_CALL_ACTION
                }
                if (extras != null) {
                    ongoingCallIntent.putExtras(extras)
                }
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                    context?.startForegroundService(ongoingCallIntent)
                } else {
                    context?.startService(ongoingCallIntent)
                }

                val payload = PushwooshVoIPMessage(extras)
                PushwooshCallPlugin.instance.callEventListener.onAnswer(payload,1)
            }
            "ACTION_REJECT_CALL" -> {
                PWLog.debug("CALL DECLINED")
                //stop service for incoming call and cancel notification
                val stopIntent = Intent(context, PushwooshCallService::class.java)
                if (extras != null) {
                    stopIntent.putExtras(extras)
                }
                context?.stopService(stopIntent)

                //destroy incoming connection
                PushwooshConnectionService.activeConnection?.setDisconnected(
                    DisconnectCause(DisconnectCause.REJECTED)
                )
                PushwooshConnectionService.activeConnection?.destroy()
                PushwooshConnectionService.activeConnection = null

                PushwooshCallPlugin.instance.callEventListener.onReject(PushwooshVoIPMessage(extras))
            }
            "ACTION_END_CALL" -> {
                PWLog.debug("CALL DECLINED")
                //stop service for incoming call and cancel notification
                val stopIntent = Intent(context, PushwooshCallService::class.java)
                if (extras != null) {
                    stopIntent.putExtras(extras)
                }
                context?.stopService(stopIntent)

                //destroy ongoing connection
                PushwooshConnectionService.activeConnection?.setDisconnected(
                    DisconnectCause(DisconnectCause.LOCAL)
                )
                PushwooshConnectionService.activeConnection?.destroy()
                PushwooshConnectionService.activeConnection = null

                val payload = PushwooshVoIPMessage(extras)
                PushwooshCallPlugin.instance.callEventListener.onDisconnect(payload)
            }
        }
    }
}