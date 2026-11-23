package com.pushwoosh.inline.internal.html

import com.pushwoosh.inline.internal.Cancelable
import com.pushwoosh.inline.internal.InlineScheduler

/**
 * Turns taps on an HTML banner and the page's close calls into clicks and a dismiss.
 *
 * A tap is answered by a hit test of the page: outside any control it is a tap on the banner
 * ([onBannerTap], `null` = the banner has no action); on an element marked with `data-pw-id` it is
 * a click with that id; on an unmarked control (a link, a button...) it is a click without an id,
 * reported [OWN_CLICK_DELAY_MS] later unless the page closes the banner meanwhile: then that control
 * was the close button and only the dismiss is sent. A close during a tap waits for the hit test.
 * Main thread only.
 */
internal class InlineHtmlTapCoordinator(
    private val scheduler: InlineScheduler,
    private val onClick: (elementId: String?) -> Unit,
    private val onDismiss: () -> Unit,
    private val onBannerTap: (() -> Unit)?,
) {
    /** What a tap hit on the page. */
    enum class Kind { NONE, MARKED, CONTROL }

    private var tapPending = false
    private var closeRequested = false
    private var closingActionRan = false
    private var pendingClick: Cancelable? = null
    private var pendingNavigation: (() -> Unit)? = null

    /** A tap on the page began; its hit test answers through [onTapResult]. */
    fun onTap() {
        flush()
        tapPending = true
        closeRequested = false
        closingActionRan = false
    }

    /** [kind] `null` = the page did not answer; [id] is the `data-pw-id` of a [Kind.MARKED] hit. */
    fun onTapResult(kind: Kind?, id: String?) {
        if (!tapPending) return
        tapPending = false
        val elementId = id?.takeIf { kind == Kind.MARKED && it.isNotEmpty() }
        if (closeRequested) {
            closeRequested = false
            if (elementId != null || closingActionRan) onClick(elementId)
            onDismiss()
        } else when (kind) {
            null -> Unit
            Kind.NONE -> onBannerTap?.invoke()
            Kind.MARKED -> onClick(elementId)
            Kind.CONTROL -> pendingClick = scheduler.postDelayed(OWN_CLICK_DELAY_MS) { flush() }
        }
        val navigate = pendingNavigation
        pendingNavigation = null
        if (navigate != null) onExternalNavigation(navigate)
    }

    /** Records the tap before navigation can remove its page, preserving the hit element's id. */
    fun onExternalNavigation(navigate: () -> Unit) {
        if (tapPending) {
            pendingNavigation = navigate
            return
        }
        flush()
        navigate()
    }

    /** The page closes the banner (`closeInApp`, `pushwoosh://close`). */
    fun onCloseInApp() {
        val click = pendingClick
        when {
            click != null -> {
                click.cancel()
                pendingClick = null
                if (closingActionRan) onClick(null)
                onDismiss()
            }
            tapPending -> closeRequested = true
            else -> onDismiss()
        }
    }

    /** The page ran an action that closes the banner on its own; a tap that triggered it counts as a click. */
    fun onClosingAction() {
        if (tapPending || pendingClick != null) closingActionRan = true
    }

    /** Reports a held-back control click now: on the next tap and when the banner leaves the window. */
    fun flush() {
        val click = pendingClick ?: return
        click.cancel()
        pendingClick = null
        onClick(null)
    }

    private companion object {
        const val OWN_CLICK_DELAY_MS = 1_000L
    }
}
