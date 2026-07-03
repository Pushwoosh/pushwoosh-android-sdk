package com.pushwoosh.calls.util

import android.app.Service
import android.content.Context
import android.content.Intent
import com.pushwoosh.calls.service.PushwooshCallService
import com.pushwoosh.internal.platform.AndroidPlatformModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Regression guard for crash candidate #20: crash-buildincomingcallnotification-context-null.
 *
 * Before the fix, PushwooshCallUtils.buildIncomingCallNotification did a Kotlin non-null cast
 *   `val context = AndroidPlatformModule.getApplicationContext() as Context`
 * which threw NullPointerException when getApplicationContext() was null, escaping the unwrapped
 * PushwooshCallService.onStartCommand → host process crash.
 *
 * The fix replaced the cast with an early-return null-guard (mirrors the sibling getFullscreenIntent:123):
 * a null context now makes buildIncomingCallNotification return null, and onStartCommand skips the
 * foreground start and returns START_NOT_STICKY. These tests assert that graceful behavior.
 */
@RunWith(RobolectricTestRunner::class)
class BuildIncomingCallNotificationNullContextTest {

    // Verifies that a null application context no longer crashes the direct call: the method
    // returns null instead of throwing the `as Context` cast NPE.
    @Test
    fun directCall_nullContext_returnsNullGracefully() {
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            mocked.`when`<Context?> { AndroidPlatformModule.getApplicationContext() }.thenReturn(null)
            val notification = PushwooshCallUtils.buildIncomingCallNotification(null)
            assertNull("null context must yield a null notification, not a thrown NPE", notification)
        }
    }

    // Verifies that the escape path is closed and the foreground contract is released: with a null
    // context, onStartCommand for the INCOMING action does not throw, returns START_NOT_STICKY, and
    // stops the service (so a startForegroundService() launch does not expire into
    // ForegroundServiceDidNotStartInTimeException).
    @Test
    fun service_nullContext_onStartCommandStopsServiceGracefully() {
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            mocked.`when`<Context?> { AndroidPlatformModule.getApplicationContext() }.thenReturn(null)
            val service = Robolectric.buildService(PushwooshCallService::class.java).create().get()
            val intent = Intent().setAction(Constants.PW_POST_INCOMING_CALL_ACTION)
            val result = service.onStartCommand(intent, 0, 1)
            assertEquals("null context must return START_NOT_STICKY", Service.START_NOT_STICKY, result)
            assertTrue("service must stop itself to release the foreground contract", shadowOf(service).isStoppedBySelf)
        }
    }

    // ================= negative controls =================

    // (1) non-null context: the method still completes and returns a real Notification → proves the
    //     guard only short-circuits the null case, it did not break the happy path.
    @Test
    fun negativeControl_nonNullContext_returnsNotification() {
        val realContext: Context = RuntimeEnvironment.getApplication()
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            mocked.`when`<Context?> { AndroidPlatformModule.getApplicationContext() }.thenReturn(realContext)
            val notification = PushwooshCallUtils.buildIncomingCallNotification(null)
            assertNotNull("with a non-null context the method completes and returns a Notification", notification)
        }
    }

    // (2) with a non-null context, onStartCommand for the INCOMING action completes without throwing
    //     (the foreground machinery may no-op under Robolectric) → the happy reach path still works.
    @Test
    fun negativeControl_service_nonNullContext_doesNotThrow() {
        val realContext: Context = RuntimeEnvironment.getApplication()
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            mocked.`when`<Context?> { AndroidPlatformModule.getApplicationContext() }.thenReturn(realContext)
            val service = Robolectric.buildService(PushwooshCallService::class.java).create().get()
            val intent = Intent().setAction(Constants.PW_POST_INCOMING_CALL_ACTION)
            service.onStartCommand(intent, 0, 1)
        }
    }
}
