package com.pushwoosh.pushmart.ui.richmedia

class FakeDelayedRunner : DelayedRunner {
    val scheduled = mutableListOf<Pair<Long, () -> Unit>>()
    val cancelled = mutableSetOf<Int>()

    override fun runLater(delayMs: Long, action: () -> Unit): () -> Unit {
        val index = scheduled.size
        scheduled.add(delayMs to action)
        return { cancelled.add(index) }
    }

    /**
     * Fires the scheduled action at [index] unless it was cancelled, like the real handler would.
     */
    fun fire(index: Int) {
        if (index !in cancelled) scheduled[index].second()
    }
}
