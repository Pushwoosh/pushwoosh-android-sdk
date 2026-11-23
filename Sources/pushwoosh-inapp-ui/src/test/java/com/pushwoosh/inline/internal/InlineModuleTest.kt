package com.pushwoosh.inline.internal

import android.os.Looper
import com.pushwoosh.inapp.inline.InlineContentHandlerProvider
import com.pushwoosh.inline.PushwooshInline
import com.pushwoosh.inline.PushwooshInlineException
import com.pushwoosh.inline.internal.net.InlineEventQueue
import com.pushwoosh.inline.internal.net.InlineEventSender
import com.pushwoosh.inline.internal.store.InlineStore
import com.pushwoosh.internal.event.AppIdChangedEvent
import com.pushwoosh.internal.event.DeviceDataRemovedEvent
import com.pushwoosh.internal.event.EventBus
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent
import com.pushwoosh.internal.event.UserIdChangeRefusedEvent
import com.pushwoosh.internal.event.UserIdUpdatedEvent
import com.pushwoosh.internal.network.NetworkException
import com.pushwoosh.internal.network.RequestBlockedException
import com.pushwoosh.internal.platform.ApplicationOpenDetector
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper
import java.io.File
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InlineModuleTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var clock: FakeClock
    private lateinit var scheduler: FakeScheduler
    private lateinit var identity: FakeIdentity
    private lateinit var transport: FakeTransport
    private lateinit var events: FakeEventSink
    private lateinit var store: InlineStore
    private lateinit var manager: InlineManager

    @Before
    fun setUp() {
        clock = FakeClock(1_800_000_000_123L)
        scheduler = FakeScheduler(clock)
        identity = FakeIdentity()
        transport = FakeTransport()
        events = FakeEventSink()
        store = InlineStore(File(folder.root, "pushwoosh/inline-banners.json"), clock, scheduler, Executor { it.run() })
        manager = InlineManager(store, transport, events, identity, clock, scheduler)
        InlineModule.resetForTest(manager)
        InlineModule.install()
    }

    @After
    fun tearDown() {
        ShadowLooper.unPauseMainLooper()
        InlineModule.resetForTest(null)
    }

    private fun send(event: com.pushwoosh.internal.event.Event) {
        EventBus.sendEvent(event)
        ShadowLooper.idleMainLooper()
    }

    private fun slots(vararg banners: Pair<String, JSONObject>): JSONObject =
        JSONObject().apply { for ((slotId, banner) in banners) put(slotId, banner) }

    private fun runOffMain(block: () -> Unit): Boolean {
        val thread = Thread(block)
        thread.start()
        thread.join(1_000)
        return !thread.isAlive
    }

    // Verifies that core lifecycle events reach the manager: foreground starts a session, an app code change clears, a confirmed user requests again, a refusal requests held slots.
    @Test
    fun testCoreEventsReachTheManager() {
        store.load(identity.owner())
        store.rememberSlots(listOf("a"))

        send(ApplicationOpenDetector.ApplicationMovedToForegroundEvent())
        assertEquals(listOf(listOf("a")), transport.calls)
        transport.answer(0, slots("a" to nativeBanner("b1")))
        assertEquals("b1", manager.banner("a")?.bannerId)

        identity.appCode = "APP-2"
        send(AppIdChangedEvent("APP-2", "APP-1"))
        assertNull(manager.banner("a"))
        assertEquals(2, transport.calls.size)
        transport.answer(1, slots("a" to nativeBanner("b2")), "u1", "APP-2")

        identity.userId = "u2"
        assertTrue(manager.isUserChangeUnconfirmed())
        send(UserIdUpdatedEvent())
        assertFalse(manager.isUserChangeUnconfirmed())
        assertEquals(listOf("a"), transport.calls[2])
        transport.answer(2, slots("a" to nativeBanner("b3")), "u2", "APP-2")

        identity.userId = "u3"
        manager.slotDidAppear("b")
        scheduler.advance(500)
        assertEquals(3, transport.calls.size)
        identity.userId = "u2"
        send(UserIdChangeRefusedEvent())
        assertEquals(listOf("b"), transport.calls[3])
        assertEquals("b3", manager.banner("a")?.bannerId)
    }

    // Verifies that the postEvent seam applies inline slots through the module.
    @Test
    fun testPostEventSeamAppliesEventSlots() {
        val handler = InlineContentHandlerProvider.get()!!

        handler.onInlineContent(JSONObject().put("slots", slots("a" to nativeBanner("b1"))), "u1", "APP-1")

        assertEquals("b1", PushwooshInline.getBanner("a")?.bannerId)
        assertTrue(transport.calls.isEmpty())
    }

    // Verifies that void calls from a background thread return without waiting for the main thread.
    @Test
    fun testCallsWithoutResultDoNotWaitForMainThread() {
        manager.onEventAnswer(JSONObject().put("slots", slots("a" to nativeBanner("b1"))), "u1", "APP-1")
        ShadowLooper.pauseMainLooper()

        val finished = runOffMain {
            PushwooshInline.requestSlots(listOf("x"), null)
            PushwooshInline.trackClick("a", "b1", "button")
            PushwooshInline.dismiss("a", "b1")
        }

        assertTrue(finished)
        assertTrue(transport.calls.isEmpty())
        assertTrue(events.events.isEmpty())

        ShadowLooper.unPauseMainLooper()
        ShadowLooper.idleMainLooper()
        assertEquals(listOf(listOf("x")), transport.calls)
        assertEquals(listOf("click", "dismiss"), events.events.map { it.getString("type") })
        assertNull(manager.banner("a"))
    }

    // Verifies that requestSlots delivers its callback on the main thread when called off main.
    @Test
    fun testRequestCallbackOnMainThread() {
        val results = mutableListOf<Pair<Thread, Int?>>()

        assertTrue(runOffMain { PushwooshInline.requestSlots(emptyList()) { error -> results += Thread.currentThread() to error?.code } })
        ShadowLooper.idleMainLooper()
        assertTrue(runOffMain { PushwooshInline.requestSlots(listOf("a")) { error -> results += Thread.currentThread() to error?.code } })
        ShadowLooper.idleMainLooper()
        transport.answer(0, slots("a" to nativeBanner("b1")))

        val main = Looper.getMainLooper().thread
        assertEquals(listOf(main to PushwooshInlineException.INVALID_ARGUMENT, main to null), results)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> fromJava(value: Any?): T = value as T

    // Verifies that a Java caller passing a null slot id or a null list gets INVALID_ARGUMENT once on the main thread, with no request, no remembered slot and no manual token spent.
    @Test
    fun testNullSlotIdsFromJavaAreInvalidArgument() {
        val results = mutableListOf<Pair<Thread, Int?>>()
        val main = Looper.getMainLooper().thread
        val invalid = main to PushwooshInlineException.INVALID_ARGUMENT

        PushwooshInline.requestSlots(fromJava(java.util.Arrays.asList("home", null))) { results += Thread.currentThread() to it?.code }
        PushwooshInline.requestSlots(fromJava(null)) { results += Thread.currentThread() to it?.code }
        assertEquals(listOf(invalid, invalid), results)

        assertTrue(runOffMain { PushwooshInline.requestSlots(fromJava(java.util.Arrays.asList(null, "home"))) { results += Thread.currentThread() to it?.code } })
        assertTrue(runOffMain { PushwooshInline.requestSlots(fromJava(null)) { results += Thread.currentThread() to it?.code } })
        ShadowLooper.idleMainLooper()
        assertEquals(listOf(invalid, invalid, invalid, invalid), results)

        assertTrue(transport.calls.isEmpty())
        assertTrue(store.knownSlots().isEmpty())
        repeat(5) { PushwooshInline.requestSlots(listOf("a"), null) }
        assertEquals(5, transport.calls.size)
    }

    // Verifies that core starting server communication sends the events it blocked, and a device data removal deletes the queued events for good.
    @Test
    fun testServerCommunicationStartAndDeviceRemovalReachTheQueue() {
        var allowed = false
        val sent = mutableListOf<String>()
        val sender = object : InlineEventSender {
            override fun send(event: JSONObject, root: String?, callback: (NetworkException?) -> Unit) {
                sent += event.getString("eventId")
                callback(if (allowed) null else RequestBlockedException("Server communication stopped"))
            }
        }
        val file = File(folder.root, "pushwoosh/inline-events.json")
        val queue = InlineEventQueue(file, sender, clock, scheduler, { true }, { null }, { it.run() }, { false })
        InlineModule.resetForTest(manager, queue)
        InlineModule.install()

        queue.send(JSONObject().put("eventId", "e1")) {}
        allowed = true
        send(ServerCommunicationStartedEvent())
        assertEquals(listOf("e1", "e1"), sent)

        allowed = false
        queue.send(JSONObject().put("eventId", "e2")) {}
        send(DeviceDataRemovedEvent())
        assertEquals("[]", file.readText())
        allowed = true
        send(ServerCommunicationStartedEvent())
        assertEquals(listOf("e1", "e1", "e2"), sent)
    }
}
