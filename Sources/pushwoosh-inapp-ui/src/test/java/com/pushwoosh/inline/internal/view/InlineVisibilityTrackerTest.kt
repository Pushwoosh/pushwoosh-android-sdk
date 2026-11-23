package com.pushwoosh.inline.internal.view

import com.pushwoosh.inline.internal.FakeClock
import com.pushwoosh.inline.internal.FakeScheduler
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InlineVisibilityTrackerTest {

    private lateinit var scheduler: FakeScheduler
    private var sample = onScreen()
    private var control = false
    private var reports = 0

    @Before
    fun setUp() {
        scheduler = FakeScheduler(FakeClock(1_800_000_000_000L))
    }

    private fun onScreen() =
        VisibilitySample(foreground = true, frontmost = true, visibleShare = 1f, lineVisible = true, contentReady = true)

    private fun tracker() = InlineVisibilityTracker({ sample }, { control }, scheduler) { reports++ }

    // Verifies that a banner on screen is shown after 0.3 s and not before.
    @Test
    fun testBannerOnScreenIsShownAfterThreeTenthsOfASecond() {
        tracker().start()

        scheduler.advance(299)
        assertEquals(0, reports)
        scheduler.advance(1)
        assertEquals(1, reports)
        scheduler.advance(5_000)
        assertEquals(1, reports)
    }

    // Verifies that leaving the 50 % share restarts the 0.3 s count, also when it happens just before the count ends.
    @Test
    fun testScrolledBannerCountsOnlyThreeTenthsInView() {
        val tracker = tracker()
        tracker.start()
        scheduler.advance(250)
        sample = sample.copy(visibleShare = 0.2f)
        scheduler.advance(150)
        assertEquals(0, reports)

        sample = sample.copy(visibleShare = 0.8f)
        tracker.recheck()
        scheduler.advance(200)
        sample = sample.copy(visibleShare = 0.1f)
        tracker.recheck()
        scheduler.advance(200)
        sample = sample.copy(visibleShare = 0.8f)
        tracker.recheck()
        scheduler.advance(299)
        assertEquals(0, reports)
        scheduler.advance(1)
        assertEquals(1, reports)
    }

    // Verifies that a banner under a dialog / another activity (no window focus) is not shown until it is frontmost again.
    @Test
    fun testBannerUnderPresentedScreenIsNotShown() {
        sample = sample.copy(frontmost = false)
        val tracker = tracker()
        tracker.start()
        scheduler.advance(5_000)
        assertEquals(0, reports)

        sample = sample.copy(frontmost = true)
        tracker.recheck()
        scheduler.advance(300)
        assertEquals(1, reports)
    }

    // Verifies that a banner of an app in the background is not shown and is not sampled until a recheck brings it back to the foreground.
    @Test
    fun testBannerOfBackgroundedAppIsNotShown() {
        sample = sample.copy(foreground = false)
        val tracker = tracker()
        tracker.start()
        scheduler.advance(5_000)
        assertEquals(0, reports)
        assertEquals(0, scheduler.pendingCount)

        sample = sample.copy(foreground = true)
        scheduler.advance(600)
        assertEquals(0, reports)
        tracker.recheck()
        scheduler.advance(300)
        assertEquals(1, reports)
    }

    // Verifies that sampling stops while the app goes to the background, the slot loses its window focus or its content is not ready, and goes on while only the slot's place fails.
    @Test
    fun testSamplingPausesOnlyWhileTheStateCannotCount() {
        val tracker = tracker()
        for (paused in listOf(sample.copy(foreground = false), sample.copy(frontmost = false), sample.copy(contentReady = false))) {
            sample = onScreen().copy(visibleShare = 0.1f)
            tracker.start()
            scheduler.advance(1_000)
            assertEquals(1, scheduler.pendingCount)

            sample = paused
            scheduler.advance(1_000)
            assertEquals(0, scheduler.pendingCount)
            tracker.stop()
        }
        assertEquals(0, reports)
    }

    // Verifies that an HTML banner counts only after its content is ready.
    @Test
    fun testHtmlBannerIsShownOnlyOnceItsDocumentHasDrawn() {
        sample = sample.copy(contentReady = false)
        val tracker = tracker()
        tracker.start()
        scheduler.advance(5_000)
        assertEquals(0, reports)

        sample = sample.copy(contentReady = true)
        tracker.recheck()
        scheduler.advance(299)
        assertEquals(0, reports)
        scheduler.advance(1)
        assertEquals(1, reports)
    }

    // Verifies that a control slot reports when its line is on screen with zero height, and not while the line is off screen.
    @Test
    fun testControlSlotReportsWhenItsPlaceIsOnScreen() {
        control = true
        sample = sample.copy(visibleShare = null, lineVisible = false)
        val tracker = tracker()
        tracker.start()
        scheduler.advance(5_000)
        assertEquals(0, reports)

        sample = sample.copy(lineVisible = true)
        tracker.recheck()
        scheduler.advance(300)
        assertEquals(1, reports)
    }

    // Verifies that 49 % visible never reports and 50 % does.
    @Test
    fun testHalfVisibleBoundary() {
        sample = sample.copy(visibleShare = 0.49f)
        val tracker = tracker()
        tracker.start()
        scheduler.advance(5_000)
        assertEquals(0, reports)

        sample = sample.copy(visibleShare = 0.5f)
        tracker.recheck()
        scheduler.advance(300)
        assertEquals(1, reports)
    }
}
