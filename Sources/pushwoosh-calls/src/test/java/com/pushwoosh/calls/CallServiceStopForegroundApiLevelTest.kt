package com.pushwoosh.calls

import com.pushwoosh.calls.service.PushwooshCallService
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * STOP_FOREGROUND_REMOVE and the stopForeground(Int) overload are API 24+ (Build.VERSION_CODES.N);
 * the module's minSdkVersion is 23. An unguarded call links against android.app.Service.stopForeground(Int),
 * which does not exist in the API 23 android-all jar, and fails exactly like it would on a real Android
 * 6.0 device.
 */
@RunWith(RobolectricTestRunner::class)
class CallServiceStopForegroundApiLevelTest {

    @Test
    @Config(sdk = [23])
    fun onDestroy_onApi23_stopsForegroundWithoutNoSuchMethodError() {
        val service = Robolectric.buildService(PushwooshCallService::class.java).create().get()

        try {
            service.onDestroy()
        } catch (e: NoSuchMethodError) {
            fail("onDestroy() must not throw NoSuchMethodError on API 23: $e")
        } catch (e: NoSuchMethodException) {
            fail("onDestroy() must not throw NoSuchMethodException on API 23: $e")
        }

        assertTrue(Shadows.shadowOf(service).isForegroundStopped)
        assertTrue(Shadows.shadowOf(service).notificationShouldRemoved)
    }

    @Test
    @Config(sdk = [34])
    fun onDestroy_onApi34_stopsForegroundWithRemoveFlag() {
        val service = Robolectric.buildService(PushwooshCallService::class.java).create().get()

        service.onDestroy()

        assertTrue(Shadows.shadowOf(service).isForegroundStopped)
        assertTrue(Shadows.shadowOf(service).notificationShouldRemoved)
    }
}
