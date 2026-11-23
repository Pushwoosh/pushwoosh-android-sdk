package com.pushwoosh.pushmart.core

/**
 * Answers [enable] at once: SUPPRESSED while [allowed], otherwise [outcome], counting [prompts].
 */
class FakePushPermission : PushPermission {
    var allowed = true
    var undetermined = false
    var outcome = PrimerOutcome.SUPPRESSED
    var prompts = 0

    override val isAllowed: Boolean
        get() = allowed

    override val isUndetermined: Boolean
        get() = undetermined

    override fun enable(onOutcome: (PrimerOutcome) -> Unit) {
        if (allowed) {
            onOutcome(PrimerOutcome.SUPPRESSED)
            return
        }
        prompts++
        onOutcome(outcome)
    }
}
