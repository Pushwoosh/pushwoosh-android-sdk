package com.pushwoosh.calls

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pushwoosh.calls.util.PushwooshCallUtils
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test to verify that PendingIntent is not cached between VoIP calls.
 *
 * This test reproduces the bug where a second incoming VoIP call displays the caller name
 * from the first call instead of the new caller name.
 *
 * Bug Description:
 * - When two VoIP calls arrive sequentially with different callerNames
 * - The fullscreen Activity shows the OLD callerName from the first call
 * - Root cause: PendingIntent.getActivity() was using fixed request code (1)
 * - Android cached the PendingIntent with old extras
 *
 * Fix:
 * - Changed request code from fixed (1) to System.currentTimeMillis().toInt()
 * - Now each call gets unique PendingIntent with correct extras
 */
@RunWith(AndroidJUnit4::class)
class VoIPCallCachingBugTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    /**
     * Tests that two sequential VoIP calls create different PendingIntents.
     *
     * This verifies that the fix (unique request codes) works correctly.
     */
    @Test
    fun testPendingIntentNotCachedBetweenCalls() {
        // Arrange - Create two different VoIP payloads
        val payload1 = Bundle().apply {
            putString("callerName", "First Caller")
            putBoolean("video", true)
            putBoolean("supportsHolding", true)
            putBoolean("supportsDTMF", true)
        }

        val payload2 = Bundle().apply {
            putString("callerName", "Second Caller")
            putBoolean("video", true)
            putBoolean("supportsHolding", true)
            putBoolean("supportsDTMF", true)
        }

        // Act - Create two notifications (simulating two VoIP calls)
        val notification1 = PushwooshCallUtils.buildIncomingCallNotification(payload1)

        // Small delay to ensure different timestamps for request codes
        Thread.sleep(5)

        val notification2 = PushwooshCallUtils.buildIncomingCallNotification(payload2)

        // Assert - Extract PendingIntents and verify they are different
        val pendingIntent1 = extractFullscreenIntent(notification1)
        val pendingIntent2 = extractFullscreenIntent(notification2)

        assertNotNull("First notification should have fullScreenIntent", pendingIntent1)
        assertNotNull("Second notification should have fullScreenIntent", pendingIntent2)

        // Key assertion: PendingIntents must be DIFFERENT objects
        // If they are the same, it means Android cached the first one
        assertNotEquals(
            "PendingIntents should be different objects (not cached)",
            pendingIntent1,
            pendingIntent2
        )

        // Additional check: different hashCodes indicate different objects
        assertNotEquals(
            "PendingIntent hashCodes should be different",
            pendingIntent1.hashCode(),
            pendingIntent2.hashCode()
        )
    }

    /**
     * Tests that callerName from payload is correctly passed to notification.
     *
     * This is a sanity check that the payload data flows correctly.
     */
    @Test
    fun testCallerNameFromPayloadIsUsedInNotification() {
        // Arrange
        val testCallerName = "Test Caller Name"
        val payload = Bundle().apply {
            putString("callerName", testCallerName)
            putBoolean("video", false)
            putBoolean("supportsHolding", false)
            putBoolean("supportsDTMF", false)
        }

        // Act
        val notification = PushwooshCallUtils.buildIncomingCallNotification(payload)

        // Assert - Check notification content contains caller name
        val extras = notification.extras
        val contentTitle = extras.getString(Notification.EXTRA_TITLE)

        assertEquals(
            "Notification title should contain callerName from payload",
            testCallerName,
            contentTitle
        )
    }

    /**
     * Tests that rapid sequential calls (within same millisecond) still get unique PendingIntents.
     *
     * This is an edge case test - in practice VoIP calls won't arrive this fast,
     * but we verify the fix handles even this extreme scenario.
     */
    @Test
    fun testRapidSequentialCallsGetUniquePendingIntents() {
        // Arrange
        val payload1 = Bundle().apply { putString("callerName", "Caller 1") }
        val payload2 = Bundle().apply { putString("callerName", "Caller 2") }
        val payload3 = Bundle().apply { putString("callerName", "Caller 3") }

        // Act - Create three notifications rapidly
        val notification1 = PushwooshCallUtils.buildIncomingCallNotification(payload1)
        val notification2 = PushwooshCallUtils.buildIncomingCallNotification(payload2)
        val notification3 = PushwooshCallUtils.buildIncomingCallNotification(payload3)

        // Assert
        val pi1 = extractFullscreenIntent(notification1)
        val pi2 = extractFullscreenIntent(notification2)
        val pi3 = extractFullscreenIntent(notification3)

        // All three should be different
        assertNotEquals("PI1 and PI2 should differ", pi1, pi2)
        assertNotEquals("PI2 and PI3 should differ", pi2, pi3)
        assertNotEquals("PI1 and PI3 should differ", pi1, pi3)

        // Verify with Set (all unique)
        val uniquePendingIntents = setOf(pi1, pi2, pi3)
        assertEquals(
            "All three PendingIntents should be unique",
            3,
            uniquePendingIntents.size
        )
    }

    /**
     * Tests that null or empty payload still creates valid notification.
     *
     * Edge case: defensive programming check.
     */
    @Test
    fun testNullPayloadDoesNotCrash() {
        // Act & Assert - Should not throw exception
        val notification = PushwooshCallUtils.buildIncomingCallNotification(null)

        assertNotNull("Notification should be created even with null payload", notification)

        val pendingIntent = extractFullscreenIntent(notification)
        assertNotNull("PendingIntent should be created even with null payload", pendingIntent)
    }

    /**
     * Helper function to extract fullScreenIntent from Notification using reflection.
     *
     * PendingIntent is stored in private field, so we use reflection to access it.
     * This is safe for testing purposes.
     */
    private fun extractFullscreenIntent(notification: Notification): PendingIntent? {
        return try {
            val field = Notification::class.java.getDeclaredField("fullScreenIntent")
            field.isAccessible = true
            field.get(notification) as? PendingIntent
        } catch (e: NoSuchFieldException) {
            fail("Failed to find fullScreenIntent field in Notification: ${e.message}")
            null
        } catch (e: IllegalAccessException) {
            fail("Failed to access fullScreenIntent field: ${e.message}")
            null
        }
    }
}
