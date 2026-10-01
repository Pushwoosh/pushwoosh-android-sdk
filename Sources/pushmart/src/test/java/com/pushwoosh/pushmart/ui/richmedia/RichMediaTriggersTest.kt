package com.pushwoosh.pushmart.ui.richmedia

import com.pushwoosh.pushmart.account.AccountOutcome
import com.pushwoosh.pushmart.sdk.FakePushwooshGateway
import com.pushwoosh.pushmart.sdk.GatewayResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric shadows android.util.Log, needed for the PWLog call in
 * [RichMediaTriggers.onContentFailed].
 */
@RunWith(RobolectricTestRunner::class)
class RichMediaTriggersTest {
    private val gateway = FakePushwooshGateway()
    private val timer = FakeDelayedRunner()
    private val reports = mutableListOf<AccountOutcome>()
    private val triggers = RichMediaTriggers(gateway, timer) { reports.add(it) }

    @Test
    fun triggerPostsEventWithoutAttributesAndSaysSending() {
        gateway.deferCallbacks = true
        triggers.trigger("showRichMedia")
        assertEquals(listOf("showRichMedia" to emptyMap<String, Any>()), gateway.events)
        assertEquals(
            listOf<AccountOutcome>(AccountOutcome.Success("Sending showRichMedia…", "")), reports)
        assertEquals(1, timer.scheduled.size)
        assertEquals(RichMediaTriggers.CONTENT_TIMEOUT_MS, timer.scheduled[0].first)
    }

    @Test
    fun callbackFailureReportsEventFailedAndArmsNoTimer() {
        gateway.postEventResult = GatewayResult.failure("no network")
        triggers.trigger("showRichMedia")
        assertEquals(AccountOutcome.Failure("Event failed", "no network"), reports.last())
        assertTrue(timer.scheduled.isEmpty())
    }

    @Test
    fun callbackSuccessReportsSentAndWaitsFiveSeconds() {
        triggers.trigger("showRichMediaClient")
        assertEquals(AccountOutcome.Success("Event sent", "Waiting for content…"), reports.last())
        assertEquals(1, timer.scheduled.size)
        assertEquals(RichMediaTriggers.CONTENT_TIMEOUT_MS, timer.scheduled[0].first)
    }

    @Test
    fun timeoutWithoutContentReportsNoContentReturned() {
        triggers.trigger("showRichMediaServer")
        timer.fire(0)
        assertEquals(
            AccountOutcome.Failure(
                "No content returned",
                "showRichMediaServer was sent but nothing came back. Configure a campaign for this event in Control Panel."),
            reports.last())
    }

    @Test
    fun contentShownBeforeTimeoutReportsShownAndCancelsTimer() {
        triggers.trigger("showRichMedia")
        triggers.onContentShown("r-ABC12")
        assertEquals(AccountOutcome.Success("Content shown", "r-ABC12"), reports.last())
        val count = reports.size
        timer.fire(0)
        assertEquals(count, reports.size)
    }

    @Test
    fun contentFailedReportsFailureAndCancelsTimer() {
        triggers.trigger("showRichMedia")
        triggers.onContentFailed("r-ABC12", "zip download failed")
        assertEquals(
            AccountOutcome.Failure("Content failed", "zip download failed"), reports.last())
        val count = reports.size
        timer.fire(0)
        assertEquals(count, reports.size)
    }

    @Test
    fun secondTriggerSupersedesFirst() {
        gateway.deferCallbacks = true
        triggers.trigger("showRichMedia")
        triggers.trigger("showRichMediaClient")
        gateway.flush()
        assertEquals(
            1, reports.count { it == AccountOutcome.Success("Event sent", "Waiting for content…") })
        // Each trigger arms its own watchdog immediately; the first one is superseded, not removed.
        assertEquals(2, timer.scheduled.size)
        assertEquals(setOf(0), timer.cancelled)
        timer.fire(1)
        val failure = reports.last() as AccountOutcome.Failure
        assertEquals("No content returned", failure.title)
        assertTrue(failure.message, failure.message.startsWith("showRichMediaClient was sent"))
    }

    @Test
    fun unsolicitedContentShownIsReported() {
        triggers.onContentShown("r-PUSH1")
        assertEquals(
            listOf<AccountOutcome>(AccountOutcome.Success("Content shown", "r-PUSH1")), reports)
    }

    @Test
    fun unsolicitedContentFailedIsReported() {
        triggers.onContentFailed("x", "boom")
        assertEquals(
            listOf<AccountOutcome>(AccountOutcome.Failure("Content failed", "boom")), reports)
    }

    @Test
    fun cancelSilencesPendingCallbackAndTimer() {
        gateway.deferCallbacks = true
        triggers.trigger("showRichMedia")
        triggers.cancel()
        gateway.flush()
        assertEquals(1, timer.scheduled.size)
        assertEquals(setOf(0), timer.cancelled)
        assertEquals(
            listOf<AccountOutcome>(AccountOutcome.Success("Sending showRichMedia…", "")), reports)
    }

    @Test
    fun timeoutWithoutCallbackReportsNoResponse() {
        gateway.deferCallbacks = true
        triggers.trigger("showRichMedia")
        timer.fire(0)
        assertEquals(
            AccountOutcome.Failure(
                "No response",
                "showRichMedia got no answer in 5 s. Check the app code and network."),
            reports.last())
        val count = reports.size
        gateway.flush()
        assertEquals(count, reports.size)
    }
}
