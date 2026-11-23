package com.pushwoosh.inline.internal.html

import com.pushwoosh.inline.internal.FakeClock
import com.pushwoosh.inline.internal.FakeScheduler
import com.pushwoosh.inline.internal.html.InlineHtmlTapCoordinator.Kind
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InlineHtmlTapCoordinatorTest {

    private val reported = mutableListOf<String>()
    private lateinit var scheduler: FakeScheduler

    @Before
    fun setUp() {
        scheduler = FakeScheduler(FakeClock(1_000L))
    }

    private fun coordinator(bannerHasAction: Boolean = false) = InlineHtmlTapCoordinator(
        scheduler,
        onClick = { reported += "click:$it" },
        onDismiss = { reported += "dismiss" },
        onBannerTap = if (bannerHasAction) ({ reported += "banner" }) else null,
    )

    // Verifies that a tap on an unmarked control is a click without element after 1 s.
    @Test
    fun testTapOnUnmarkedHtmlElementIsClick() {
        val taps = coordinator()

        taps.onTap()
        taps.onTapResult(Kind.CONTROL, null)
        scheduler.advance(999)
        assertEquals(emptyList<String>(), reported)
        scheduler.advance(1)

        assertEquals(listOf("click:null"), reported)
    }

    // Verifies that closeInApp right after an unmarked-control tap sends only the dismiss, whichever arrives first.
    @Test
    fun testHtmlCloseInAppAfterTapIsOnlyDismiss() {
        val hitFirst = coordinator()
        hitFirst.onTap()
        hitFirst.onTapResult(Kind.CONTROL, null)
        hitFirst.onCloseInApp()
        scheduler.advance(5_000)
        assertEquals("hit test first", listOf("dismiss"), reported)

        reported.clear()
        val closeFirst = coordinator()
        closeFirst.onTap()
        closeFirst.onCloseInApp()
        assertEquals("close waits for the hit test", emptyList<String>(), reported)
        closeFirst.onTapResult(Kind.CONTROL, null)
        scheduler.advance(5_000)
        assertEquals("close first", listOf("dismiss"), reported)
    }

    // Verifies that a closing action (registerForPushNotifications/openAppSettings) is click then dismiss.
    @Test
    fun testHtmlClosingActionIsClickThenDismiss() {
        val hitFirst = coordinator()
        hitFirst.onTap()
        hitFirst.onTapResult(Kind.CONTROL, null)
        hitFirst.onClosingAction()
        hitFirst.onCloseInApp()
        scheduler.advance(5_000)
        assertEquals("hit test first", listOf("click:null", "dismiss"), reported)

        reported.clear()
        val bridgeFirst = coordinator()
        bridgeFirst.onTap()
        bridgeFirst.onClosingAction()
        bridgeFirst.onCloseInApp()
        bridgeFirst.onTapResult(Kind.CONTROL, null)
        scheduler.advance(5_000)
        assertEquals("bridge first", listOf("click:null", "dismiss"), reported)
    }

    // Verifies that a marked element click carries its data-pw-id and close after it gives click then dismiss.
    @Test
    fun testMarkedElementClickThenClose() {
        val hitFirst = coordinator()
        hitFirst.onTap()
        hitFirst.onTapResult(Kind.MARKED, "buy")
        hitFirst.onCloseInApp()
        assertEquals("hit test first", listOf("click:buy", "dismiss"), reported)

        reported.clear()
        val closeFirst = coordinator()
        closeFirst.onTap()
        closeFirst.onCloseInApp()
        closeFirst.onTapResult(Kind.MARKED, "buy")
        assertEquals("close first", listOf("click:buy", "dismiss"), reported)
    }

    // Verifies that a tap outside any control runs the banner action only when the banner has one.
    @Test
    fun testTapOutsideControlsRunsBannerActionOnlyWhenPresent() {
        val withAction = coordinator(bannerHasAction = true)
        withAction.onTap()
        withAction.onTapResult(Kind.NONE, null)
        assertEquals(listOf("banner"), reported)

        reported.clear()
        val withoutAction = coordinator()
        withoutAction.onTap()
        withoutAction.onTapResult(Kind.NONE, null)
        scheduler.advance(5_000)
        assertEquals(emptyList<String>(), reported)
    }

    // Verifies that a held-back control click is reported at once on the next tap or on flush, and only once.
    @Test
    fun testPendingClickIsFlushedOnNextTapOrDetach() {
        val taps = coordinator(bannerHasAction = true)
        taps.onTap()
        taps.onTapResult(Kind.CONTROL, null)
        taps.onTap()
        assertEquals("next tap", listOf("click:null"), reported)
        taps.onTapResult(Kind.NONE, null)

        taps.onTap()
        taps.onTapResult(Kind.CONTROL, null)
        taps.flush()
        scheduler.advance(5_000)

        assertEquals(listOf("click:null", "banner", "click:null"), reported)
    }
}
