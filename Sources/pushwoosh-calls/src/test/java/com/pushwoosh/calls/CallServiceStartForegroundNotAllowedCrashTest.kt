package com.pushwoosh.calls

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Context
import android.content.Intent
import com.pushwoosh.calls.service.PushwooshCallService
import com.pushwoosh.calls.test.ShadowThrowingStartForegroundService
import com.pushwoosh.calls.util.Constants
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.platform.app.AppInfoProvider
import com.pushwoosh.internal.platform.prefs.PrefsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.MockedStatic
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Deterministic reproduction of crash candidate #15: crash-callservice-startforeground-fgs-not-allowed.
 *
 * On API 31+ the platform Service.startForeground() throws ForegroundServiceStartNotAllowedException
 * (a subtype of IllegalStateException) when a foreground service is started from the background without
 * an allowed exemption. PushwooshCallService.onStartCommand (a Service lifecycle entry) and its private
 * startForegroundNotification() call startForeground() with no try/catch, so that OS-side throw escapes
 * uncaught through both real frames into the framework → host process crash.
 *
 * The OS-side throw is OEM/version/race-dependent (the while-in-use FGS exemption failing on the ONGOING
 * Accept path). We substitute determinism for that timing: ShadowThrowingStartForegroundService makes
 * startForeground() throw every time. The stand-in replaces only the OS decision to reject the start;
 * the reach path (PushwooshCallService.startForegroundNotification -> onStartCommand) is real, unmodified
 * code. The shadow is wired in per-method, so the "exemption granted" barrier control below runs with the
 * stock non-throwing ShadowService.
 *
 * sdk=34 is required: ForegroundServiceStartNotAllowedException exists only on API 31+, and it is exactly
 * the API level range where startForeground() can reject a background FGS start. The signal's stock
 * sibling tests run on Robolectric's lower default SDK, which is why they don't exercise this throw.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CallServiceStartForegroundNotAllowedCrashTest {

    // The notification-building path (buildOngoingCallNotification) reads CallPrefs and the app-info
    // provider through AndroidPlatformModule; mockStatic zeroes every static, so stub the two providers
    // the ONGOING path touches before startForeground(). providePrefs() may return null — PreferenceStringValue
    // tolerates it; a fake package name is fine — AppIconHelper.getAppIcon swallows PM lookup failures.
    private fun stubReachProviders(mocked: MockedStatic<AndroidPlatformModule>, context: Context) {
        mocked.`when`<Context?> { AndroidPlatformModule.getApplicationContext() }.thenReturn(context)

        val prefsProvider = Mockito.mock(PrefsProvider::class.java)
        mocked.`when`<PrefsProvider?> { AndroidPlatformModule.getPrefsProvider() }.thenReturn(prefsProvider)

        val appInfoProvider = Mockito.mock(AppInfoProvider::class.java)
        Mockito.`when`(appInfoProvider.packageName).thenReturn(context.packageName)
        mocked.`when`<AppInfoProvider?> { AndroidPlatformModule.getAppInfoProvider() }.thenReturn(appInfoProvider)
    }

    // ================= reproduction =================

    // ONGOING Accept path: a valid ONGOING intent + non-null context builds the notification and reaches
    // startForeground(), which (OS-denied via the shadow) throws. The exception must escape uncaught.
    @Test
    @Config(shadows = [ShadowThrowingStartForegroundService::class])
    fun onStartCommand_ongoing_startForegroundDenied_escapesUncaught() {
        val realContext: Context = RuntimeEnvironment.getApplication()
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            stubReachProviders(mocked, realContext)

            val service = Robolectric.buildService(PushwooshCallService::class.java).create().get()
            val intent = Intent()
                .setAction(Constants.PW_POST_ONGOING_CALL_ACTION)
                .putExtra("callerName", "Test Caller")

            val error = assertThrows(ForegroundServiceStartNotAllowedException::class.java) {
                service.onStartCommand(intent, 0, 1)
            }

            // The escaping type is an IllegalStateException subtype (the signal's predicted type).
            assertTrue(
                "escaping type must be an IllegalStateException subtype",
                error is IllegalStateException)
            // Origin + escape proof: it left onStartCommand via the unguarded startForegroundNotification.
            val frames = error.stackTrace.joinToString("\n") { it.toString() }
            assertTrue(
                "must propagate through startForegroundNotification (the unguarded call site)",
                frames.contains("startForegroundNotification"))
            assertTrue(
                "must escape onStartCommand (the Service lifecycle entry, unwrapped)",
                frames.contains("onStartCommand"))
        }
    }

    // ================= barrier controls =================

    // Barrier held (the crash's normal preventer): when the OS grants the FGS exemption, startForeground()
    // does not throw. Running with the stock non-throwing ShadowService (no shadow override here), the same
    // ONGOING drive completes and returns START_NOT_STICKY. Proves the crash requires the OS-side denial
    // (condition removed -> no crash), i.e. the repro assert is not vacuous.
    @Test
    fun barrier_ongoing_startForegroundGranted_completesNormally() {
        val realContext: Context = RuntimeEnvironment.getApplication()
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            stubReachProviders(mocked, realContext)

            val service = Robolectric.buildService(PushwooshCallService::class.java).create().get()
            val intent = Intent()
                .setAction(Constants.PW_POST_ONGOING_CALL_ACTION)
                .putExtra("callerName", "Test Caller")

            val result = service.onStartCommand(intent, 0, 1)
            assertEquals(
                "with the FGS exemption granted, onStartCommand completes",
                Service.START_NOT_STICKY, result)
        }
    }

    // Barrier held (second preventer, already the #21 fix): a null context makes buildOngoingCallNotification
    // return null, so onStartCommand takes stopSelf() and never reaches startForeground(). Even with the
    // throwing shadow wired in, no exception is thrown — the crash needs a non-null context to reach the
    // unguarded startForeground() call.
    @Test
    @Config(shadows = [ShadowThrowingStartForegroundService::class])
    fun barrier_ongoing_nullContext_neverReachesStartForeground() {
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            mocked.`when`<Context?> { AndroidPlatformModule.getApplicationContext() }.thenReturn(null)

            val service = Robolectric.buildService(PushwooshCallService::class.java).create().get()
            val intent = Intent().setAction(Constants.PW_POST_ONGOING_CALL_ACTION)

            val result = service.onStartCommand(intent, 0, 1)
            assertEquals(
                "null context short-circuits to stopSelf() before startForeground()",
                Service.START_NOT_STICKY, result)
        }
    }
}
