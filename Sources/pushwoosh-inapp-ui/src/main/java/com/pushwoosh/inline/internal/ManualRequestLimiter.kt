package com.pushwoosh.inline.internal

/** Token bucket for manual slot requests: 5 at once, then one per 3 minutes. */
internal class ManualRequestLimiter(private val clock: InlineClock) {
    private var tokens = CAPACITY
    private var refilledAt = clock.nowMillis()

    fun take(): Boolean {
        val now = clock.nowMillis()
        val refills = ((now - refilledAt) / REFILL_INTERVAL_MS).coerceAtLeast(0)
        if (refills > 0) {
            tokens = minOf(CAPACITY.toLong(), tokens + refills).toInt()
            refilledAt += refills * REFILL_INTERVAL_MS
        }
        if (tokens == 0) return false
        if (tokens == CAPACITY) refilledAt = now
        tokens -= 1
        return true
    }

    fun reset() {
        tokens = CAPACITY
        refilledAt = clock.nowMillis()
    }

    private companion object {
        const val CAPACITY = 5
        const val REFILL_INTERVAL_MS = 180_000L
    }
}
