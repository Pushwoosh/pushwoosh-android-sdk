package com.pushwoosh.calls

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.telecom.Connection
import com.pushwoosh.calls.listener.CallAudioStateListener
import com.pushwoosh.calls.service.PushwooshConnectionService
import com.pushwoosh.exception.PushwooshException
import com.pushwoosh.function.Callback
import com.pushwoosh.function.Result
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.BackgroundExecutor
import com.pushwoosh.internal.utils.PWLog

/**
 * Controls the microphone and the speakerphone of the active VoIP call.
 *
 * Each method hands the request to the system and reports a successful result once it is handed
 * over. The actual microphone state and audio route are reported to your [CallAudioStateListener].
 * If there is no answered call in progress (or the device runs Android below 8.0), the callback
 * receives a [PushwooshException] with the message `No active call`.
 *
 * ```kotlin
 * PushwooshCallControl.mute { result ->
 *     if (!result.isSuccess) {
 *         Log.w("App", "Mute failed: ${result.exception?.message}")
 *     }
 * }
 * PushwooshCallControl.speakerOn(null)
 * ```
 *
 * **Note:** Callbacks run on the main thread.
 *
 * @see CallAudioStateListener
 */
object PushwooshCallControl {
    private const val TAG = "PushwooshCallControl"
    private const val NO_ACTIVE_CALL = "No active call"

    /**
     * Mutes the microphone of the active call.
     *
     * @param callback receives the result; may be `null`
     */
    @JvmStatic
    fun mute(callback: Callback<Void, PushwooshException>?) = setMicrophoneMute(true, callback)

    /**
     * Unmutes the microphone of the active call.
     *
     * @param callback receives the result; may be `null`
     */
    @JvmStatic
    fun unmute(callback: Callback<Void, PushwooshException>?) = setMicrophoneMute(false, callback)

    /**
     * Routes the audio of the active call to the speakerphone.
     *
     * @param callback receives the result; may be `null`
     */
    @JvmStatic
    fun speakerOn(callback: Callback<Void, PushwooshException>?) = setSpeaker(true, callback)

    /**
     * Routes the audio of the active call back from the speakerphone: to the route used before
     * [speakerOn], otherwise to the earpiece, wired headset or Bluetooth, whichever is available.
     *
     * @param callback receives the result; may be `null`
     */
    @JvmStatic
    fun speakerOff(callback: Callback<Void, PushwooshException>?) = setSpeaker(false, callback)

    private fun setMicrophoneMute(muted: Boolean, callback: Callback<Void, PushwooshException>?) {
        BackgroundExecutor.main {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || activeCall() == null) {
                fail(callback, NO_ACTIVE_CALL)
                return@main
            }
            val audioManager = AndroidPlatformModule.getApplicationContext()
                ?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (audioManager == null) {
                fail(callback, "Audio service is not available")
                return@main
            }
            audioManager.isMicrophoneMute = muted
            PWLog.info(TAG, "Microphone mute requested: $muted, microphone muted: ${audioManager.isMicrophoneMute}")
            succeed(callback)
        }
    }

    private fun setSpeaker(on: Boolean, callback: Callback<Void, PushwooshException>?) {
        BackgroundExecutor.main {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                fail(callback, NO_ACTIVE_CALL)
                return@main
            }
            val connection = activeCall()
            if (connection == null) {
                fail(callback, NO_ACTIVE_CALL)
                return@main
            }
            connection.requestSpeaker(on)
            succeed(callback)
        }
    }

    private fun activeCall(): PushwooshConnection? =
        PushwooshConnectionService.getActiveConnection()?.takeIf { it.state == Connection.STATE_ACTIVE }

    private fun succeed(callback: Callback<Void, PushwooshException>?) {
        callback?.process(Result.fromData(null))
    }

    private fun fail(callback: Callback<Void, PushwooshException>?, message: String) {
        PWLog.warn(TAG, message)
        callback?.process(Result.fromException(PushwooshException(message)))
    }
}
