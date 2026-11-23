package com.pushwoosh.inline.internal.net

import com.pushwoosh.inline.internal.FakeClock
import com.pushwoosh.inline.internal.FakeScheduler
import com.pushwoosh.internal.network.ConnectionException
import com.pushwoosh.internal.network.NetworkException
import com.pushwoosh.internal.network.RequestBlockedException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InlineEventQueueTest {
    @get:Rule
    val folder = TemporaryFolder()

    private class FakeSender : InlineEventSender {
        val sent = mutableListOf<Pair<String, String?>>()
        val script = ArrayDeque<NetworkException?>()

        override fun send(event: JSONObject, root: String?, callback: (NetworkException?) -> Unit) {
            sent += event.getString("eventId") to root
            callback(if (script.isEmpty()) null else script.removeAt(0))
        }
    }

    private val clock = FakeClock(1_000_000L)
    private val scheduler = FakeScheduler(clock)
    private val sender = FakeSender()
    private var online = true
    private var root: String? = "https://a.example/json/1.3/"
    private val lost = ConnectionException("lost", 0, 0)
    private var deviceRemoved = false
    private var writes = 0
    private val defaultFile get() = java.io.File(folder.root, "inline-events.json")

    private fun queue(file: java.io.File = defaultFile) =
        InlineEventQueue(file, sender, clock, scheduler, { online }, { root }, { writes++; it.run() }, { deviceRemoved })

    private fun queuedIds(file: java.io.File = defaultFile): List<String> {
        if (!file.exists()) return emptyList()
        val json = org.json.JSONArray(file.readText())
        return List(json.length()) { json.getJSONObject(it).getJSONObject("event").getString("eventId") }
    }

    private fun event(id: String) = JSONObject().put("eventId", id)

    // Verifies that an offline event waits for the network without spending attempts and is sent on flush.
    @Test
    fun testOfflineEventWaitsForNetworkWithoutSpendingAttempts() {
        val queue = queue()
        online = false
        var delivered: Boolean? = null

        queue.send(event("e1")) { delivered = it }
        scheduler.advance(24 * 3_600_000L)
        queue.flush()

        assertEquals(false, delivered)
        assertTrue(sender.sent.isEmpty())

        online = true
        sender.script += lost
        sender.script += lost
        queue.flush()
        scheduler.advance(70_000)
        scheduler.advance(70_000)
        assertEquals(3, sender.sent.size)
    }

    // Verifies that a network-back flush sends parked events while the connectivity state still reads offline, and parks them again when that send is lost.
    @Test
    fun testNetworkBackFlushSendsWhileStateStillOffline() {
        val queue = queue()
        online = false
        queue.send(event("e1")) {}

        sender.script += lost
        queue.flush(networkBack = true)
        assertEquals(1, sender.sent.size)

        queue.flush()
        assertEquals(1, sender.sent.size)

        queue.flush(networkBack = true)
        assertEquals(2, sender.sent.size)
        queue.flush(networkBack = true)
        assertEquals(2, sender.sent.size)
    }

    // Verifies that a lost connection (transient 0/0) spends an attempt and is retried after the backoff, then dropped after 3 attempts.
    @Test
    fun testTransientFailureRetriesThreeTimesThenDrops() {
        val queue = queue()
        sender.script += lost
        sender.script += lost
        sender.script += lost

        queue.send(event("e1")) {}
        assertEquals(1, sender.sent.size)
        scheduler.advance(10_000)
        assertEquals(1, sender.sent.size)
        scheduler.advance(30_000)
        assertEquals(2, sender.sent.size)
        scheduler.advance(70_000)
        assertEquals(3, sender.sent.size)
        scheduler.advance(7 * 24 * 3_600_000L)
        queue.flush()
        assertEquals(3, sender.sent.size)
    }

    // Verifies that a non-retriable failure (400) drops the event and reports not delivered.
    @Test
    fun testNonRetriableFailureDropsEvent() {
        val queue = queue()
        sender.script += ConnectionException("bad", 400, 0)
        var delivered: Boolean? = null

        queue.send(event("e1")) { delivered = it }
        scheduler.advance(24 * 3_600_000L)
        queue.flush()

        assertEquals(false, delivered)
        assertEquals(1, sender.sent.size)
    }

    // Verifies that a queued event keeps the root of its first attempt after the app root moved.
    @Test
    fun testQueuedEventKeepsItsRoot() {
        val queue = queue()
        sender.script += ConnectionException("busy", 503, 0)

        queue.send(event("e1")) {}
        root = "https://b.example/json/1.3/"
        scheduler.advance(40_000)

        assertEquals(listOf("e1" to "https://a.example/json/1.3/", "e1" to "https://a.example/json/1.3/"), sender.sent)
    }

    // Verifies that a queued event delivered after relaunch is announced through onQueuedDelivered.
    @Test
    fun testQueuedEventDeliveredAfterRelaunchIsAnnounced() {
        val file = java.io.File(folder.root, "inline-events.json")
        sender.script += lost
        queue(file).send(event("e1")) {}

        val relaunched = queue(file)
        val announced = mutableListOf<String>()
        relaunched.onQueuedDelivered = { announced += it.getString("eventId") }
        clock.nowMillis += 60_000
        relaunched.flush()

        assertEquals(listOf("e1"), announced)
        assertEquals("https://a.example/json/1.3/", sender.sent.last().second)
    }

    // Verifies that the queue holds at most 100 events, dropping the oldest.
    @Test
    fun testQueueCappedAtHundredDroppingTheOldest() {
        val queue = queue()
        online = false
        repeat(101) { queue.send(event("e$it")) {} }
        online = true
        queue.flush()

        assertEquals(100, sender.sent.size)
        assertFalse(sender.sent.any { it.first == "e0" })
        assertTrue(sender.sent.any { it.first == "e100" })
    }

    // Verifies that an entry older than 3 days is dropped without sending.
    @Test
    fun testExpiredEntryIsDropped() {
        val queue = queue()
        online = false
        queue.send(event("e1")) {}
        clock.nowMillis += 3 * 24 * 3_600_000L
        online = true
        queue.flush()

        assertTrue(sender.sent.isEmpty())
    }

    // Verifies that a first-attempt success reports delivered and announces nothing.
    @Test
    fun testImmediateDeliveryIsNotAnnounced() {
        val queue = queue()
        var announced = false
        queue.onQueuedDelivered = { announced = true }
        var delivered: Boolean? = null

        queue.send(event("e1")) { delivered = it }

        assertEquals(true, delivered)
        assertFalse(announced)
    }

    // Verifies that a persisted event survives a not-ready SDK without spending attempts and is delivered by a later flush.
    @Test
    fun testNotReadySdkKeepsPersistedEventWithoutSpendingAttempts() {
        val file = java.io.File(folder.root, "inline-events.json")
        online = false
        queue(file).send(event("e1")) {}
        online = true

        val relaunched = queue(file)
        val announced = mutableListOf<String>()
        relaunched.onQueuedDelivered = { announced += it.getString("eventId") }
        repeat(5) { sender.script += NetworkException("SDK is not initialized") }
        relaunched.flush()
        repeat(4) { scheduler.advance(30_000) }
        assertEquals(5, sender.sent.size)
        assertTrue(announced.isEmpty())

        scheduler.advance(30_000)

        assertEquals(6, sender.sent.size)
        assertEquals(listOf("e1"), announced)
    }

    // Verifies that an event refused while server communication is blocked stays in the persistent queue without spending attempts and is delivered once communication is allowed, also after a relaunch.
    @Test
    fun testBlockedCommunicationKeepsEventUntilAllowed() {
        val file = java.io.File(folder.root, "inline-events.json")
        val queue = queue(file)
        repeat(5) { sender.script += RequestBlockedException("Server communication is stopped") }
        var delivered: Boolean? = null

        queue.send(event("e1")) { delivered = it }
        assertEquals(false, delivered)
        repeat(4) { queue.flush() }
        assertEquals(5, sender.sent.size)

        val relaunched = queue(file)
        val announced = mutableListOf<String>()
        relaunched.onQueuedDelivered = { announced += it.getString("eventId") }
        clock.nowMillis += 30_000
        relaunched.flush()

        assertEquals(listOf("e1"), announced)
        assertEquals(6, sender.sent.size)
    }

    // Verifies that blocked events do not wake the queue up: they wait for an outside flush, and a flush pass refused for every entry rewrites no file.
    @Test
    fun testBlockedEventsWaitForOutsideFlushWithoutPolling() {
        val queue = queue()
        val announced = mutableListOf<String>()
        queue.onQueuedDelivered = { announced += it.getString("eventId") }
        repeat(3) { sender.script += RequestBlockedException("Server communication stopped") }
        repeat(3) { queue.send(event("e$it")) {} }
        assertEquals(listOf("e0", "e1", "e2"), queuedIds())

        scheduler.advance(24 * 3_600_000L)
        assertEquals(3, sender.sent.size)
        assertEquals(0, scheduler.pendingCount)

        val writesBefore = writes
        repeat(3) { sender.script += RequestBlockedException("Server communication stopped") }
        queue.flush()
        assertEquals(6, sender.sent.size)
        assertTrue(writes - writesBefore <= 1)
        assertEquals(0, scheduler.pendingCount)

        queue.flush()
        assertEquals(listOf("e0", "e1", "e2"), announced)
        assertEquals(emptyList<String>(), queuedIds())
    }

    // Verifies that a bare not-initialised failure still holds the event for 25 s, while a blocked one next to it is not retried by that timer.
    @Test
    fun testNotReadyHoldTimerDoesNotRetryBlockedEvents() {
        val queue = queue()
        sender.script += RequestBlockedException("Server communication stopped")
        sender.script += NetworkException("SDK is not initialized")
        queue.send(event("blocked")) {}
        queue.send(event("early")) {}

        scheduler.advance(30_000)

        assertEquals(listOf("blocked", "early", "early"), sender.sent.map { it.first })
    }

    // Verifies that removing device data deletes the queued events from memory and file, and an event blocked because the device was removed is dropped, not held.
    @Test
    fun testDeviceDataRemovalDeletesQueuedEvents() {
        val queue = queue()
        online = false
        queue.send(event("e1")) {}
        queue.send(event("e2")) {}
        assertEquals(listOf("e1", "e2"), queuedIds())

        queue.clear()
        assertEquals(emptyList<String>(), queuedIds())
        online = true
        queue.flush()
        queue(defaultFile).flush()
        assertTrue(sender.sent.isEmpty())

        deviceRemoved = true
        sender.script += RequestBlockedException("Device data was removed")
        var delivered: Boolean? = null
        queue.send(event("e3")) { delivered = it }
        assertEquals(false, delivered)
        assertEquals(emptyList<String>(), queuedIds())
        queue.flush()
        assertEquals(listOf("e3"), sender.sent.map { it.first })
    }
}
