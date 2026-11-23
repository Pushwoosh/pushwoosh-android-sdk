package com.pushwoosh.pushmart.account

import com.pushwoosh.pushmart.core.PrimerOutcome
import com.pushwoosh.pushmart.core.PushPermission
import com.pushwoosh.pushmart.sdk.InboxGateway
import com.pushwoosh.pushmart.sdk.PushwooshGateway

sealed class AccountOutcome {
    data class Success(val title: String, val message: String) : AccountOutcome()

    data class Failure(val title: String, val message: String) : AccountOutcome()
}

sealed class CodeSwitch {
    data class Applied(val code: String) : CodeSwitch()

    object Removed : CodeSwitch()

    object Invalid : CodeSwitch()

    object Unchanged : CodeSwitch()

    object NoConnection : CodeSwitch()

    object Busy : CodeSwitch()
}

class AccountManager(
    private val store: AccountStore,
    private val gateway: PushwooshGateway,
    private val inbox: InboxGateway,
    private val permission: PushPermission,
    private val isOnline: () -> Boolean
) {
    private val switchListeners = mutableListOf<() -> Unit>()

    var isSwitching = false
        private set(value) {
            field = value
            switchListeners.toList().forEach { it() }
        }

    fun addSwitchListener(listener: () -> Unit) {
        switchListeners.add(listener)
    }

    fun removeSwitchListener(listener: () -> Unit) {
        switchListeners.remove(listener)
    }

    fun applyOnLaunch() {
        gateway.setAppId(store.personalCode ?: AppCode.DEMO)
        gateway.resetUserId()
    }

    fun skipOnboarding(rawCode: String?) {
        store.onboarded = true
        store.personalCode =
            rawCode?.let { AppCode.mask(it) }?.takeIf { AppCode.isValid(it) && it != AppCode.DEMO }
        applyOnLaunch()
    }

    fun applyCode(rawCode: String, onResult: (CodeSwitch) -> Unit) {
        val code = AppCode.mask(rawCode.trim())
        when {
            !AppCode.isValid(code) -> onResult(CodeSwitch.Invalid)
            code == (store.personalCode ?: AppCode.DEMO) -> onResult(CodeSwitch.Unchanged)
            code == AppCode.DEMO -> removeCode(onResult)
            else -> switchTo(code, CodeSwitch.Applied(code), onResult)
        }
    }

    fun removeCode(onResult: (CodeSwitch) -> Unit) {
        if (store.isGuest) {
            onResult(CodeSwitch.Unchanged)
        } else {
            switchTo(null, CodeSwitch.Removed, onResult)
        }
    }

    private fun switchTo(code: String?, success: CodeSwitch, onResult: (CodeSwitch) -> Unit) {
        if (isSwitching) {
            onResult(CodeSwitch.Busy)
            return
        }
        if (!isOnline()) {
            onResult(CodeSwitch.NoConnection)
            return
        }
        isSwitching = true
        // The SDK unregister worker reads the app id when it runs: switch only after its callback.
        gateway.unregisterForPushNotifications { result ->
            if (!result.isSuccess) {
                // The SDK drops its registered state before sending, so the device is off anyway.
                store.pushNotificationEnabled = false
                isSwitching = false
                onResult(CodeSwitch.NoConnection)
                return@unregisterForPushNotifications
            }
            store.personalCode = code
            gateway.setAppId(code ?: AppCode.DEMO)
            inbox.resync()
            isSwitching = false
            onResult(success)
            if (!permission.isUndetermined) {
                register()
                return@unregisterForPushNotifications
            }
            permission.enable { outcome ->
                if (outcome == PrimerOutcome.DECLINED) store.pushNotificationEnabled = false
                else register()
            }
        }
    }

    private fun register() {
        gateway.registerForPushNotifications { registered ->
            store.pushNotificationEnabled = registered.isSuccess
        }
    }

    /**
     * Profile > Your details > Save, as iOS `UserView.save`: sets a non-empty email, registers each
     * number that is new or changed, and stores both numbers (a cleared one as empty, with no SDK
     * call). [onResult] comes after the `setEmail` callback, or at once without an email.
     */
    fun saveDetails(
        email: String,
        sms: String,
        whatsapp: String,
        onResult: (AccountOutcome) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanSms = sms.trim()
        val cleanWhatsapp = whatsapp.trim()
        if (cleanEmail.isEmpty() && cleanSms.isEmpty() && cleanWhatsapp.isEmpty()) {
            onResult(
                AccountOutcome.Failure(
                    "Nothing to save", "Add your email or a phone number first."))
            return
        }
        val registerSms = cleanSms.isNotEmpty() && cleanSms != store.smsNumber
        val registerWhatsapp = cleanWhatsapp.isNotEmpty() && cleanWhatsapp != store.whatsappNumber
        store.smsNumber = cleanSms
        store.whatsappNumber = cleanWhatsapp
        val saved = AccountOutcome.Success("Saved", "Your details are up to date.")
        if (cleanEmail.isNotEmpty()) {
            store.userEmail = cleanEmail
            gateway.setEmail(cleanEmail) { result ->
                if (result.isSuccess) {
                    onResult(saved)
                } else {
                    onResult(AccountOutcome.Failure("Save failed", result.error.orEmpty()))
                }
            }
        }
        if (registerSms) gateway.registerSmsNumber(cleanSms)
        if (registerWhatsapp) gateway.registerWhatsappNumber(cleanWhatsapp)
        if (cleanEmail.isEmpty()) onResult(saved)
    }

    /**
     * Turns push on through the Push Primer, or off. [onResult] gets whether push is on now: the
     * SDK registered and the system allows notifications. On Android 13+ the register callback
     * comes after the shopper answers the system dialog. While a code switch runs nothing changes.
     */
    fun setPushEnabled(on: Boolean, onResult: (pushOn: Boolean) -> Unit) {
        if (isSwitching) {
            onResult(store.pushNotificationEnabled)
            return
        }
        if (!on) {
            gateway.unregisterForPushNotifications()
            store.pushNotificationEnabled = false
            onResult(false)
            return
        }
        permission.enable { outcome ->
            if (outcome == PrimerOutcome.DECLINED) {
                onResult(false)
                return@enable
            }
            gateway.registerForPushNotifications { result ->
                store.pushNotificationEnabled = result.isSuccess
                onResult(result.isSuccess && permission.isAllowed)
            }
        }
    }
}
