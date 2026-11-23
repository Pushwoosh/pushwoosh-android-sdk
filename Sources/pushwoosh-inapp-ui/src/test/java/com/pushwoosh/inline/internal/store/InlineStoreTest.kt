package com.pushwoosh.inline.internal.store

import com.pushwoosh.inline.internal.FakeClock
import com.pushwoosh.inline.internal.FakeScheduler
import com.pushwoosh.inline.internal.htmlBanner
import com.pushwoosh.inline.internal.model.InlineBanner
import com.pushwoosh.inline.internal.nativeBanner
import com.pushwoosh.inline.internal.parser.InlineParser
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
class InlineStoreTest {

    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File
    private lateinit var clock: FakeClock
    private lateinit var scheduler: FakeScheduler
    private val changes = mutableListOf<Pair<String, String?>>()

    @Before
    fun setUp() {
        file = File(folder.root, "pushwoosh/inline-banners.json")
        clock = FakeClock(1_000_000L)
        scheduler = FakeScheduler(clock)
    }

    private fun newStore(owner: String? = OWNER): InlineStore =
        InlineStore(file, clock, scheduler, Executor { it.run() }).apply {
            onChange = { slotId, banner -> changes += slotId to banner?.bannerId }
            load(owner)
        }

    private fun slots(vararg entries: Pair<String, Any?>): JSONObject = JSONObject().apply {
        for ((slotId, banner) in entries) put(slotId, banner ?: JSONObject.NULL)
    }

    private fun testBanner(id: String, version: Int = 1): JSONObject = nativeBanner(id, version) { put("test", true) }

    // Verifies that apply fills answered slots and empties requested slots without a banner.
    @Test
    fun testApplyPutsBannersAndEmptiesSlotsWithout() {
        val store = newStore()
        store.apply(listOf("a", "b"), slots("a" to nativeBanner("b1"), "b" to htmlBanner("h1")), OWNER)

        val applied = store.apply(listOf("a", "b", "c"), slots("a" to nativeBanner("b2"), "b" to null), OWNER)

        assertTrue(applied)
        assertEquals("b2", store.banner("a")?.bannerId)
        assertNull(store.banner("b"))
        assertNull(store.banner("c"))
        assertEquals(listOf("a" to "b1", "b" to "h1", "a" to "b2", "b" to null), changes)
    }

