package com.pushwoosh.calls

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pushwoosh.calls.service.PushwooshConnectionService
import com.pushwoosh.calls.util.Constants
import com.pushwoosh.internal.utils.PWLog

class PushwooshCallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "PushwooshCallReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent) {
        PWLog.noise(TAG, "onReceive() action=${intent.action}")
        val payload = intent.extras
        when (intent.action) {
            "ACTION_ACCEPT_CALL" -> {
                PWLog.debug(TAG, "CALL ACCEPTED")
                PushwooshConnectionService.stopCallNotificationService(payload)
                PushwooshConnectionService.startCallNotificationService(Constants.PW_POST_ONGOING_CALL_ACTION, payload)
                PushwooshConnectionService.acceptCall()
            }
            "ACTION_REJECT_CALL" -> {
                PWLog.debug(TAG, "CALL DECLINED")
                PushwooshConnectionService.stopCallNotificationService(payload)
                PushwooshConnectionService.rejectCall()
            }
            "ACTION_END_CALL" -> {
                PWLog.debug(TAG, "CALL ENDED")
                PushwooshConnectionService.stopCallNotificationService(payload)
                PushwooshConnectionService.endCall()
            }
        }
    }
}
