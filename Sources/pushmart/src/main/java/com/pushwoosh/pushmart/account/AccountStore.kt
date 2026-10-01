package com.pushwoosh.pushmart.account

import com.pushwoosh.pushmart.core.KeyValueStore

class AccountStore(private val prefs: KeyValueStore) {
    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED)
        set(value) = prefs.putBoolean(KEY_ONBOARDED, value)

    var appCode: String
        get() = prefs.getString(KEY_APP_CODE)
        set(value) = prefs.putString(KEY_APP_CODE, value)

    var userName: String
        get() = prefs.getString(KEY_USER_NAME)
        set(value) = prefs.putString(KEY_USER_NAME, value)

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL)
        set(value) = prefs.putString(KEY_USER_EMAIL, value)

    var pushNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_PUSH_ENABLED)
        set(value) = prefs.putBoolean(KEY_PUSH_ENABLED, value)

    val isGuest: Boolean
        get() = userName.isEmpty() && userEmail.isEmpty()

    val firstName: String
        get() = userName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()

    val initials: String
        get() {
            val words = userName.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
            if (words.isEmpty()) return "G"
            return words.take(2).joinToString("") { it.first().uppercase() }
        }

    fun clearIdentity() {
        prefs.remove(KEY_USER_NAME)
        prefs.remove(KEY_USER_EMAIL)
    }

    companion object {
        const val KEY_ONBOARDED = "nuvo.onboarded"
        const val KEY_APP_CODE = "nuvo.appCode"
        const val KEY_USER_NAME = "nuvo.userName"
        const val KEY_USER_EMAIL = "nuvo.userEmail"
        const val KEY_PUSH_ENABLED = "pushNotificationEnabled"
    }
}
