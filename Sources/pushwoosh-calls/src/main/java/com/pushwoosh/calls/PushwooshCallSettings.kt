package com.pushwoosh.calls

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.pushwoosh.calls.internal.CallPermissionCallbackManager
import com.pushwoosh.calls.ui.PhoneNumbersPermissionActivity
import com.pushwoosh.calls.util.CallPrefs
import com.pushwoosh.calls.util.PushwooshCallUtils
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.internal.utils.RequestPermissionHelper

/**
 * Configuration and settings management for Pushwoosh VoIP calls functionality.
 *
 * This singleton provides methods to configure VoIP call behavior, request necessary permissions,
 * and customize the call experience in your Android application. It manages phone account settings,
 * notification channels, ringtone customization, and permission status tracking.
 *
 * **Key Features:**
 * - Request VoIP call permissions (READ_PHONE_NUMBERS)
 * - Configure phone account and handle names for Android Telecom Framework
 * - Customize notification channel names for incoming and ongoing calls
 * - Set custom ringtone for incoming VoIP calls
 * - Check permission status for VoIP functionality
 *
 * **Quick Start:**
 * ```kotlin
 * // Request VoIP call permissions
 * PushwooshCallSettings.requestCallPermissions { granted, grantedPerms, deniedPerms ->
 *     if (granted) {
 *         Log.d("App", "VoIP permissions granted")
 *     } else {
 *         Log.w("App", "Permissions denied: $deniedPerms")
 *     }
 * }
 *
 * // Customize call settings
 * PushwooshCallSettings.setIncomingCallChannelName("Video Calls")
 * PushwooshCallSettings.setCallSound("custom_ringtone")
 *
 * // Check permission status
 * when (PushwooshCallSettings.getCallPermissionStatus()) {
 *     0 -> requestPermissions() // Not requested
 *     1 -> enableVoIP()         // Granted
 *     2 -> showPermissionDenied() // Denied
 * }
 * ```
 *
 * **Important Notes:**
 * - Requires Android 8.0+ (API 26+) for full Telecom Framework support
 * - READ_PHONE_NUMBERS permission is required for VoIP calls on Android 8+
 * - Custom [CallEventListener] must be registered in AndroidManifest.xml
 * - Works with FCM/HMS push notifications containing `voip=true` flag
 *
 * @see CallPermissionsCallback
 * @see CallEventListener
 * @see PushwooshVoIPMessage
 */
object PushwooshCallSettings {
    /**
     * Requests VoIP call permissions from the user without a callback.
     *
     * This method requests the READ_PHONE_NUMBERS permission required for VoIP calls on Android 8+.
     * A system permission dialog is shown if the permission has not been granted yet.
     * Use [requestCallPermissions] overload with callback to receive the permission result.
     *
     * **Example:**
     * ```kotlin
     * override fun onCreate(savedInstanceState: Bundle?) {
     *     super.onCreate(savedInstanceState)
     *
     *     // Request permissions on app launch
     *     PushwooshCallSettings.requestCallPermissions()
     *
     *     // Check status later
     *     val status = PushwooshCallSettings.getCallPermissionStatus()
     *     if (status == 1) {
     *         Log.d("App", "VoIP ready")
     *     }
     * }
     * ```
     *
     * @see requestCallPermissions
     * @see getCallPermissionStatus
     */
    @JvmStatic
    fun requestCallPermissions() {
        requestCallPermissions(null)
    }

