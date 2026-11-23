package com.pushwoosh.inline.internal

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualRequestLimiterTest {
    private val clock = FakeClock()
    private val limiter = ManualRequestLimiter(clock)

    // Verifies that the bucket gives 5, refuses the 6th, refuses at +179 s, gives one at +180 s and refuses again, and is full after reset.
    @Test
    fun testFiveThenOnePerThreeMinutes() {
        repeat(5) { assertTrue(limiter.take()) }
        assertFalse(limiter.take())
        clock.advance(179_000)
        assertFalse(limiter.take())
        clock.advance(1_000)
        assertTrue(limiter.take())
        assertFalse(limiter.take())
        limiter.reset()
        repeat(5) { assertTrue(limiter.take()) }
    }

    // Verifies that the refill clock starts when the bucket leaves full, not when the limiter was created.
    @Test
    fun testRefillClockStartsWhenBucketLeavesFull() {
        clock.advance(10 * 60_000)
        repeat(5) { assertTrue(limiter.take()) }
        clock.advance(179_000)
        assertFalse(limiter.take())
        clock.advance(1_000)
        assertTrue(limiter.take())
    }

    private fun FakeClock.advance(ms: Long) {
        nowMillis += ms
    }
}
