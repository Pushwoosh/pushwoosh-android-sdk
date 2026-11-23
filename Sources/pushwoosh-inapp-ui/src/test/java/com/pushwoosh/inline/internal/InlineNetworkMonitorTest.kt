package com.pushwoosh.inline.internal

import com.pushwoosh.inline.internal.net.InlineEventQueue
import com.pushwoosh.inline.internal.net.InlineEventSender
import com.pushwoosh.internal.network.ConnectionException
import com.pushwoosh.internal.network.NetworkException
import com.pushwoosh.internal.platform.utils.GeneralUtils
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InlineNetworkMonitorTest {
    @get:Rule
    val folder = TemporaryFolder()

    private class FakeSender : InlineEventSender {
        val sent = mutableListOf<String>()
        val script = ArrayDeque<NetworkException?>()

        override fun send(event: JSONObject, root: String?, callback: (NetworkException?) -> Unit) {
            sent += event.getString("eventId")
            callback(if (script.isEmpty()) null else script.removeAt(0))
        }
    }

    private val clock = FakeClock(1_000_000L)
    private val scheduler = FakeScheduler(clock)
    private val sender = FakeSender()
    private lateinit var utils: MockedStatic<GeneralUtils>

    private val monitor by lazy { InlineNetworkMonitor(RuntimeEnvironment.getApplication()) {} }

    private fun queue() = InlineEventQueue(
        File(folder.root, "inline-events.json"), sender, clock, scheduler, monitor::isOnline, { "https://a.example/json/1.3/" }, { it.run() }, { false },
    )

    @Before
    fun setUp() {
        utils = Mockito.mockStatic(GeneralUtils::class.java)
        utils.`when`<Boolean> { GeneralUtils.isNetworkAvailable() }.thenThrow(SecurityException("ACCESS_NETWORK_STATE"))
    }

    @After
    fun tearDown() {
        utils.close()
    }

    // Verifies that without ACCESS_NETWORK_STATE connectivity reads as unknown, and flushing the queue and sending an event never throw and the event is sent.
    @Test
    fun testUnreadableConnectivityNeverThrowsAndSendsEvent() {
        monitor.start()
        assertNull(monitor.isOnline())
        val queue = queue()
        var delivered: Boolean? = null

        queue.flush()
        queue.send(JSONObject().put("eventId", "e1")) { delivered = it }

        assertEquals(listOf("e1"), sender.sent)
        assertEquals(true, delivered)
        monitor.stop()
    }

    // Verifies that without ACCESS_NETWORK_STATE an event whose request is lost stays queued and is retried after the backoff.
    @Test
    fun testUnreadableConnectivityKeepsFailedEventForRetry() {
        val queue = queue()
        val announced = mutableListOf<String>()
        queue.onQueuedDelivered = { announced += it.getString("eventId") }
        sender.script += ConnectionException("lost", 0, 0)
        var delivered: Boolean? = null

        queue.send(JSONObject().put("eventId", "e1")) { delivered = it }
        assertEquals(false, delivered)
        scheduler.advance(40_000)

        assertEquals(listOf("e1", "e1"), sender.sent)
        assertEquals(listOf("e1"), announced)
    }

    // Verifies that without ACCESS_NETWORK_STATE lost requests spend no attempts: the event is retried with a growing backoff past the 3-attempt budget and then delivered.
    @Test
    fun testUnreadableConnectivityRetriesLostEventBeyondAttemptBudget() {
        val queue = queue()
        val announced = mutableListOf<String>()
        queue.onQueuedDelivered = { announced += it.getString("eventId") }
        repeat(5) { sender.script += ConnectionException("lost", 0, 0) }

        queue.send(JSONObject().put("eventId", "e1")) {}
        val delays = mutableListOf<Long>()
        repeat(5) {
            delays += checkNotNull(scheduler.nextDelayMs)
            scheduler.advance(delays.last())
        }

        assertEquals(List(6) { "e1" }, sender.sent)
        assertEquals(listOf("e1"), announced)
        assertTrue(delays.last() > delays.first())
    }
}
