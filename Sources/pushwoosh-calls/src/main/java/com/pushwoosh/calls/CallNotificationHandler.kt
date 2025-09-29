package com.pushwoosh.calls

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import com.pushwoosh.calls.service.PushwooshConnectionService
import com.pushwoosh.calls.util.CallPrefs
import com.pushwoosh.calls.util.Constants.Companion.PW_NOTIFICATION_ID_INCOMING
import com.pushwoosh.calls.util.PushwooshCallUtils
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandler


class CallNotificationHandler : MessageSystemHandler {
    override fun preHandleMessage(pushBundle: Bundle?): Boolean {
        val voipKey = pushBundle?.get("voip").toBoolean()
        if(voipKey) {
            PushwooshCallUtils.syncCallPermissionWithSystem()
            
            val permissionStatus = PushwooshCallPlugin.instance.callPrefs.getCallPermissionStatus()
            if (permissionStatus == CallPrefs.PERMISSION_STATUS_DENIED) {
                PWLog.warn("CallNotificationHandler", "VoIP call received but permissions are denied, ignoring call")
                return true
            }
            
            val context = AndroidPlatformModule.getApplicationContext()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (context != null) {
                    val telecomManager =
                        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    val phoneAccountHandleName =
                        PushwooshCallPlugin.instance.callPrefs.getPhoneAccountHandle()

                    val componentName =
                        ComponentName(context, PushwooshConnectionService::class.java)
                    val phoneAccountHandle =
                        PhoneAccountHandle(componentName, phoneAccountHandleName)

                    telecomManager.addNewIncomingCall(phoneAccountHandle, pushBundle)
                    return true
                }
                return false
            } else {
                val notification = PushwooshCallUtils.buildIncomingCallNotification(pushBundle)
                val nm = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
                nm.notify(PW_NOTIFICATION_ID_INCOMING, notification)

                return false
            }
        }
        return false
    }

    private fun Any?.toBoolean(): Boolean {
        return when (this) {
            is Boolean -> this
            is String -> this.equals("true", ignoreCase = true)
            is Number -> this.toInt() != 0
            else -> false
        }
    }
}