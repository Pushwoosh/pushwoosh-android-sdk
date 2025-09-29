package com.pushwoosh.calls

import com.pushwoosh.calls.ui.PhoneNumbersPermissionActivity
import com.pushwoosh.calls.util.PushwooshCallUtils
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.utils.RequestPermissionHelper

object PushwooshCallSettings {
    @JvmStatic
    fun requestCallPermissions() {
        val context = AndroidPlatformModule.getApplicationContext()

        RequestPermissionHelper.requestPermissionsForClass(
            PhoneNumbersPermissionActivity::class.java,
            context, arrayOf("android.permission.READ_PHONE_NUMBERS")
        )
    }

    @JvmStatic
    fun setPhoneAccount(phoneAccount: String) {
        PushwooshCallPlugin.instance.callPrefs.setPhoneAccount(phoneAccount)
    }

    @JvmStatic
    fun setPhoneAccountHandle(phoneAccountHandle: String) {
        PushwooshCallPlugin.instance.callPrefs.setPhoneAccountHandle(phoneAccountHandle)
    }

    @JvmStatic
    fun setIncomingCallChannelName(name: String) {
        PushwooshCallPlugin.instance.callPrefs.setIncomingCallChannelName(name)
    }

    @JvmStatic
    fun setOngoingCallChannelName(name: String) {
        PushwooshCallPlugin.instance.callPrefs.setOngoingCallChannelName(name)
    }

    @JvmStatic
    fun setCallSound(sound: String) {
        PushwooshCallPlugin.instance.callPrefs.setCallSoundName(sound)
    }
    
    /**
     * Gets the current VoIP call permission status
     * @return Int - permission status:
     *   0 - Permission not requested yet
     *   1 - Permission granted by user
     *   2 - Permission denied by user
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
