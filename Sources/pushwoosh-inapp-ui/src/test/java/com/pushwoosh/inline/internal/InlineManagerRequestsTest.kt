package com.pushwoosh.inline.internal

import com.pushwoosh.inline.PushwooshInlineException
import com.pushwoosh.inline.internal.store.InlineStore
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
import java.util.concurrent.Executor

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.LEGACY)
class InlineManagerRequestsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var clock: FakeClock
    private lateinit var scheduler: FakeScheduler
    private lateinit var identity: FakeIdentity
    private lateinit var transport: FakeTransport
    private lateinit var listener: RecordingListener
    private lateinit var store: InlineStore
    private lateinit var manager: InlineManager

    @Before
    fun setUp() {
        clock = FakeClock(1_000_000L)
        scheduler = FakeScheduler(clock)
        identity = FakeIdentity()
        transport = FakeTransport()
        listener = RecordingListener()
        store = InlineStore(File(folder.root, "pushwoosh/inline-banners.json"), clock, scheduler, Executor { it.run() })
        manager = InlineManager(store, transport, FakeEventSink(), identity, clock, scheduler)
        manager.listener = listener
    }

    private fun slots(vararg entries: Pair<String, Any?>): JSONObject = JSONObject().apply {
        for ((slotId, banner) in entries) put(slotId, banner ?: JSONObject.NULL)
    }

    private fun request(vararg ids: String): MutableList<Int?> {
        val codes = mutableListOf<Int?>()
        manager.requestSlots(ids.toList()) { codes += it?.code }
        return codes
    }

    /** Puts [banners] into their slots through an event answer of the current user, as the server would. */
    private fun putBanners(vararg banners: Pair<String, JSONObject>) {
        manager.onEventAnswer(JSONObject().put("slots", slots(*banners)), identity.userId, identity.appCode)
    }

    // Verifies that with no known slots nothing is requested on session start.
    @Test
    fun testNoKnownSlotsSendNothing() {
        manager.onForeground()

        assertTrue(transport.calls.isEmpty())
    }

    // Verifies that session start requests all known slots, ten per request, sorted.
    @Test
    fun testKnownSlotsGoTenPerRequest() {
        manager.onForeground()
        val ids = (0 until 12).map { "s%02d".format(it) }
        store.rememberSlots(ids.shuffled(java.util.Random(7)))
        manager.onBackground()
        clock.nowMillis += 30 * 60_000L

        manager.onForeground()

        assertEquals(listOf(ids.take(10), ids.drop(10)), transport.calls)
    }

    // Verifies that a return after exactly 30 minutes is a new session and after 29 minutes is not.
    @Test
    fun testThirtyMinuteBreakStartsNewSession() {
        manager.onForeground()
        store.rememberSlots(listOf("a"))
        manager.onNetworkAvailable()
        transport.answer(0, slots("a" to null))

        manager.onBackground()
        clock.nowMillis += 29 * 60_000L
        manager.onForeground()
        assertEquals(1, transport.calls.size)

        manager.onBackground()
        clock.nowMillis += 30 * 60_000L
        manager.onForeground()
        assertEquals(listOf(listOf("a"), listOf("a")), transport.calls)
    }

    // Verifies that new slot views within 0.5 s share one request and a cached slot is not requested on appear.
    @Test
    fun testNewSlotViewsShareOneRequest() {
        manager.onForeground()
        putBanners("cached" to nativeBanner("b1"))

        manager.slotDidAppear("a")
        scheduler.advance(200)
        manager.slotDidAppear("b")
        manager.slotDidAppear("cached")
        manager.slotDidAppear("a")
        scheduler.advance(299)
        assertTrue(transport.calls.isEmpty())

        scheduler.advance(1)
        assertEquals(listOf(listOf("a", "b")), transport.calls)
        assertEquals(listOf("a", "b", "cached"), store.knownSlots())
    }

    // Verifies the manual request error codes and order: empty → 1, 11 slots → 1 (slots not remembered), unconfirmed → 5 (slots remembered, no token spent), 6th call → 3 without network.
    @Test
    fun testManualRequestErrorsAndOrder() {
        manager.onForeground()

        assertEquals(listOf(PushwooshInlineException.INVALID_ARGUMENT), request("", ""))
        assertEquals(listOf(PushwooshInlineException.INVALID_ARGUMENT), request(*Array(11) { "x$it" }))
        assertTrue(store.knownSlots().isEmpty())

        identity.userId = "u2"
        assertEquals(listOf(PushwooshInlineException.OWNER_CHANGED), request("held"))
        assertTrue("held" in store.knownSlots())
        identity.userId = "u1"

        repeat(5) { request("a", "a", "", "b") }
        assertEquals(5, transport.calls.size)
        assertEquals(listOf("a", "b"), transport.calls[0])

        assertEquals(listOf(PushwooshInlineException.RATE_LIMITED), request("a"))
        assertEquals(5, transport.calls.size)
        assertTrue(transport.calls.flatten().none { it.startsWith("x") || it == "held" })
    }

    // Verifies that automatic requests do not spend manual tokens.
    @Test
    fun testAutomaticRequestsDoNotSpendManualLimit() {
        manager.onForeground()
        store.rememberSlots(listOf("a"))
        repeat(6) { i ->
            manager.onNetworkAvailable()
            transport.fail(i)
        }
        assertEquals(6, transport.calls.size)

        val codes = (0 until 6).flatMap { request("m") }

        assertEquals(11, transport.calls.size)
        assertEquals(listOf(PushwooshInlineException.RATE_LIMITED), codes)
    }

    // Verifies that a rate-limited call keeps the cached banner.
    @Test
    fun testLimitedRequestKeepsCachedBanner() {
        manager.onForeground()
        request("a")
        transport.answer(0, slots("a" to nativeBanner("b1")))
        repeat(4) { request("a") }
        listener.updates.clear()

        val codes = request("a")

        assertEquals(listOf(PushwooshInlineException.RATE_LIMITED), codes)
        assertEquals("b1", store.banner("a")?.bannerId)
        assertTrue(listener.updates.isEmpty())
    }

    // Verifies that the answer fills requested slots, empties the rest, ignores unrequested keys and notifies the listener only for changed slots.
    @Test
    fun testResponseFillsSlotsAndEmptiesTheRest() {
        manager.onForeground()
        request("a", "b")
        transport.answer(0, slots("a" to nativeBanner("b1"), "b" to htmlBanner("h1")))
        listener.updates.clear()

        request("a", "b", "c", "d")
        transport.answer(1, slots("a" to nativeBanner("b1"), "c" to controlBanner("c1"), "z" to nativeBanner("bz")))

        assertEquals("b1", store.banner("a")?.bannerId)
        assertNull(store.banner("b"))
        assertNotNull(store.banner("c"))
        assertNull(store.banner("d"))
        assertNull(store.banner("z"))
        assertEquals(listOf("b" to null, "c" to null), listener.updates)
    }

    // Verifies that a non-object banner value empties only its slot while the other requested slot applies.
    @Test
    fun testNonObjectBannerEmptiesOnlyItsSlot() {
        manager.onForeground()
        putBanners("a" to nativeBanner("old"))
        request("a", "b")

        transport.answer(0, slots("a" to "oops", "b" to nativeBanner("b1")))

        assertNull(store.banner("a"))
        assertEquals("b1", store.banner("b")?.bannerId)
    }

    // Verifies that a failed request keeps banners and the slot is requested again on the next appear / network back.
    @Test
    fun testFailedRequestIsRepeatedWhenSlotShowsAgainOrNetworkBack() {
        manager.onForeground()
        putBanners("a" to nativeBanner("b1"))
        store.rememberSlots(listOf("a", "b"))
        manager.onNetworkAvailable()
        assertEquals(listOf("a", "b"), transport.calls[0])

        transport.fail(0)
        assertEquals("b1", store.banner("a")?.bannerId)

        manager.slotDidAppear("b")
        scheduler.advance(500)
        assertEquals(listOf("b"), transport.calls[1])
        transport.fail(1, PushwooshInlineException.INVALID_RESPONSE)

        manager.onBackground()
        manager.onNetworkAvailable()
        assertEquals(2, transport.calls.size)

        manager.onForeground()
        assertEquals(listOf("a", "b"), transport.calls[2])
        transport.fail(2)
        manager.onNetworkAvailable()
        assertEquals(listOf("a", "b"), transport.calls[3])
    }

    // Verifies that an answer of the previous user arriving after a confirmed change is dropped and the new user's request still applies.
    @Test
    fun testDroppedAnswerOfPreviousUserKeepsNewUserRequestInFlight() {
        manager.onForeground()
        val codes = request("a")
        identity.userId = "u2"
        manager.onUserConfirmed()

        transport.answer(0, slots("a" to nativeBanner("old")))
        transport.answer(1, slots("a" to nativeBanner("new")))

        assertEquals(listOf(PushwooshInlineException.OWNER_CHANGED), codes)
        assertEquals(listOf("a" to "new"), listener.updates)
    }

    // Verifies that a confirmed user change clears the previous user's banners (didUpdate null) and requests known slots.
    @Test
    fun testUserChangeReplacesPreviousUserBanners() {
        manager.onForeground()
        request("a")
        transport.answer(0, slots("a" to nativeBanner("b1")))
        listener.updates.clear()
        identity.userId = "u2"
        assertTrue(manager.isUserChangeUnconfirmed())

        manager.onUserConfirmed()

        assertFalse(manager.isUserChangeUnconfirmed())
        assertNull(store.banner("a"))
        assertEquals(listOf("a" to null), listener.updates)
        assertEquals(listOf("a"), transport.calls[1])

        transport.answer(1, slots("a" to nativeBanner("b2")), "u2", "APP-1")
        assertEquals("b2", store.banner("a")?.bannerId)
    }

    // Verifies that before the hwid exists nothing is requested, and the first known owner is adopted without an unconfirmed user change.
    @Test
    fun testOwnerUnknownUntilHwidArrivesOpensNoUserChangeWindow() {
        identity.userId = null
        identity.hwid = ""
        manager.onForeground()
        manager.slotDidAppear("a")
        scheduler.advance(500)
        val early = request("b")

        assertTrue(transport.calls.isEmpty())
        assertEquals(listOf(PushwooshInlineException.OWNER_CHANGED), early)

        identity.hwid = "h1"
        assertFalse(manager.isUserChangeUnconfirmed())
        manager.onForeground()
        assertEquals(listOf(listOf("a", "b")), transport.calls)
        transport.answer(0, slots("a" to nativeBanner("b1")), "h1", "APP-1")

        assertEquals("b1", store.banner("a")?.bannerId)
        val late = request("c")
        transport.answer(1, slots("c" to null), "h1", "APP-1")
        assertEquals(listOf<Int?>(null), late)
    }

    // Verifies that a cache of another user read while the owner was unknown is discarded once the owner is known, without a user change window.
    @Test
    fun testCacheReadWithUnknownOwnerIsDiscardedForFirstKnownOwner() {
        manager.onForeground()
        putBanners("a" to nativeBanner("old"))
        identity.userId = null
        identity.hwid = ""
        val reloadedStore = InlineStore(File(folder.root, "pushwoosh/inline-banners.json"), clock, scheduler, Executor { it.run() })
        val reloaded = InlineManager(reloadedStore, transport, FakeEventSink(), identity, clock, scheduler)
        reloaded.onForeground()
        identity.hwid = "h1"

        assertFalse(reloaded.isUserChangeUnconfirmed())
        assertNull(reloadedStore.banner("a"))
        assertTrue(reloaded.canPostEvent())
    }

    // Verifies that during an unconfirmed change automatic requests are held and an answer arriving for the previous owner is dropped.
    @Test
    fun testUnconfirmedWindowHoldsRequestsAndDropsAnswers() {
        manager.onForeground()
        putBanners("shown" to nativeBanner("b0"))
        val codes = request("a")
        identity.userId = "u2"

        transport.answer(0, slots("a" to nativeBanner("b1")), "u1", "APP-1")
        manager.slotDidAppear("b")
        scheduler.advance(500)
        manager.onNetworkAvailable()
        manager.onBackground()
        manager.onForeground()

        assertEquals(listOf(PushwooshInlineException.OWNER_CHANGED), codes)
        assertNull(store.banner("a"))
        assertEquals("b0", store.banner("shown")?.bannerId)
        assertEquals(1, transport.calls.size)
    }

    // Verifies that a refusal keeps the previous banners and requests held plus unrequested known slots in one request.
    @Test
    fun testRefusalRequestsHeldAndUnrequestedSlotsTogether() {
        manager.onForeground()
        request("a")
        transport.answer(0, slots("a" to nativeBanner("b1")))
        store.rememberSlots(listOf("c"))
        identity.userId = "u2"
        manager.slotDidAppear("b")
        scheduler.advance(500)
        assertEquals(1, transport.calls.size)
        listener.updates.clear()

        identity.userId = "u1"
        manager.onUserRefused()

        assertEquals("b1", store.banner("a")?.bannerId)
        assertTrue(listener.updates.isEmpty())
        assertEquals(listOf(listOf("a"), listOf("b", "c")), transport.calls)
    }

    // Verifies that an app code change clears banners and requests known slots.
    @Test
    fun testApplicationChangeClearsBannersAndRequestsKnownSlots() {
        manager.onForeground()
        request("a")
        transport.answer(0, slots("a" to nativeBanner("b1")))
        listener.updates.clear()

        identity.appCode = "APP-2"
        manager.onAppCodeChanged()

        assertNull(store.banner("a"))
        assertEquals(listOf("a" to null), listener.updates)
        assertEquals(listOf("a"), transport.calls[1])

        transport.answer(1, slots("a" to nativeBanner("b2")), "u1", "APP-2")
        assertEquals("b2", store.banner("a")?.bannerId)
    }

    // Verifies that a user or app code confirmation that leaves the owner as it was (first hwid registration) requests nothing again and keeps the banners.
    @Test
    fun testConfirmationOfUnchangedOwnerRequestsNothingAgain() {
        manager.onForeground()
        manager.slotDidAppear("a")
        scheduler.advance(500)
        transport.answer(0, slots("a" to nativeBanner("b1")), "u1", "APP-1")
        listener.updates.clear()

        manager.onUserConfirmed()
        manager.onAppCodeChanged()
        scheduler.advance(500)

        assertEquals(listOf(listOf("a")), transport.calls)
        assertEquals("b1", store.banner("a")?.bannerId)
        assertTrue(listener.updates.isEmpty())
    }

    // Verifies that an event answer updates only listed slots, ignores a missing slots object and is dropped for another owner.
    @Test
    fun testEventAnswerUpdatesOnlyListedSlots() {
        manager.onForeground()
        putBanners("a" to nativeBanner("a1"), "b" to nativeBanner("b1"))

        manager.onEventAnswer(JSONObject().put("slots", slots("a" to nativeBanner("a2"))), "u1", "APP-1")
        assertEquals("a2", store.banner("a")?.bannerId)
        assertEquals("b1", store.banner("b")?.bannerId)

        manager.onEventAnswer(JSONObject(), "u1", "APP-1")
        manager.onEventAnswer(JSONObject().put("slots", JSONObject()), "u1", "APP-1")
        manager.onEventAnswer(JSONObject().put("slots", slots("a" to null)), "u2", "APP-1")
        manager.onEventAnswer(JSONObject().put("slots", slots("b" to null)), "u1", "APP-2")
        assertEquals("a2", store.banner("a")?.bannerId)
        assertEquals("b1", store.banner("b")?.bannerId)

        identity.userId = null
        manager.onUserConfirmed()
        manager.onEventAnswer(JSONObject().put("slots", slots("a" to nativeBanner("a3"))), null, "APP-1")
        assertEquals("a3", store.banner("a")?.bannerId)
    }

    // Verifies that requestSlots success completes after the store applied and the listener was notified.
    @Test
    fun testRequestCallbackComesAfterListener() {
        manager.onForeground()
        val order = mutableListOf<String>()
        listener.onUpdate = { order += "listener:" + store.banner("a")?.bannerId }
        manager.requestSlots(listOf("a")) { error -> order += "callback:" + (error?.code ?: "ok") }

        transport.answer(0, slots("a" to nativeBanner("b1")))

        assertEquals(listOf("listener:b1", "callback:ok"), order)
    }
}
