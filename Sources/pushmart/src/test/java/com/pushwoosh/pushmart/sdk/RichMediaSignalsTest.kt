package com.pushwoosh.pushmart.sdk

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class RichMediaSignalsTest {
    private val received = mutableListOf<RichMediaSignals.Signal>()

    @After
    fun clearListener() {
        RichMediaSignals.listener = null
    }

    @Test
    fun publishWithoutListenerIsDropped() {
        RichMediaSignals.publish(RichMediaSignals.Signal.Presented("rm-1"))
    }

    @Test
    fun listenerReceivesSignalsInOrder() {
        RichMediaSignals.listener = { received += it }

        RichMediaSignals.publish(RichMediaSignals.Signal.Presented("rm-1"))
        RichMediaSignals.publish(RichMediaSignals.Signal.Failed("rm-2", "boom"))

        assertEquals(
            listOf(
                RichMediaSignals.Signal.Presented("rm-1"),
                RichMediaSignals.Signal.Failed("rm-2", "boom")),
            received)
    }

    @Test
    fun listenerIsCalledSynchronously() {
        RichMediaSignals.listener = { received += it }

        RichMediaSignals.publish(RichMediaSignals.Signal.Presented("rm-1"))

        assertEquals(1, received.size)
    }

    @Test
    fun clearedListenerReceivesNothing() {
        RichMediaSignals.listener = { received += it }
        RichMediaSignals.listener = null

        RichMediaSignals.publish(RichMediaSignals.Signal.Presented("rm-1"))

        assertEquals(emptyList<RichMediaSignals.Signal>(), received)
    }

    @Test
    fun repeatedSignalsAreNotCoalesced() {
        RichMediaSignals.listener = { received += it }

        RichMediaSignals.publish(RichMediaSignals.Signal.Presented("rm-1"))
        RichMediaSignals.publish(RichMediaSignals.Signal.Presented("rm-1"))

        assertEquals(2, received.size)
    }

    @Test
    fun nothingIsReplayedToALateListener() {
        RichMediaSignals.publish(RichMediaSignals.Signal.Failed("rm-1", "boom"))

        RichMediaSignals.listener = { received += it }

        assertEquals(emptyList<RichMediaSignals.Signal>(), received)
    }
}
