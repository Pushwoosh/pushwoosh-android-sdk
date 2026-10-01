package com.pushwoosh.pushmart.ui.richmedia

import android.os.Handler
import androidx.annotation.MainThread

/** Schedules [action] after [delayMs]; the returned function cancels it. Faked in unit tests. */
fun interface DelayedRunner {
    @MainThread fun runLater(delayMs: Long, action: () -> Unit): () -> Unit
}

/** [handler] must be bound to the main looper; [action] then runs on the main thread too. */
class HandlerDelayedRunner(private val handler: Handler) : DelayedRunner {
    override fun runLater(delayMs: Long, action: () -> Unit): () -> Unit {
        val runnable = Runnable { action() }
        handler.postDelayed(runnable, delayMs)
        return { handler.removeCallbacks(runnable) }
    }
}
