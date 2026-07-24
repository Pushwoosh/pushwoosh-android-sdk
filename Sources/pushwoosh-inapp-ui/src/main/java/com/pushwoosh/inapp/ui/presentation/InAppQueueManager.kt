package com.pushwoosh.inapp.ui.presentation

import com.pushwoosh.inapp.ui.InAppMessageDelegate
import com.pushwoosh.inapp.ui.model.InAppMessage
import com.pushwoosh.internal.utils.BackgroundExecutor

/**
 * The mechanism that actually shows a message (Activity launch / overlay). Wired at init.
 *
 * Contract: [present] claims the queue's single slot. An implementation that begins a
 * presentation and then fails to reach the screen MUST release the slot via
 * [InAppQueueManager.onDismissed] with the message id, or the slot stays set and every later
 * in-app stalls. A pure dispatcher ([InAppRoutingChannel]) delegates this duty to the channel it
 * forwards to. Kept as a documented contract rather than a base-class template method: the two
 * leaf channels release identically but fail for different reasons (a silently-dropped background
 * startActivity vs. a missing host Activity or a throwing view build), and each keeps that
 * rationale inline next to its own catch.
 */
internal interface InAppPresentationChannel {
    fun present(message: InAppMessage)
}

/**
 * FIFO one-at-a-time queue for blocking in-app messages, mirroring iOS
 * `PushwooshInAppUI`. New messages enqueue; the next is shown when the current one is
 * dismissed.
 *
 * Never launches while backgrounded: on API 29+ a background startActivity is dropped
 * silently (no exception, no Activity, no onDestroy), which would leave [showing] set
 * forever. A launch that slipped through anyway — [foregroundProvider] lags a real
 * background transition by the detector's debounce — is caught by the confirmation
 * handshake ([onPresentConfirmed]) and re-presented on the next foreground.
 *
 * The overlay path can also stall while foregrounded: its synchronous present needs the
 * foreground Activity's decorView, which is absent whenever `topActivity` is null — a screen
 * handing off to the next, but also an Activity sitting paused under a permission dialog or in
 * PiP. That is a retryable state, reported as [onPresentDeferred] — the message is kept at the
 * head of the queue and retried on the next Activity resume ([onActivityBroughtOnTop]), not
 * dropped.
 *
 * All methods must be called on the main thread. The actual presentation is delegated to
 * [channel] so the queue logic stays UI-free and unit-testable.
 *
 * @param frequencyStore enforces opt-in display caps (count / cooldown / expiry); consulted
 *   both at enqueue and again at show time.
 * @param foregroundProvider reads the app's live foreground state at call time (not a
 *   construction-time snapshot), so [advance] can refuse to launch while backgrounded.
 *   Backed by the core SDK's lifecycle detector.
 * @param mainExecutor marshals resume work onto the main thread. Defaults to
 *   [BackgroundExecutor.main]; injected inline in unit tests. Placed before [delegateProvider]
 *   so existing trailing-lambda call sites keep binding their lambda to [delegateProvider].
 * @param delegateProvider reads the host-set [InAppMessageDelegate] at call time, so a
 *   delegate the integrator registers after this singleton is built is still honoured;
 *   returns `null` when none is set.
 */
