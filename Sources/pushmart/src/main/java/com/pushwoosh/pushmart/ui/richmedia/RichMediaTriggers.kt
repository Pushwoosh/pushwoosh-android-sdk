package com.pushwoosh.pushmart.ui.richmedia

import androidx.annotation.MainThread
import com.pushwoosh.internal.utils.PWLog
import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.sdk.PushwooshGateway

/**
 * Posts a server-trigger event and reports what really happened. The `postEvent` callback only says
 * the request went through — whether a campaign answered is known solely from the rich media
 * delegate, so "content shown" is reported from [onContentShown], never from the callback. The
 * watchdog is armed right after posting, not from inside the callback, so a request the SDK never
 * answers (bad app code, init not ready) still resolves instead of leaving the screen stuck.
 */
@MainThread
class RichMediaTriggers(
    private val gateway: PushwooshGateway,
    private val timer: DelayedRunner,
    private val report: (AccountOutcome) -> Unit,
) {
    private var generation = 0
    private var cancelTimeout: (() -> Unit)? = null

    fun trigger(eventName: String) {
        val current = startGeneration()
        report(AccountOutcome.Success("Sending $eventName…", ""))
        var callbackArrived = false
        var settled = false
        gateway.postEvent(eventName, emptyMap()) { result ->
            if (current != generation || settled) return@postEvent
            callbackArrived = true
            if (!result.isSuccess) {
                settled = true
                cancelTimeout?.invoke()
                cancelTimeout = null
                report(AccountOutcome.Failure("Event failed", result.error ?: "unknown error"))
                return@postEvent
            }
            report(AccountOutcome.Success("Event sent", "Waiting for content…"))
        }
        if (current != generation || settled) return
        val cancel =
            timer.runLater(CONTENT_TIMEOUT_MS) {
                if (current != generation || settled) return@runLater
                settled = true
                cancelTimeout = null
                report(
                    if (callbackArrived) {
                        AccountOutcome.Failure(
                            "No content returned",
                            "$eventName was sent but nothing came back. Configure a campaign for this event in Control Panel.")
                    } else {
                        AccountOutcome.Failure(
                            "No response",
                            "$eventName got no answer in 5 s. Check the app code and network.")
                    })
            }
        if (current == generation && !settled) cancelTimeout = cancel else cancel()
    }

    fun onContentShown(code: String) {
        startGeneration()
        report(AccountOutcome.Success("Content shown", code))
    }

    fun onContentFailed(code: String, error: String) {
        startGeneration()
        PWLog.warn(TAG, "rich media $code failed: $error")
        report(AccountOutcome.Failure("Content failed", error))
    }

    /**
     * Drops the pending trigger silently (screen left); late callbacks and the timer are ignored.
     */
    fun cancel() {
        startGeneration()
    }

    private fun startGeneration(): Int {
        cancelTimeout?.invoke()
        cancelTimeout = null
        return ++generation
    }

    companion object {
        const val CONTENT_TIMEOUT_MS = 5_000L
        private const val TAG = "RichMediaTriggers"
    }
}
