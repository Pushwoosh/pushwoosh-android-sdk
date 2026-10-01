package com.pushwoosh.pushmart.account

import com.pushwoosh.pushmart.sdk.GatewayResult
import com.pushwoosh.pushmart.sdk.PushwooshGateway

sealed class AccountOutcome {
    data class Success(val title: String, val message: String) : AccountOutcome()

    data class Failure(val title: String, val message: String) : AccountOutcome()
}

class AccountManager(private val store: AccountStore, private val gateway: PushwooshGateway) {
    fun connect(rawCode: String): String? {
        val code = AppCode.mask(rawCode.trim())
        if (!AppCode.isValid(code)) return "Enter your access code in the format XXXXX-XXXXX."
        gateway.setAppId(code)
        store.appCode = code
        return null
    }

    fun signIn(name: String, email: String, onError: (String) -> Unit) {
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        store.userName = cleanName
        store.userEmail = cleanEmail
        if (cleanEmail.isNotEmpty()) applyIdentity(cleanEmail, onError)
        store.onboarded = true
    }

    fun continueAsGuest() {
        store.clearIdentity()
        store.onboarded = true
    }

    /** Debug entry for e2e runs: connect and continue as guest without the onboarding screens. */
    fun enterWithoutOnboarding(appCode: String?): Boolean {
        if (store.onboarded) return false
        val masked = appCode?.let { AppCode.mask(it.trim()) }
        val code = if (masked != null && AppCode.isValid(masked)) masked else AppCode.DEMO
        connect(code)
        continueAsGuest()
        return true
    }

    fun switchAccount(
        rawCode: String,
        name: String,
        email: String,
        onResult: (AccountOutcome) -> Unit,
    ) {
        val code = AppCode.mask(rawCode.trim())
        if (!AppCode.isValid(code)) {
            onResult(AccountOutcome.Failure("Invalid app code", "Use the format XXXXX-XXXXX."))
            return
        }
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        gateway.unregisterForPushNotifications()
        gateway.setAppId(code)
        store.appCode = code
        store.userName = cleanName
        store.userEmail = cleanEmail

        fun finishSwitch(identityError: String?) {
            gateway.registerForPushNotifications { result ->
                store.pushNotificationEnabled = result.isSuccess
                val error = identityError ?: result.error.takeIf { !result.isSuccess }
                if (error == null) {
                    onResult(AccountOutcome.Success("Account switched", "Connected to app $code."))
                } else {
                    onResult(
                        AccountOutcome.Failure(
                            "Switch incomplete", "Connected to app $code, but $error."))
                }
            }
        }

        if (cleanEmail.isEmpty()) {
            finishSwitch(null)
            return
        }
        gateway.setEmail(cleanEmail) { emailResult ->
            val emailError = emailResult.error.takeIf { !emailResult.isSuccess }
            gateway.setUserId(cleanEmail) { userIdResult ->
                val identityError =
                    emailError ?: userIdResult.error.takeIf { !userIdResult.isSuccess }
                finishSwitch(identityError)
            }
        }
    }

    fun signOut() {
        gateway.unregisterForPushNotifications()
        store.clearIdentity()
        store.pushNotificationEnabled = false
        store.onboarded = false
    }

    fun saveDetails(
        name: String,
        memberId: String,
        email: String,
        onResult: (AccountOutcome) -> Unit,
    ) {
        val cleanId = memberId.trim()
        val cleanEmail = email.trim()
        store.userName = name.trim()
        if (cleanId.isEmpty() && cleanEmail.isEmpty()) {
            onResult(AccountOutcome.Failure("Nothing to save", "Add a member ID or email first."))
            return
        }
        var pending = 0
        if (cleanId.isNotEmpty()) pending++
        if (cleanEmail.isNotEmpty()) pending++
        var firstError: String? = null
        fun onCallbackReturned(result: GatewayResult<Unit>) {
            if (!result.isSuccess && firstError == null) firstError = result.error.orEmpty()
            pending--
            if (pending == 0) {
                val error = firstError
                if (error == null) {
                    onResult(AccountOutcome.Success("Saved", "Your details are up to date."))
                } else {
                    onResult(AccountOutcome.Failure("Save failed", error))
                }
            }
        }
        if (cleanId.isNotEmpty()) {
            gateway.setUserId(cleanId) { onCallbackReturned(it) }
        }
        if (cleanEmail.isNotEmpty()) {
            store.userEmail = cleanEmail
            gateway.setEmail(cleanEmail) { onCallbackReturned(it) }
        }
    }

    fun setPushEnabled(on: Boolean, onResult: (AccountOutcome) -> Unit) {
        if (!on) {
            gateway.unregisterForPushNotifications()
            store.pushNotificationEnabled = false
            onResult(AccountOutcome.Success("Notifications off", ""))
            return
        }
        gateway.registerForPushNotifications { result ->
            if (result.isSuccess) {
                store.pushNotificationEnabled = true
                onResult(AccountOutcome.Success("Registered", result.data.orEmpty()))
            } else {
                store.pushNotificationEnabled = false
                onResult(AccountOutcome.Failure("Register failed", result.error.orEmpty()))
            }
        }
    }

    private fun applyIdentity(email: String, onError: (String) -> Unit) {
        gateway.setEmail(email) { if (!it.isSuccess) onError(it.error.orEmpty()) }
        gateway.setUserId(email) { if (!it.isSuccess) onError(it.error.orEmpty()) }
    }
}
