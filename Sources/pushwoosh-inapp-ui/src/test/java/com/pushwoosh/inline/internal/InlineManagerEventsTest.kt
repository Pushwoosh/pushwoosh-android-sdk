package com.pushwoosh.inline.internal

import com.pushwoosh.inline.internal.store.InlineStore
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import java.io.File
import java.util.UUID
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InlineManagerEventsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var clock: FakeClock
    private lateinit var scheduler: FakeScheduler
    private lateinit var identity: FakeIdentity
    private lateinit var transport: FakeTransport
    private lateinit var events: FakeEventSink
    private lateinit var listener: RecordingListener
    private lateinit var store: InlineStore
    private lateinit var manager: InlineManager

    @Before
    fun setUp() {
        clock = FakeClock(1_800_000_000_123L)
        scheduler = FakeScheduler(clock)
        identity = FakeIdentity()
        transport = FakeTransport()
        events = FakeEventSink()
        listener = RecordingListener()
        store = InlineStore(File(folder.root, "pushwoosh/inline-banners.json"), clock, scheduler, Executor { it.run() })
        manager = InlineManager(store, transport, events, identity, clock, scheduler)
        manager.listener = listener
        manager.onForeground()
    }

    /** Puts [banners] into their slots through an event answer of [userId], as the server would. */
    private fun putBanners(vararg banners: Pair<String, JSONObject>, userId: String? = identity.userId) {
        val slots = JSONObject().apply { for ((slotId, banner) in banners) put(slotId, banner) }
        manager.onEventAnswer(JSONObject().put("slots", slots), userId, identity.appCode)
    }

    private fun types(): List<String> = events.events.map { it.getString("type") }

    // Verifies that show is reported once per banner version, again for a higher version, and again for another user.
    @Test
    fun testShowIsReportedOncePerBannerVersionAndUser() {
        putBanners("a" to nativeBanner("b1", version = 1))

        manager.bannerDidBecomeVisible("a", "b1")
        manager.bannerDidBecomeVisible("a", "b1")
        assertEquals(listOf("show"), types())

        putBanners("a" to nativeBanner("b1", version = 2))
        manager.bannerDidBecomeVisible("a", "b1")
        assertEquals(listOf(1, 2), events.events.map { it.getInt("version") })

        identity.userId = "u2"
        manager.onUserConfirmed()
        putBanners("a" to nativeBanner("b1", version = 2), userId = "u2")
        manager.bannerDidBecomeVisible("a", "b1")
        manager.trackShown("a", "b1")

        assertEquals(listOf("u1", "u1", "u2"), events.events.map { it.getString("userId") })
        assertEquals(listOf("a" to "b1", "a" to "b1", "a" to "b1"), listener.shown)
    }

    // Verifies that a manual trackShown sends the event with the timestamp of that moment and fires onSlotShown.
    @Test
    fun testManualShowSendsEventOfThatMoment() {
        putBanners("a" to nativeBanner("b1", version = 3))
        clock.nowMillis += 61_000L

        manager.trackShown("a", "other")
        assertTrue(events.events.isEmpty())

        manager.trackShown("a", "b1")

        val event = events.events.single()
        assertEquals("show", event.getString("type"))
        assertEquals("a", event.getString("slotId"))
        assertEquals("b1", event.getString("bannerId"))
        assertEquals(3, event.getInt("version"))
        assertEquals(1_800_000_061L, event.getLong("timestamp"))
        assertFalse(event.getBoolean("test"))
        assertEquals("u1", event.getString("userId"))
        assertEquals("APP-1", event.getString("application"))
        assertFalse(event.has("elementId"))
        UUID.fromString(event.getString("eventId"))
        assertEquals(listOf("a" to "b1"), listener.shown)
    }

    // Verifies that a click carries the tapped element and a banner-level click carries none; clicks are not deduplicated.
    @Test
    fun testClickCarriesTappedElement() {
        putBanners("a" to nativeBanner("b1"))

        manager.click("a", "b1", "cta_buy")
        manager.click("a", "b1", null)

        assertEquals(listOf("click", "click"), types())
        assertEquals("cta_buy", events.events[0].getString("elementId"))
        assertFalse(events.events.last().has("elementId"))
        assertNotEquals(events.events[0].getString("eventId"), events.events[1].getString("eventId"))
        assertEquals(listOf(Triple("a", "b1", "cta_buy"), Triple("a", "b1", null)), listener.clicked)
    }

    // Verifies that a tap on a banner the slot no longer holds is not reported.
    @Test
    fun testTapOnPreviousUsersBannerIsNotClick() {
        putBanners("a" to nativeBanner("b1"))
        identity.userId = "u2"
        manager.onUserConfirmed()

        manager.click("a", "b1", "cta")
        putBanners("a" to nativeBanner("b2"), userId = "u2")
        manager.click("a", "b1", "cta")
        manager.dismiss("a", "b1")

        assertTrue(events.events.isEmpty())
        assertTrue(listener.clicked.isEmpty())
        assertTrue(listener.dismissed.isEmpty())
        assertEquals("b2", store.banner("a")?.bannerId)
    }

    // Verifies that events in the unconfirmed window carry the previous user id and canPostEvent is false.
    @Test
    fun testBannerEventsInUserChangeWindowGoToPreviousUser() {
        putBanners("a" to nativeBanner("b1"))
        assertTrue(manager.canPostEvent())

        identity.userId = "u2"
        manager.trackShown("a", "b1")
        manager.click("a", "b1", null)

        assertFalse(manager.canPostEvent())
        assertEquals(listOf("show", "click"), types())
        assertEquals(listOf("u1", "u1"), events.events.map { it.getString("userId") })

        identity.userId = null
        manager.onUserConfirmed()
        putBanners("a" to nativeBanner("b9"), userId = null)
        manager.trackShown("a", "b9")
        assertEquals("hwid", events.events.last().getString("userId"))
        assertTrue(manager.canPostEvent())
    }

    // Verifies that dismiss collapses the slot, fires updated(null) before dismissed, sends dismiss and requests the next banner once delivered.
    @Test
    fun testDismissIsSentAndSlotGetsNextBanner() {
        putBanners("a" to nativeBanner("b1"))
        listener.updates.clear()
        val seenAtUpdate = mutableListOf<Int>()
        listener.onUpdate = { seenAtUpdate += listener.dismissed.size + events.events.size }

        manager.dismiss("a", "b1")

        assertNull(store.banner("a"))
        assertEquals(listOf("a" to null), listener.updates)
        assertEquals(listOf(0), seenAtUpdate)
        assertEquals(listOf("a" to "b1"), listener.dismissed)
        assertEquals(listOf("dismiss"), types())
        assertTrue(transport.calls.isEmpty())

        manager.dismiss("a", "b1")
        assertEquals(1, events.events.size)

        events.deliver(0)
        assertEquals(listOf(listOf("a")), transport.calls)
        transport.answer(0, JSONObject().put("a", nativeBanner("b2")))
        assertEquals("b2", store.banner("a")?.bannerId)
    }

    // Verifies that an undelivered dismiss leaves the slot empty until the queue announces its delivery.
    @Test
    fun testDismissDeliveredByRetryGetsNextBanner() {
        putBanners("a" to nativeBanner("b1"), "b" to nativeBanner("c1"))
        manager.dismiss("a", "b1")
        manager.click("b", "c1", null)

        events.deliver(0, delivered = false)
        assertTrue(transport.calls.isEmpty())

        manager.onQueuedEventDelivered(events.events[1])
        assertTrue(transport.calls.isEmpty())

        manager.onQueuedEventDelivered(events.events[0])
        assertEquals(listOf(listOf("a")), transport.calls)

        transport.answer(0, JSONObject().put("a", nativeBanner("b2")))
        manager.onQueuedEventDelivered(events.events[0])
        assertEquals(1, transport.calls.size)
    }

    // Verifies that a dismiss delivered while a user change is unconfirmed requests its slot once the change is refused.
    @Test
    fun testDismissDeliveredInUserChangeWindowIsRequestedAfterRefusal() {
        putBanners("a" to nativeBanner("b1"))
        manager.dismiss("a", "b1")
        identity.userId = "u2"

        events.deliver(0)
        assertTrue(transport.calls.isEmpty())

        identity.userId = "u1"
        manager.onUserRefused()

        assertEquals(listOf(listOf("a")), transport.calls)
    }

    // Verifies that a control banner read as data is null and reports control once; listener never gets onSlotShown for it.
    @Test
    fun testControlBannerReadAsDataIsEmptyAndReportedOnce() {
        putBanners("c" to controlBanner("c1"), "a" to nativeBanner("b1"))

        assertNull(manager.banner("c"))
        assertNull(manager.banner("c"))
        manager.bannerDidBecomeVisible("c", "c1")

        assertEquals(listOf("control"), types())
        assertEquals("c1", events.events.single().getString("bannerId"))
        assertTrue(listener.shown.isEmpty())

        assertEquals("b1", manager.banner("a")?.bannerId)
        assertEquals(1, events.events.size)
        assertNull(manager.banner("missing"))
    }

    // Verifies that test banner events carry "test": true and a test show does not mark the live banner as shown.
    @Test
    fun testTestShowDoesNotHideLiveShow() {
        putBanners("a" to nativeBanner("b1"))
        putBanners("a" to nativeBanner("b1") { put("test", true) })

        manager.bannerDidBecomeVisible("a", "b1")
        assertTrue(events.events.single().getBoolean("test"))

        manager.dismiss("a", "b1")
        assertEquals(listOf(true, true), events.events.map { it.getBoolean("test") })
        assertEquals("b1", store.banner("a")?.bannerId)
        assertFalse(store.banner("a")!!.isTest)

        manager.bannerDidBecomeVisible("a", "b1")
        assertEquals(listOf("show", "dismiss", "show"), types())
        assertFalse(events.events.last().getBoolean("test"))
    }

    // Verifies that re-checking expiry on foreground, attach, click and live checks keeps a single expiry timer pending, which collapses the slot once.
    @Test
    fun testExpiryRechecksKeepOneTimerPending() {
        putBanners("a" to nativeBanner("b1") { put("expireDate", clock.nowMillis / 1000 + 60) })
        assertEquals(1, scheduler.pendingCount)

        repeat(5) {
            manager.onForeground()
            manager.subscribe("a") {}.cancel()
            manager.click("a", "b1", null)
            manager.isBannerLive("a", "b1")
        }
        assertEquals(1, scheduler.pendingCount)

        listener.updates.clear()
        scheduler.advance(61_000)
        assertEquals(listOf<Pair<String, String?>>("a" to null), listener.updates)
        assertEquals(0, scheduler.pendingCount)
    }

    // Verifies that a slot detached and attached again (new view, same session) is neither requested nor reported again.
    @Test
    fun testReattachedSlotDoesNotRequestOrReportAgain() {
        manager.slotDidAppear("a")
        manager.slotDidAppear("empty")
        scheduler.advance(500)
        transport.answer(0, JSONObject().put("a", nativeBanner("b1")).put("empty", JSONObject.NULL))
        manager.bannerDidBecomeVisible("a", "b1")

        manager.slotDidAppear("a")
        manager.slotDidAppear("empty")
        scheduler.advance(500)
        manager.bannerDidBecomeVisible("a", "b1")

        assertEquals(listOf(listOf("a", "empty")), transport.calls)
        assertEquals(listOf("show"), types())
    }
}
