package com.pushwoosh.pushmart.sdk

import androidx.annotation.MainThread

/**
 * What the rich media delegate observed; the Rich media screen turns these into user-facing
 * results.
 *
 * A bare listener slot rather than a LiveData: in the legacy display mode the rich media runs in
 * its own activity, so `onPresent` arrives while the screen that asked for it is stopped, and a
 * lifecycle-gated observer would drop it. Nothing is buffered either — a signal nobody listens for
 * is gone, never replayed on the next visit to the screen.
 *
 * The delegate callbacks come from the SDK event bus on the main thread, so [publish] calls the
 * listener inline.
 */
@MainThread
object RichMediaSignals {
    sealed class Signal {
        data class Presented(val code: String) : Signal()

        data class Failed(val code: String, val error: String) : Signal()
    }

    var listener: ((Signal) -> Unit)? = null

    fun publish(signal: Signal) {
        listener?.invoke(signal)
    }
}
