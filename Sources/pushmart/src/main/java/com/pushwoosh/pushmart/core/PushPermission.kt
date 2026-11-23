package com.pushwoosh.pushmart.core

/** How a push opt-in ended, as iOS `PWPushPrimerOutcome`. */
enum class PrimerOutcome {
    /** The shopper accepted while the system had not asked yet: registering shows its dialog. */
    ACCEPTED,

    /** Notifications are already allowed: no prompt was needed. */
    SUPPRESSED,

    /** The shopper said no, the prompt could not show, or the app settings were opened instead. */
    DECLINED
}

/**
 * Every push opt-in in PushMart goes through [enable], as iOS `PushPermission.enable`: the Push
 * Primer stays silent when notifications are already allowed.
 */
interface PushPermission {
    /** The system lets PushMart post notifications. */
    val isAllowed: Boolean

    /** The system notification dialog can still show: Android 13+, not granted, never asked. */
    val isUndetermined: Boolean

    fun enable(onOutcome: (PrimerOutcome) -> Unit)
}
