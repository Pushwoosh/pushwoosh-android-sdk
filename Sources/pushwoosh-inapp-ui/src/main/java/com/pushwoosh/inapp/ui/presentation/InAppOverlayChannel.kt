package com.pushwoosh.inapp.ui.presentation

import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.inapp.ui.overlay.InAppOverlayController
import com.pushwoosh.internal.utils.PWLog

/**
 * Presentation channel for non-blocking templates (banner, floating modal). Adapts the
 * decorView overlay ([InAppOverlayController]) to the queue's synchronous handshake: an attach
 * that succeeds confirms the show ([InAppQueueManager.onPresentConfirmed]); an attach that finds
 * no host Activity yet is a transient, retryable state, deferred back onto the queue
 * ([InAppQueueManager.onPresentDeferred]) rather than dropped; a present that throws is a real
 * failure and releases the slot silently ([InAppQueueManager.onDismissed] with no `didClose`,
 * mirroring iOS skipping a failed present).
 *
 * The eventual dismissal is reported back through a callback bound to this same queue manager and
 * threaded into [InAppOverlayController.show], so a close routes to the queue this channel holds a
 * strong reference to — never by re-resolving the nullable application-context global from inside
 * the controller.
 *
 * The [show] seam defaults to the real controller and is overridden in unit tests.
 */
internal class InAppOverlayChannel(
    private val queueManager: InAppQueueManager,
    private val show: (InAppMessage, (String?) -> Unit) -> Boolean = InAppOverlayController::show
) : InAppPresentationChannel {

    override fun present(message: InAppMessage) {
        val shown = try {
            show(message, queueManager::onDismissed)
        } catch (e: Throwable) {
            // show() builds the view and runs integrator delegate callbacks (willPresent /
            // didPresent) synchronously inside advance(), after the queue has already claimed
            // the slot. A throw here would leave `showing` set forever and stall every later
            // in-app — one slot serves all templates. Release it, same as InAppActivityChannel
            // does for a failed launch. Throwable, not Exception: a TODO()/assert in show() is
            // an Error, which BackgroundExecutor.main already swallows without a crash — so
            // narrowing this to Exception would only strand the slot, not surface the fault.
            PWLog.error(TAG, "Failed to present in-app overlay", e)
            queueManager.onDismissed(message.id)
            return
        }
        if (shown) {
            queueManager.onPresentConfirmed()
        } else {
            // show() returns false only when there is no host Activity yet (topActivity null
            // between screens while still foregrounded) — an un-presentable layout throws instead
            // (caught above), so false can never mean "unknown layout", it is always this transient
            // state. Defer (keep at the head of the queue until the next Activity resume re-pumps)
            // instead of dropping, so a burst flushed in this window is not lost — and nothing
            // spins while topActivity stays null (a paused Activity under a permission dialog
            // holds that state for minutes). A genuine failure throws and is dropped in the catch
            // above.
            queueManager.onPresentDeferred(message)
        }
    }

    private companion object {
        const val TAG = "InAppOverlayChannel"
    }
}
