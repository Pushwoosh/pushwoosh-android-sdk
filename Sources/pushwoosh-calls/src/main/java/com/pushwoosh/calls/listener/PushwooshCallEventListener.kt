package com.pushwoosh.calls.listener

import android.content.Intent
import android.os.Bundle
import com.pushwoosh.calls.PushwooshVoIPMessage
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.PWLog

/**
 * Default fallback implementation of [CallEventListener].
 *
 * **This is NOT intended for production use!**
 *
 * You MUST create your own implementation to:
 * - Navigate to your in-call UI ([onAnswer])
 * - Establish WebRTC/VoIP connection
 * - Handle call lifecycle events ([onReject], [onDisconnect])
 *
 * Register your custom listener in AndroidManifest.xml:
 * ```xml
 * <meta-data
 *     android:name="com.pushwoosh.CALL_EVENT_LISTENER"
 *     android:value="com.yourapp.YourCallEventListener" />
 * ```
 *
 * @see CallEventListener
 */
class PushwooshCallEventListener : CallEventListener {
    override fun onAnswer(voIPMessage: PushwooshVoIPMessage, videoState: Int) {
        PWLog.warn("PushwooshCallEventListener", "Default onAnswer() called - implement custom CallEventListener for production!")
        PWLog.info("PushwooshCallEventListener", "onAnswer event received: caller=${voIPMessage.callerName}")

        // Fallback: Launch main activity to bring app to foreground
        try {
            val context = AndroidPlatformModule.getApplicationContext()
            val launchIntent = context?.packageManager?.getLaunchIntentForPackage(context.packageName)

            launchIntent?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("voip_call_active", true)
                putExtra("voip_caller_name", voIPMessage.callerName)
                putExtra("voip_has_video", voIPMessage.hasVideo)
            }

            context?.startActivity(launchIntent)
            PWLog.info("PushwooshCallEventListener", "Launched main activity for call")
        } catch (e: Exception) {
            PWLog.error("PushwooshCallEventListener", "Failed to launch activity", e)
        }
    }

    override fun onReject(voIPMessage: PushwooshVoIPMessage) {
        PWLog.warn("PushwooshCallEventListener", "Default onReject() called - implement custom CallEventListener for production!")
        PWLog.info("PushwooshCallEventListener", "onReject event received: caller=${voIPMessage.callerName}")
    }

    override fun onDisconnect(voIPMessage: PushwooshVoIPMessage) {
        PWLog.warn("PushwooshCallEventListener", "Default onDisconnect() called - implement custom CallEventListener for production!")
        PWLog.info("PushwooshCallEventListener", "onDisconnect event received: caller=${voIPMessage.callerName}")
    }

    override fun onCallAdded(voIPMessage: PushwooshVoIPMessage) {
        PWLog.info("PushwooshCallEventListener", "onCallAdded event received: caller=${voIPMessage.callerName}")
    }

    override fun onCallRemoved(voIPMessage: PushwooshVoIPMessage) {
        PWLog.info("PushwooshCallEventListener", "onCallRemoved event received: caller=${voIPMessage.callerName}")
    }

    override fun onCreateIncomingConnection(payload: Bundle?) {
        PWLog.info("PushwooshCallEventListener", "onCreateIncomingConnection event received")
    }
}
