package com.pushwoosh.calls

import com.pushwoosh.calls.ui.PhoneNumbersPermissionActivity
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
}