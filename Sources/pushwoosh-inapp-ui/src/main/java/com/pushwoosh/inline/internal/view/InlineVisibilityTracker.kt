package com.pushwoosh.inline.internal.view

import com.pushwoosh.inline.internal.Cancelable
import com.pushwoosh.inline.internal.InlineScheduler

/** What a slot view looks like on screen at one moment. */
internal data class VisibilitySample(
    /** The app is in the foreground. */
    val foreground: Boolean,
    /** The slot is attached, shown and its window has focus: no dialog or other activity is over it. */
    val frontmost: Boolean,
    /** Visible height / full height of the slot; `null` = the slot has no height. */
    val visibleShare: Float?,
    /** The slot's top line lies inside what is visible of its parent. */
    val lineVisible: Boolean,
    /** The content can count as shown (an HTML document has reported its height). */
    val contentReady: Boolean,
)

/**
 * Calls [onVisible] once the slot passes the impression rule for [HOLD_MS] without a break: app in
 * the foreground, slot frontmost, content ready, and at least half of its height visible (a control
 * slot: its line visible). Samples every [COUNTING_CHECK_MS] while counting and every [IDLE_CHECK_MS]
 * while only the slot's place on screen fails, plus on every [recheck]. While the app is in the
 * background, the slot is not frontmost or its content is not ready it does not sample until the next
 * [recheck]. Stops after [onVisible]. Main thread only.
 */
internal class InlineVisibilityTracker(
    private val probe: () -> VisibilitySample,
    private val isControl: () -> Boolean,
    private val scheduler: InlineScheduler,
    private val onVisible: () -> Unit,
) {
    private var running = false
    private var check: Cancelable? = null
    private var deadline: Cancelable? = null

    fun start() {
        if (running) return
        running = true
        evaluate()
    }

    fun stop() {
        running = false
        check?.cancel()
        check = null
        deadline?.cancel()
        deadline = null
    }

    /** Samples now: something that changes visibility happened (scroll, layout, focus, content ready). */
    fun recheck() {
        if (running) evaluate()
    }

    private fun evaluate() {
        check?.cancel()
        check = null
        val sample = probe()
        val passes = passes(sample)
        if (passes && deadline == null) {
            deadline = scheduler.postDelayed(HOLD_MS) { fire() }
        } else if (!passes) {
            deadline?.cancel()
            deadline = null
        }
        if (!isEligible(sample)) return
        check = scheduler.postDelayed(if (passes) COUNTING_CHECK_MS else IDLE_CHECK_MS) { evaluate() }
    }

    private fun fire() {
        deadline = null
        if (!passes(probe())) {
            evaluate()
            return
        }
        stop()
        onVisible()
    }

    /** The app, window and content state let the slot count; only these come with a [recheck] when they change. */
    private fun isEligible(sample: VisibilitySample): Boolean = sample.foreground && sample.frontmost && sample.contentReady

    private fun passes(sample: VisibilitySample): Boolean {
        if (!isEligible(sample)) return false
        return if (isControl()) sample.lineVisible else (sample.visibleShare ?: 0f) >= MIN_VISIBLE_SHARE
    }

    private companion object {
        const val HOLD_MS = 300L
        const val COUNTING_CHECK_MS = 100L
        const val IDLE_CHECK_MS = 300L
        const val MIN_VISIBLE_SHARE = 0.5f
    }
}