    /**
     * Requests VoIP call permissions from the user with a result callback.
     *
     * This method requests the READ_PHONE_NUMBERS permission required for VoIP calls on Android 8+.
     * A system permission dialog is shown if the permission has not been granted yet. The callback
     * is invoked on the main thread after the user responds to the permission request.
     *
     * **Example:**
     * ```kotlin
     * class MainActivity : AppCompatActivity() {
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *         super.onCreate(savedInstanceState)
     *
     *         // Request permissions with callback
     *         PushwooshCallSettings.requestCallPermissions { granted, grantedPerms, deniedPerms ->
     *             if (granted) {
     *                 Log.d("App", "VoIP enabled: $grantedPerms")
     *                 enableVoIPFeatures()
     *             } else {
     *                 Log.w("App", "Permission denied: $deniedPerms")
     *                 showVoIPUnavailableMessage()
     *             }
     *         }
     *     }
     *
     *     private fun enableVoIPFeatures() {
     *         // Configure VoIP settings
     *         PushwooshCallSettings.setCallSound("ringtone_video_call")
     *         Toast.makeText(this, "VoIP calls enabled", Toast.LENGTH_SHORT).show()
     *     }
     * }
     * ```
     *
     * @param callback optional callback to receive the permission request result.
     *                 Called on the main thread after the user responds to the permission dialog.
     *                 If null, no callback is invoked.
     *
     * @see CallPermissionsCallback
     * @see getCallPermissionStatus
     */
    @JvmStatic
    fun requestCallPermissions(callback: CallPermissionsCallback?) {
        PWLog.noise("PushwooshCallSettings", "requestCallPermissions()")
        val context = AndroidPlatformModule.getApplicationContext()
        if (context == null) {
            PWLog.warn("PushwooshCallSettings", "can not requestCallPermissions, context is null")
            return
        }
        val permission = "android.permission.READ_PHONE_NUMBERS"

        // use callback if permissions already granted
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            callback?.onPermissionResult(
                granted = true,
                grantedPermissions = listOf(permission),
                deniedPermissions = emptyList()
            )
            return
        }

        // Store the callback for later invocation
        CallPermissionCallbackManager.setCallback(callback)

