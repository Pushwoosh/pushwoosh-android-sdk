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

/**
 * Intercepts and handles incoming VoIP push notifications in the Pushwoosh SDK.
 *
 * This handler is registered in the message processing chain and routes VoIP calls to the appropriate
 * Android component based on the device's API level:
 * - **Android 8+**: Uses [TelecomManager] to display native system call UI
 * - **Android <8**: Falls back to custom notifications
 *
 * The handler performs permission checks before processing calls and safely handles all errors to
 * prevent crashes in the critical message delivery path. When a VoIP push is detected (by the "voip"
 * flag in the push bundle), it synchronizes call permissions with the system and verifies the user
 * has granted necessary permissions before proceeding.
 *
 * @see MessageSystemHandler
 * @see PushwooshConnectionService
 * @see PushwooshCallPlugin
 */
class CallNotificationHandler : MessageSystemHandler {
    companion object {
        private const val TAG = "CallNotificationHandler"
    }
    /**
     * Entry point for processing incoming push messages in the Pushwoosh message chain.
     *
     * Checks if the push bundle contains a VoIP call by examining the "voip" key. If present,
     * the handler performs the following steps:
     * 1. Synchronizes call permissions with the system to ensure up-to-date status
     * 2. Checks if the user has denied call permissions and aborts if so
     * 3. Routes the call to [registerIncomingCallWithTelecom] on Android 8+
     *    or [showLegacyVoipNotification] on older versions
     *
     * This method is called on the main message processing thread and must not perform blocking operations.
     *
     * @param pushBundle Bundle containing push notification data, including the optional "voip" flag
     * @return `true` if this was a VoIP call and was handled (stops further processing),
     *         `false` if not a VoIP call (allows other handlers to process)
     */
    override fun preHandleMessage(pushBundle: Bundle?): Boolean {
        PWLog.noise(TAG, "preHandleMessage()")
        val voipKey = pushBundle?.get("voip").toBoolean()
        if(voipKey) {
            PushwooshCallUtils.syncCallPermissionWithSystem()
            
            val permissionStatus = PushwooshCallPlugin.instance.callPrefs.getCallPermissionStatus()
            if (permissionStatus == CallPrefs.PERMISSION_STATUS_DENIED) {
                PWLog.warn(TAG, "VoIP call received but permissions are denied, ignoring call")
                return true
            }
            
            val context = AndroidPlatformModule.getApplicationContext()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                registerIncomingCallWithTelecom(context, pushBundle)
            } else {
                showLegacyVoipNotification(context, pushBundle)
            }
        }
        return false
    }

    /**
     * Registers incoming VoIP call with Android Telecom Framework (API 26+)
     * Creates necessary Telecom objects and triggers the native incoming call UI
     *
     * @param context Application context (nullable)
     * @param pushBundle VoIP push data containing call information
     * @return true if call was successfully registered, false on error
     */
    private fun registerIncomingCallWithTelecom(context: Context?, pushBundle: Bundle?): Boolean {
        PWLog.noise(TAG, "registerIncomingCallWithTelecom()")
        if (context == null) {
            PWLog.error(TAG, "Cannot register VoIP call: context is null")
            return false
        }

        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val phoneAccountHandleName = PushwooshCallPlugin.instance.callPrefs.getPhoneAccountHandle()

            val componentName = ComponentName(context, PushwooshConnectionService::class.java)
            val phoneAccountHandle = PhoneAccountHandle(componentName, phoneAccountHandleName)

            telecomManager.addNewIncomingCall(phoneAccountHandle, pushBundle)
            return true

        } catch (e: ClassCastException) {
            PWLog.error(TAG, "Failed to get TelecomManager service", e)
            return false
        } catch (e: SecurityException) {
            PWLog.error(TAG, "SecurityException: Missing MANAGE_OWN_CALLS permission", e)
            return false
        } catch (e: IllegalArgumentException) {
            PWLog.error(TAG, "IllegalArgumentException: Phone account not registered", e)
            return false
        } catch (e: Exception) {
            PWLog.error(TAG, "Unexpected error registering VoIP call", e)
            return false
        }
    }

    /**
     * Shows VoIP call notification on Android <8 (legacy mode)
     * Used as fallback when Telecom Framework is not available
     *
     * @param context Application context (nullable)
     * @param pushBundle VoIP push data containing call information
     * @return false to allow further processing in message chain
     */
    private fun showLegacyVoipNotification(context: Context?, pushBundle: Bundle?): Boolean {
        if (context == null) {
            PWLog.error(TAG, "Cannot show VoIP notification: context is null")
            return false
        }

        try {
            val notification = PushwooshCallUtils.buildIncomingCallNotification(pushBundle)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            if (nm != null) {
                nm.notify(PW_NOTIFICATION_ID_INCOMING, notification)
                PWLog.info(TAG, "VoIP notification shown (legacy mode for Android <8)")
                return true
            } else {
                PWLog.error(TAG, "NotificationManager not available")
            }
        } catch (e: Exception) {
            PWLog.error(TAG, "Failed to show VoIP notification", e)
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
