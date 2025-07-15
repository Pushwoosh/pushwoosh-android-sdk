package com.pushwoosh.calls.util

import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.preference.PreferenceStringValue

class CallPrefs {
    companion object {
        private const val PREFERENCE = "com.pushwoosh.calls"
        private const val PROPERTY_PHONE_ACCOUNT_NAME = "phone_account_name"
        private const val PROPERTY_PHONE_ACCOUNT_HANDLE_NAME = "phone_account_handle_name"
        private const val PROPERTY_CALL_CHANNEL_INCOMING = "call_channel_incoming"
        private const val PROPERTY_CALL_CHANNEL_ONGOING = "call_channel_ongoing"
        private const val PROPERTY_CUSTOM_CALL_SOUND = "call_sound"

    }

    private val phoneAccount : PreferenceStringValue
    private val phoneAccountHandle: PreferenceStringValue
    private val incomingCallChannel: PreferenceStringValue
    private val ongoingCallChannel: PreferenceStringValue
    private val callSound: PreferenceStringValue

    init {
        val prefs = AndroidPlatformModule.getPrefsProvider().providePrefs(PREFERENCE)
        phoneAccount = PreferenceStringValue(
            prefs,
            PROPERTY_PHONE_ACCOUNT_NAME,
            "pushwoosh"
        )
        phoneAccountHandle = PreferenceStringValue(
            prefs,
            PROPERTY_PHONE_ACCOUNT_HANDLE_NAME,
            "pushwoosh handle"
        )
        incomingCallChannel = PreferenceStringValue(
            prefs,
            PROPERTY_CALL_CHANNEL_INCOMING,
            "Incoming Calls"
        )
        ongoingCallChannel = PreferenceStringValue(
            prefs,
            PROPERTY_CALL_CHANNEL_ONGOING,
            "Ongoing Calls"
        )
        callSound = PreferenceStringValue(
            prefs,
            PROPERTY_CUSTOM_CALL_SOUND,
            "default"
        )
    }

    fun getPhoneAccount(): String = phoneAccount.get()

    fun setPhoneAccount(value: String) = phoneAccount.set(value)

    fun getPhoneAccountHandle(): String = phoneAccountHandle.get()

    fun setPhoneAccountHandle(value: String) = phoneAccountHandle.set(value)

    fun getIncomingCallChannelName(): String = incomingCallChannel.get()

    fun setIncomingCallChannelName(value: String) = incomingCallChannel.set(value)

    fun getOngoingCallChannelName(): String = ongoingCallChannel.get()

    fun setOngoingCallChannelName(value: String) = ongoingCallChannel.set(value)

    fun getCallSoundName(): String = callSound.get()

    fun setCallSoundName(value: String) = callSound.set(value)
}