package com.pushwoosh.pushmart.sdk

import com.pushwoosh.exception.PushwooshException
import com.pushwoosh.richmedia.fakeRichMedia
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LoggingRichMediaDelegateTest {
    private val delegate = LoggingRichMediaDelegate()
    private val received = mutableListOf<RichMediaSignals.Signal>()

    init {
        RichMediaSignals.listener = { received += it }
    }

    @After
    fun clearListener() {
        RichMediaSignals.listener = null
    }

    @Test
    fun presentPublishesTheCode() {
        delegate.onPresent(fakeRichMedia("rm-1"))

        assertEquals(listOf(RichMediaSignals.Signal.Presented("rm-1")), received)
    }

    @Test
    fun errorWithoutExceptionPublishesUnknownError() {
        delegate.onError(fakeRichMedia("rm-1"), null)

        assertEquals(listOf(RichMediaSignals.Signal.Failed("rm-1", "unknown error")), received)
    }

    @Test
    fun errorPublishesTheExceptionMessage() {
        delegate.onError(fakeRichMedia("rm-1"), PushwooshException("network is down"))

        assertEquals(listOf(RichMediaSignals.Signal.Failed("rm-1", "network is down")), received)
    }

    @Test
    fun closePublishesNothing() {
        delegate.onClose(fakeRichMedia("rm-1"))

        assertEquals(emptyList<RichMediaSignals.Signal>(), received)
    }

    @Test
    fun shouldPresentAlwaysAllowsAndPublishesNothing() {
        assertEquals(true, delegate.shouldPresent(fakeRichMedia("rm-1")))
        assertEquals(emptyList<RichMediaSignals.Signal>(), received)
    }

    @Test
    fun missingRichMediaIsReportedAsUnknownCode() {
        delegate.onPresent(null)

        assertEquals(listOf(RichMediaSignals.Signal.Presented("?")), received)
    }
}
