package com.pushwoosh.pushmart.account

import com.pushwoosh.pushmart.core.KeyValueStore

class AccountStore(private val prefs: KeyValueStore) {
    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED)
        set(value) = prefs.putBoolean(KEY_ONBOARDED, value)

    var personalCode: String?
        get() = prefs.getString(KEY_APP_CODE).takeIf { AppCode.isValid(it) && it != AppCode.DEMO }
        set(value) {
            if (value == null) prefs.remove(KEY_APP_CODE) else prefs.putString(KEY_APP_CODE, value)
        }

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL)
        set(value) = prefs.putString(KEY_USER_EMAIL, value)

    var smsNumber: String
        get() = prefs.getString(KEY_SMS_NUMBER)
        set(value) = prefs.putString(KEY_SMS_NUMBER, value)

    var whatsappNumber: String
        get() = prefs.getString(KEY_WHATSAPP_NUMBER)
        set(value) = prefs.putString(KEY_WHATSAPP_NUMBER, value)

    var pushNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_PUSH_ENABLED)
        set(value) = prefs.putBoolean(KEY_PUSH_ENABLED, value)

    val isGuest: Boolean
        get() = personalCode == null

    companion object {
        const val KEY_ONBOARDED = "nuvo.onboarded"
        const val KEY_APP_CODE = "nuvo.appCode"
        const val KEY_USER_EMAIL = "nuvo.userEmail"
        const val KEY_SMS_NUMBER = "pushmart.smsNumber"
        const val KEY_WHATSAPP_NUMBER = "pushmart.whatsappNumber"
        const val KEY_PUSH_ENABLED = "pushNotificationEnabled"
    }
}