        RequestPermissionHelper.requestPermissionsForClass(
            PhoneNumbersPermissionActivity::class.java,
            context, arrayOf(permission)
        )
    }

    /**
     * Sets the phone account name for VoIP calls in the Android Telecom Framework.
     *
     * The phone account name identifies your app's calling service in the system's
     * call management UI. This is displayed to users in the native phone app and
     * system call screens.
     *
     * **Example:**
     * ```kotlin
     * class Application : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *
     *         // Configure phone account with app name
     *         PushwooshCallSettings.setPhoneAccount("MyApp Video Calls")
     *         PushwooshCallSettings.setPhoneAccountHandle("myapp_voip_handle")
     *     }
     * }
     * ```
     *
     * @param phoneAccount the name to display for this phone account.
     *                     Default is "pushwoosh" if not set.
     *
     * @see setPhoneAccountHandle
     */
    @JvmStatic
    fun setPhoneAccount(phoneAccount: String) {
        PushwooshCallPlugin.instance.callPrefs.setPhoneAccount(phoneAccount)
    }

    /**
     * Sets the phone account handle name for VoIP calls in the Android Telecom Framework.
     *
     * The phone account handle is a unique identifier for your app's calling service
     * within the Android Telecom system. It must be unique per application.
     *
     * **Example:**
     * ```kotlin
     * // Configure handle for telecom system
     * PushwooshCallSettings.setPhoneAccountHandle("com.myapp.voip")
     * ```
     *
     * @param phoneAccountHandle the unique handle identifier for this phone account.
     *                          Default is "pushwoosh handle" if not set.
     *
     * @see setPhoneAccount
     */
    @JvmStatic
    fun setPhoneAccountHandle(phoneAccountHandle: String) {
        PushwooshCallPlugin.instance.callPrefs.setPhoneAccountHandle(phoneAccountHandle)
    }

    /**
     * Sets the notification channel name for incoming VoIP call notifications.
     *
     * This name is displayed in the Android notification settings where users can
     * customize the notification behavior for incoming calls. The channel is used
     * for incoming call notifications with ringtone and full-screen intent.
     *
     * **Example:**
     * ```kotlin
     * class SettingsActivity : AppCompatActivity() {
     *     override fun onCreate(savedInstanceState: Bundle?) {
     *         super.onCreate(savedInstanceState)
     *
     *         // Customize notification channels
     *         PushwooshCallSettings.setIncomingCallChannelName("Video Calls")
     *         PushwooshCallSettings.setOngoingCallChannelName("Active Calls")
     *     }
     * }
     * ```
     *
     * @param name the channel name to display in notification settings.
     *             Default is "Incoming Calls" if not set.
     *
     * @see setOngoingCallChannelName
     */
    @JvmStatic
    fun setIncomingCallChannelName(name: String) {
        PushwooshCallPlugin.instance.callPrefs.setIncomingCallChannelName(name)
    }

    /**
     * Sets the notification channel name for ongoing VoIP call notifications.
     *
     * This name is displayed in the Android notification settings for the channel
     * used during active calls. The ongoing call notification is silent and maintains
     * the foreground service while a call is in progress.
     *
     * **Example:**
     * ```kotlin
     * // Customize ongoing call notification
     * PushwooshCallSettings.setOngoingCallChannelName("Active Video Calls")
     * ```
     *
     * @param name the channel name to display in notification settings.
     *             Default is "Ongoing Calls" if not set.
     *
     * @see setIncomingCallChannelName
     */
    @JvmStatic
    fun setOngoingCallChannelName(name: String) {
        PushwooshCallPlugin.instance.callPrefs.setOngoingCallChannelName(name)
    }

    /**
     * Sets the custom ringtone sound for incoming VoIP calls.
     *
     * Specifies the audio file to play when a VoIP call arrives. The sound file
     * must be located in the app's `res/raw` folder. Use the filename without extension.
     * The default system ringtone is used if set to "default".
     *
     * **Example:**
     * ```kotlin
     * class Application : Application() {
     *     override fun onCreate() {
     *         super.onCreate()
     *
     *         // Use custom ringtone from res/raw/video_call_ringtone.mp3
     *         PushwooshCallSettings.setCallSound("video_call_ringtone")
     *
     *         // Or use default system ringtone
     *         PushwooshCallSettings.setCallSound("default")
     *     }
     * }
     * ```
     *
     * **Note:** The ringtone only plays for incoming call notifications.
     * It stops when the user answers or rejects the call.
     *
     * @param sound the name of the sound file in res/raw (without extension),
     *              or "default" to use the system ringtone.
     *              Default is "default" if not set.
     */
    @JvmStatic
    fun setCallSound(sound: String) {
        PushwooshCallPlugin.instance.callPrefs.setCallSoundName(sound)
    }

    /**
     * Sets the timeout duration for incoming VoIP calls.
     *
     * When an incoming call is not answered within this timeout period, it will be automatically
     * ended and reported to the system as an unanswered (missed) call.
     *
     * @param timeout The timeout duration in seconds. Default value is 30.0 seconds.
     */
    @JvmStatic
    fun setIncomingCallTimeout(timeout: Double) {
        PushwooshCallPlugin.instance.callPrefs.setIncomingCallTimeout(timeout)
    }

    /**
     * Gets the current VoIP call permission status.
     *
     * This method returns the current state of the READ_PHONE_NUMBERS permission
     * required for VoIP calls. The status is synchronized with the system before
     * being returned to ensure accuracy.
     *
     * **Status Values:**
     * - `0` - Permission not requested yet
     * - `1` - Permission granted by user
     * - `2` - Permission denied by user
     *
     * **Example:**
     * ```kotlin
     * class MainActivity : AppCompatActivity() {
     *     override fun onResume() {
     *         super.onResume()
     *
     *         // Check permission status on resume
     *         when (PushwooshCallSettings.getCallPermissionStatus()) {
     *             0 -> {
     *                 // First time - show explanation and request
     *                 showVoIPPermissionExplanation()
     *                 PushwooshCallSettings.requestCallPermissions()
     *             }
     *             1 -> {
     *                 // Granted - enable VoIP features
     *                 enableVoIPCallButton()
     *                 Log.d("App", "VoIP calls available")
     *             }
     *             2 -> {
     *                 // Denied - show settings prompt
     *                 showPermissionDeniedDialog()
     *                 disableVoIPCallButton()
     *             }
     *         }
     *     }
     *
     *     private fun showPermissionDeniedDialog() {
     *         AlertDialog.Builder(this)
     *             .setTitle("VoIP Permission Required")
     *             .setMessage("Please enable phone permissions in Settings")
     *             .setPositiveButton("Settings") { _, _ ->
     *                 openAppSettings()
     *             }
     *             .show()
     *     }
     * }
     * ```
     *
     * @return the current permission status:
     *         - `0` if permission has not been requested yet
     *         - `1` if permission was granted by the user
     *         - `2` if permission was denied by the user
     *
     * @see requestCallPermissions
     */
    @JvmStatic
    fun getCallPermissionStatus(): Int {
        PushwooshCallUtils.syncCallPermissionWithSystem()
        return PushwooshCallPlugin.instance.callPrefs.getCallPermissionStatus()
    }
    
    /**
     * Internal method to set VoIP call permission status
     * Used internally by the SDK to track permission state
     * @param status Int - permission status (0=not requested, 1=granted, 2=denied)
     */
    @JvmStatic
    internal fun setCallPermissionStatus(status: Int) {
        PushwooshCallPlugin.instance.callPrefs.setCallPermissionStatus(status)
    }
}
