package com.pushwoosh.calls.util

import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.preference.PreferenceIntValue
import com.pushwoosh.internal.preference.PreferenceStringValue

class CallPrefs {
    companion object {
        private const val PREFERENCE = "com.pushwoosh.calls"
        private const val PROPERTY_PHONE_ACCOUNT_NAME = "phone_account_name"
        private const val PROPERTY_PHONE_ACCOUNT_HANDLE_NAME = "phone_account_handle_name"
        private const val PROPERTY_CALL_CHANNEL_INCOMING = "call_channel_incoming"
        private const val PROPERTY_CALL_CHANNEL_ONGOING = "call_channel_ongoing"
        private const val PROPERTY_CUSTOM_CALL_SOUND = "call_sound"
        private const val PROPERTY_CALL_PERMISSION_STATUS = "call_permission_status"
        private const val PROPERTY_INCOMING_CALL_TIMEOUT = "incoming_call_timeout"

        // VoIP Call Permission Status Constants
        const val PERMISSION_STATUS_NOT_REQUESTED = 0
        const val PERMISSION_STATUS_GRANTED = 1
        const val PERMISSION_STATUS_DENIED = 2
        // VoIP timeout constant
        const val DEFAULT_INCOMING_CALL_TIMEOUT = 30.0

    }

    private val phoneAccount : PreferenceStringValue
    private val phoneAccountHandle: PreferenceStringValue
    private val incomingCallChannel: PreferenceStringValue
    private val ongoingCallChannel: PreferenceStringValue
    private val callSound: PreferenceStringValue
    private val callPermissionStatus: PreferenceIntValue
    private val incomingCallTimeout: PreferenceStringValue

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
        callPermissionStatus = PreferenceIntValue(
            prefs,
            PROPERTY_CALL_PERMISSION_STATUS,
            PERMISSION_STATUS_NOT_REQUESTED
        )
        incomingCallTimeout = PreferenceStringValue(
            prefs,
            PROPERTY_INCOMING_CALL_TIMEOUT,
            DEFAULT_INCOMING_CALL_TIMEOUT.toString()
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
    
    /**
     * Gets the current VoIP call permission status
     * @return Int - permission status:
     *   0 (PERMISSION_STATUS_NOT_REQUESTED) - permission not requested
     *   1 (PERMISSION_STATUS_GRANTED) - permission granted
     *   2 (PERMISSION_STATUS_DENIED) - permission denied
     */
    fun getCallPermissionStatus(): Int = callPermissionStatus.get()
    
    /**
     * Sets the VoIP call permission status
     * @param status Int - permission status:
     *   0 (PERMISSION_STATUS_NOT_REQUESTED) - permission not requested
     *   1 (PERMISSION_STATUS_GRANTED) - permission granted  
     *   2 (PERMISSION_STATUS_DENIED) - permission denied
     */
    fun setCallPermissionStatus(status: Int) = callPermissionStatus.set(status)

    fun getIncomingCallTimeout(): Double {
        return try {
            incomingCallTimeout.get().toDouble()
        } catch (e: NumberFormatException) {
            DEFAULT_INCOMING_CALL_TIMEOUT
        }
    }

    fun setIncomingCallTimeout(timeout: Double) {
        incomingCallTimeout.set(timeout.toString())
    }
}