internal class InAppQueueManager(
    private val frequencyStore: InAppFrequencyStore,
    private val foregroundProvider: () -> Boolean,
    private val mainExecutor: (Runnable) -> Unit = BackgroundExecutor::main,
    private val delegateProvider: () -> InAppMessageDelegate?
) {

    /** The presentation mechanism, wired at construction by [com.pushwoosh.inapp.ui.InAppModule];
     *  [advance] no-ops while this is `null`. */
    var channel: InAppPresentationChannel? = null

    /** When `true`, eligible messages accumulate in the queue but are not shown; clearing it
     *  drains the queue via [advance]. The flag is written synchronously on the caller's thread
     *  (iOS parity); only the resume/advance is marshalled to main, because the queue is
     *  main-only. */
    @Volatile
    var isPaused: Boolean = false
        set(value) {
            field = value
            if (!value) mainExecutor(Runnable { advance() })
        }

    private val queue = ArrayDeque<InAppMessage>()
    private var showing: InAppMessage? = null
    private var showingConfirmed = false

    /**
     * Enqueues a blocking message and attempts to show it immediately.
     *
     * The message is dropped (never queued) if a frequency cap denies it
     * ([InAppFrequencyStore.canShow]) or the same id is already showing or already waiting in
     * the queue. Otherwise it is appended and [advance] shows it when the slot is free and the
     * app is foregrounded. The host delegate's veto is asked later, at show time, in [advance]
     * (iOS parity), not here.
     */
    fun enqueueForeground(message: InAppMessage) {
        if (!frequencyStore.canShow(message)) return
        if (isDuplicate(message)) return
        queue.addLast(message)
        advance()
    }

    /**
     * Resumes the queue when the app returns to the foreground.
     *
     * Re-presents a launch the OS dropped silently while backgrounded (see the class doc),
     * then advances the queue.
     */
    fun onAppForegrounded() {
        // An unconfirmed launch at a real foreground transition means the OS dropped it
        // silently while backgrounded; retry it. The frequency cap is recorded on confirm, so
        // a dropped launch has not consumed it; willPresent fires in the Activity's onCreate
        // when the view is actually built.
        val droppedLaunch = showing.takeUnless { showingConfirmed }
        if (droppedLaunch != null) {
            channel?.present(droppedLaunch)
        }
        advance()
    }

    /**
     * Called when a launch actually reaches the screen — the Activity's `onCreate`, or a
     * successful overlay attach. Records the show against the frequency caps here, at
     * confirmation, not at slot claim in [advance]: a launch that never renders (an overlay
     * fired while `topActivity` is momentarily null between screens) must not burn a display
     * count and get dropped for good under `maxDisplays`. Idempotent — a double launch via the
     * foreground retry confirms, and records, exactly once.
     */
    fun onPresentConfirmed() {
        val confirmed = showing ?: return
        if (showingConfirmed) return
        showingConfirmed = true
        frequencyStore.recordShown(confirmed)
    }

    /**
     * Called when a synchronous overlay present could not reach the screen for a transient,
     * retryable reason: there is no host Activity yet (`topActivity` null while the app is still
     * foregrounded — between one screen's `onPause` and the next screen's `onResume`, or for as
     * long as an Activity sits paused under a permission dialog or in PiP). Unlike [onDismissed]
     * this does not drop the message — it un-claims the slot and returns the message to the head
     * of the queue: a burst of banners flushed in this window must not be drained against the
     * missing Activity and lost (the resumed Activity attaches a tick later and the queue must
     * still hold them). Deliberately no immediate re-post of the pump: the no-Activity state can
     * last minutes, and a self-post spins advance→present→defer at looper speed, polling the
     * integrator's `shouldDisplay` and the frequency store the whole time. The retry is
     * event-driven instead — the next Activity resume ([onActivityBroughtOnTop]) pumps the queue
     * exactly when a host Activity exists again.
     */
    fun onPresentDeferred(message: InAppMessage) {
        showing = null
        showingConfirmed = false
        queue.addFirst(message)
    }

    /**
     * Called on every Activity resume (core posts `ActivityBroughtOnTopEvent` right after setting
     * `topActivity`; [com.pushwoosh.inapp.ui.InAppUiPlugin] routes it here). This is the retry
     * signal for a present deferred by [onPresentDeferred]: an overlay attach can succeed now
     * that a host Activity exists. A cheap no-op when nothing is queued or a message is showing.
     */
    fun onActivityBroughtOnTop() {
        advance()
    }

    /** Called when the current message's view is dismissed, advancing the queue. */
    fun onDismissed(messageId: String?) {
        val closed = showing
        val closedId = closed?.id
        // A stale dismissal from a duplicate overlay (double launch via the foreground
        // retry) must not advance the queue past a live message. Null ids fall through:
        // the parse-failure and launch-failure paths must still release the slot.
        if (messageId != null && closedId != null && messageId != closedId) return
        val wasShown = showingConfirmed
        showing = null
        // Reset with the slot so a later stale dismissal (queue already drained) cannot read a
        // sticky `true` and re-fire didClose/onClosed.
        showingConfirmed = false
        // Only a launch that actually reached the screen (confirmed) reports didClose; a
        // dropped or failed launch releases the slot silently (iOS drops a failed present
        // with no delegate events).
        if (wasShown) delegateProvider()?.didClose(messageId ?: closedId)
        advance()
    }

    /**
     * Core pump: shows the next queued message, but only when every precondition holds —
     * not paused, no message currently showing, the queue is non-empty, the app is
     * foregrounded, the host delegate does not veto it, and the candidate still passes its
     * frequency caps (re-checked here because a same-id sibling may have shown while it
     * waited). Veto and caps are asked in iOS order (delegate, then caps); a rejection at
     * either drops the candidate for good and tries the next. On success it claims the slot
     * and hands off to [channel]; the show is recorded against the frequency caps only once
     * the launch is confirmed ([onPresentConfirmed]), not here (the delegate's `willPresent`
     * fires later too, when the view is actually built).
     */
    private fun advance() {
        if (isPaused || showing != null || queue.isEmpty()) return
        if (!foregroundProvider()) return
        val next = queue.removeFirst()
        // Delegate veto is asked here, at show time (iOS order: delegate, then caps). A vetoed
        // message is dropped for good and the next one is tried.
        if (delegateProvider()?.shouldDisplay(next.id) == false) {
            advance()
            return
        }
        // Re-check caps at show time: a sibling with the same id may have shown while
        // this one waited in the queue.
        if (!frequencyStore.canShow(next)) {
            advance()
            return
        }
        val activeChannel = channel ?: return
        showing = next
        showingConfirmed = false
        activeChannel.present(next)
    }

    private fun isDuplicate(message: InAppMessage): Boolean {
        val id = message.id ?: return false
        if (showing?.id == id) return true
        return queue.any { it.id == id }
    }
}
