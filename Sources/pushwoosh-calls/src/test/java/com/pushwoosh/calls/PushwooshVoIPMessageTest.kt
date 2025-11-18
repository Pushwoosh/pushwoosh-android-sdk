package com.pushwoosh.calls

import android.os.Bundle
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushwooshVoIPMessageTest {

    @Test
    fun testBooleanValue_whenBooleanTrue_returnsTrue() {
        val bundle = Bundle().apply {
            putBoolean("video", true)
        }
        val message = PushwooshVoIPMessage(bundle)
        assertTrue(message.hasVideo)
    }

    @Test
    fun testBooleanValue_whenBooleanFalse_returnsFalse() {
        val bundle = Bundle().apply {
            putBoolean("video", false)
        }
        val message = PushwooshVoIPMessage(bundle)
        assertFalse(message.hasVideo)
    }

    @Test
    fun testStringValue_whenStringTrue_returnsTrue() {
        val bundle = Bundle().apply {
            putString("video", "true")
        }
        val message = PushwooshVoIPMessage(bundle)
        assertTrue(message.hasVideo)
    }

    @Test
    fun testStringValue_whenStringFalse_returnsFalse() {
        val bundle = Bundle().apply {
            putString("video", "false")
        }
        val message = PushwooshVoIPMessage(bundle)
        assertFalse(message.hasVideo)
    }

    @Test
    fun testStringValue_whenString1_returnsTrue() {
        val bundle = Bundle().apply {
            putString("video", "1")
        }
        val message = PushwooshVoIPMessage(bundle)
        assertTrue(message.hasVideo)
    }

    @Test
    fun testStringValue_whenString0_returnsFalse() {
        val bundle = Bundle().apply {
            putString("video", "0")
        }
        val message = PushwooshVoIPMessage(bundle)
        assertFalse(message.hasVideo)
    }

    @Test
    fun testMissingKey_returnsDefaultFalse() {
        val bundle = Bundle()
        val message = PushwooshVoIPMessage(bundle)
        assertFalse(message.hasVideo)
    }

    @Test
    fun testNullBundle_returnsDefaultFalse() {
        val message = PushwooshVoIPMessage(null)
        assertFalse(message.hasVideo)
    }

    @Test
    fun testCallerName_whenPresent_returnsValue() {
        val bundle = Bundle().apply {
            putString("callerName", "John Doe")
        }
        val message = PushwooshVoIPMessage(bundle)
        assertEquals("John Doe", message.callerName)
    }

    @Test
    fun testCallerName_whenMissing_returnsDefault() {
        val bundle = Bundle()
        val message = PushwooshVoIPMessage(bundle)
        assertEquals("Unknown Caller", message.callerName)
    }

    @Test
    fun testMultipleFields_allParsedCorrectly() {
        val bundle = Bundle().apply {
            putString("callerName", "Jane Smith")
            putString("video", "true")
            putBoolean("supportsHolding", true)
            putString("supportsDTMF", "false")
        }
        val message = PushwooshVoIPMessage(bundle)

        assertEquals("Jane Smith", message.callerName)
        assertTrue(message.hasVideo)
        assertTrue(message.supportsHolding)
        assertFalse(message.supportsDTMF)
    }

    // callId conversion tests
    @Test
    fun testCallId_whenString_returnsString() {
        val bundle = Bundle().apply { putString("callId", "abc-123") }
        val message = PushwooshVoIPMessage(bundle)
        assertEquals("abc-123", message.callId)
    }

    @Test
    fun testCallId_whenInt_convertsToString() {
        val bundle = Bundle().apply { putInt("callId", 123) }
        val message = PushwooshVoIPMessage(bundle)
        assertEquals("123", message.callId)
    }

    @Test
    fun testCallId_whenLong_convertsToString() {
        val bundle = Bundle().apply { putLong("callId", 9876543210L) }
        val message = PushwooshVoIPMessage(bundle)
        assertEquals("9876543210", message.callId)
    }

    @Test
    fun testCallId_whenMissing_returnsNull() {
        val bundle = Bundle()
        val message = PushwooshVoIPMessage(bundle)
        assertNull(message.callId)
    }

    @Test
    fun testCancelCall_withIntCallId_bothParsed() {
        val bundle = Bundle().apply {
            putInt("callId", 42)
            putBoolean("cancelCall", true)
        }
        val message = PushwooshVoIPMessage(bundle)
        assertEquals("42", message.callId)
        assertTrue(message.cancelCall)
    }
}