    // Verifies that the same banner id with version <= stored is not a change (no onChange, no subscriber call).
    @Test
    fun testRepeatedBannerIsNotAChange() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 2)), OWNER)
        val seen = mutableListOf<String?>()
        store.subscribe("a") { seen += it?.bannerId }
        changes.clear()

        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 2)), OWNER)
        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 1)), OWNER)

        assertEquals(listOf<String?>("b1"), seen)
        assertTrue(changes.isEmpty())
        assertEquals(2, store.banner("a")?.version)

        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 3)), OWNER)
        assertEquals(3, store.banner("a")?.version)
        assertEquals(listOf("a" to "b1"), changes)
    }

    // Verifies that a dismissed banner does not come back at the same version, comes back at a higher one, and returns after its expireDate passes.
    @Test
    fun testClosedBannerDoesNotComeBack() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 2)), OWNER)

        assertEquals("b1", store.dismiss("a")?.bannerId)
        assertNull(store.banner("a"))
        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 2)), OWNER)
        assertNull(store.banner("a"))
        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 3)), OWNER)
        assertEquals(3, store.banner("a")?.version)

        val nowSec = clock.nowMillis / 1000
        store.apply(listOf("e"), slots("e" to nativeBanner("x1") { put("expireDate", nowSec + 100) }), OWNER)
        store.dismiss("e")
        clock.nowMillis += 100_000
        store.apply(listOf("e"), slots("e" to nativeBanner("x1") { put("expireDate", nowSec + 200) }), OWNER)
        assertEquals("x1", store.banner("e")?.bannerId)
    }

    // Verifies that banners, dismissals, reported versions and known slots survive a new InlineStore on the same file.
    @Test
    fun testStateSurvivesRelaunch() {
        val first = newStore()
        first.apply(listOf("a", "d"), slots("a" to nativeBanner("b1", version = 2), "d" to htmlBanner("h1")), OWNER)
        first.dismiss("d")
        assertTrue(first.markReported(first.banner("a")!!))
        first.rememberSlots(listOf("a", "d"))

        val second = newStore()

        assertEquals(OWNER, second.owner)
        assertEquals(2, second.banner("a")?.version)
        assertTrue(second.isReported(second.banner("a")!!))
        assertFalse(second.markReported(second.banner("a")!!))
        second.apply(listOf("d"), slots("d" to htmlBanner("h1")), OWNER)
        assertNull(second.banner("d"))
        assertEquals(listOf("a", "d"), second.knownSlots())
    }

    // Verifies that a cache of another owner is not shown after relaunch.
    @Test
    fun testCacheOfAnotherOwnerIsNotShownAfterRelaunch() {
        newStore().apply(listOf("a"), slots("a" to nativeBanner("b1")), OWNER)
        changes.clear()

        val other = newStore("u2|A")

        assertEquals("u2|A", other.owner)
        assertNull(other.banner("a"))
        assertNull(other.previousUserId("u2|A"))
        assertTrue(changes.isEmpty())
    }

    // Verifies that an expired banner is not loaded and a live one collapses when the expiry timer fires.
    @Test
    fun testExpiredBannerIsDroppedOnLoadAndByTimer() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1") { put("expireDate", 1010L) }), OWNER)
        val seen = mutableListOf<String?>()
        store.subscribe("a") { seen += it?.bannerId }

        scheduler.advance(9_000)
        assertEquals("b1", store.banner("a")?.bannerId)
        scheduler.advance(1_000)

        assertEquals(listOf("b1", null), seen)
        assertEquals("a" to null, changes.last())
        assertNull(newStore().banner("a"))

        clock.nowMillis = 1_000_000L
        file.writeText(
            JSONObject().put("owner", OWNER).put(
                "banners",
                JSONObject().put(
                    "a",
                    JSONObject().put("banner", nativeBanner("old") { put("expireDate", 999L) }).put("receivedAt", 900L),
                ),
            ).toString(),
        )
        assertNull(newStore().banner("a"))
    }

    // Verifies that an expireDate at the int64 limit keeps the banner shown with one future expiry check instead of a busy loop.
    @Test
    fun testFarExpireDateDoesNotSpin() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1") { put("expireDate", Long.MAX_VALUE) }), OWNER)

        assertEquals("b1", store.banner("a")?.bannerId)
        assertEquals(1, scheduler.pendingCount)
        assertTrue(scheduler.nextDelayMs!! > 0)

        scheduler.advance(48L * 3600 * 1000)

        assertEquals("b1", store.banner("a")?.bannerId)
        assertEquals(1, scheduler.pendingCount)
        assertTrue(scheduler.nextDelayMs!! > 0)
    }

    // Verifies that an answer for another owner changes nothing.
    @Test
    fun testAnswerForAnotherOwnerIsDropped() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1")), OWNER)
        changes.clear()

        val applied = store.apply(listOf("a", "b"), slots("a" to null, "b" to nativeBanner("b2")), "u2|A")

        assertFalse(applied)
        assertEquals("b1", store.banner("a")?.bannerId)
        assertNull(store.banner("b"))
        assertTrue(changes.isEmpty())
    }

    // Verifies that a test banner covers the live one, survives an empty answer, is not persisted, and dismissing it uncovers the live one.
    @Test
    fun testTestBannerCoversLiveBannerInMemoryOnly() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("live")), OWNER)
        store.apply(listOf("a"), slots("a" to testBanner("t1")), OWNER)
        assertEquals("t1", store.banner("a")?.bannerId)
        assertEquals("live", newStore().banner("a")?.bannerId)

        store.apply(listOf("a"), slots("a" to null), OWNER)
        assertEquals("t1", store.banner("a")?.bannerId)

        store.apply(listOf("a"), slots("a" to nativeBanner("live")), OWNER)
        assertEquals("t1", store.dismiss("a")?.bannerId)
        assertEquals("live", store.banner("a")?.bannerId)
        store.apply(listOf("a"), slots("a" to testBanner("t1")), OWNER)
        assertEquals("live", store.banner("a")?.bannerId)
    }

    // Verifies that a known slot survives relaunch and is forgotten after 30 days.
    @Test
    fun testKnownSlotForgottenAfterThirtyDays() {
        val store = newStore()
        store.rememberSlots(listOf("b", "a"))
        clock.nowMillis += DAY_MS * 3 / 4
        store.rememberSlots(listOf("b"))
        clock.nowMillis += DAY_MS * 3 / 4
        store.rememberSlots(listOf("a"))

        assertEquals(listOf("a", "b"), newStore().knownSlots())

        clock.nowMillis += 30 * DAY_MS - DAY_MS
        val relaunched = newStore()
        assertEquals(listOf("a"), relaunched.knownSlots())
        clock.nowMillis += DAY_MS
        assertEquals(emptyList<String>(), relaunched.knownSlots())
    }

    // Verifies that markReported is once per banner version and owner and resets after syncOwner.
    @Test
    fun testReportedOncePerVersionAndOwner() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 1)), OWNER)
        val v1 = store.banner("a")!!

        assertTrue(store.markReported(v1))
        assertFalse(store.markReported(v1))
        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 2)), OWNER)
        assertTrue(store.markReported(store.banner("a")!!))

        assertEquals(listOf("a"), store.syncOwner("u2|A"))
        store.apply(listOf("a"), slots("a" to nativeBanner("b1", version = 2)), "u2|A")
        val forOther: InlineBanner = store.banner("a")!!
        assertFalse(store.isReported(forOther))
        assertTrue(store.markReported(forOther))

        store.setCurrentOwner("u2|B")
        assertFalse(store.markReported(nativeBannerOf("b9")))
    }

    // Verifies that previousUserId is the cache user while the owner differs in the same app, and null across app codes.
    @Test
    fun testPreviousUserIdOnlyWithinSameApp() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1")), OWNER)

        assertNull(store.previousUserId(OWNER))
        assertEquals("u1", store.previousUserId("u2|A"))
        assertNull(store.previousUserId("u1|B"))
        assertNull(store.previousUserId(null))
        assertEquals("b1", store.banner("a")?.bannerId)

        store.syncOwner("u2|A")
        assertNull(store.previousUserId("u2|A"))
    }

    // Verifies that syncing to an owner of another app code empties shown slots for subscribers and the listener.
    @Test
    fun testSyncOwnerAcrossAppCodesNotifiesClearedSlots() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1")), OWNER)
        val seen = mutableListOf<String?>()
        store.subscribe("a") { seen += it?.bannerId }
        changes.clear()

        assertEquals(listOf("a"), store.syncOwner("u1|B"))

        assertEquals(listOf("b1", null), seen)
        assertEquals(listOf<Pair<String, String?>>("a" to null), changes)
    }

    // Verifies that dismissing a banner that expired before the timer fired neither returns nor records it.
    @Test
    fun testDismissOfExpiredBannerIsIgnored() {
        val store = newStore()
        store.apply(listOf("a"), slots("a" to nativeBanner("b1") { put("expireDate", 1010L) }), OWNER)
        clock.nowMillis = 1_010_000L

        assertNull(store.dismiss("a"))

        clock.nowMillis = 1_000_000L
        store.apply(listOf("a"), slots("a" to nativeBanner("b1") { put("expireDate", 2000L) }), OWNER)
        assertEquals("b1", store.banner("a")?.bannerId)
    }

    // Verifies that a truncated cache file starts the store empty and the next save overwrites it with a valid file.
    @Test
    fun testCorruptCacheFileStartsEmptyAndIsOverwritten() {
        file.parentFile!!.mkdirs()
        file.writeText("{\"owner\":\"u|A\",\"banners\":{")

        val store = newStore("u|A")
        assertNull(store.banner("a"))
        store.apply(listOf("a"), slots("a" to nativeBanner("b1")), "u|A")

        val saved = JSONObject(file.readText())
        assertEquals("u|A", saved.getString("owner"))
        assertNotNull(saved.getJSONObject("banners").optJSONObject("a"))
    }

    private fun nativeBannerOf(id: String): InlineBanner =
        checkNotNull(InlineParser.parse(nativeBanner(id), "z"))

    private companion object {
        const val OWNER = "u1|A"
        const val DAY_MS = 24L * 3600 * 1000
    }
}
