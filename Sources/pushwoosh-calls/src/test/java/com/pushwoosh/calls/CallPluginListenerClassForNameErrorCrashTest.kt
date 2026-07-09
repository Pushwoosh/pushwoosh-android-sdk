package com.pushwoosh.calls

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.pushwoosh.calls.listener.PushwooshCallEventListener
import com.pushwoosh.internal.platform.AndroidPlatformModule
import com.pushwoosh.internal.platform.prefs.PrefsProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

/**
 * Reproduces crash-callplugin-listener-classforname-error: a host that names a custom
 * CallEventListener via the AndroidManifest.xml meta-data key `com.pushwoosh.CALL_EVENT_LISTENER`,
 * where the class exists but fails to load (throwing <clinit>), makes
 * PushwooshCallPlugin.resolveCallEventListener() at :106 call the one-arg Class.forName
 * (initialize=true), which throws an ExceptionInInitializerError. That is a subtype of Error, so the
 * catch(Exception) at :112 does not intercept it and the Error escapes the plugin constructor —
 * which in production runs eagerly at SDK init (AndroidManifestConfig plugin loop → pluginClass
 * .newInstance()), crashing the app on startup for every user of that build.
 */
@RunWith(RobolectricTestRunner::class)
class CallPluginListenerClassForNameErrorCrashTest {

    companion object {
        private const val META_KEY = "com.pushwoosh.CALL_EVENT_LISTENER"
        private const val TEST_PACKAGE = "com.pushwoosh.calls.test"
        private const val THROWING_CLINIT_CLASS = "com.pushwoosh.calls.test.ThrowingClinitCallEventListener"
    }

    // Builds a context whose application meta-data carries the CALL_EVENT_LISTENER key pointing at
    // `listenerClassName`, mirroring what resolveCallEventListener() reads from the host manifest.
    private fun contextWithListenerMeta(listenerClassName: String?): Context {
        val context = Mockito.mock(Context::class.java)
        val packageManager = Mockito.mock(PackageManager::class.java)
        val applicationInfo = ApplicationInfo()
        applicationInfo.metaData = Bundle().apply { listenerClassName?.let { putString(META_KEY, it) } }
        Mockito.`when`(context.packageName).thenReturn(TEST_PACKAGE)
        Mockito.`when`(context.packageManager).thenReturn(packageManager)
        Mockito.`when`(packageManager.getApplicationInfo(TEST_PACKAGE, PackageManager.GET_META_DATA))
            .thenReturn(applicationInfo)
        return context
    }

    // The sole trigger of the throwing-clinit fixture: an ExceptionInInitializerError fires only on
    // the first init attempt per classloader, so keep exactly one initializing test in this file.
    @Test
    fun construct_throwingClinitListener_errorEscapesCatchException() {
        val context = contextWithListenerMeta(THROWING_CLINIT_CLASS)
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            mocked.`when`<Context?> { AndroidPlatformModule.getApplicationContext() }.thenReturn(context)

            val error: Throwable =
                assertThrows(ExceptionInInitializerError::class.java) { PushwooshCallPlugin() }

            val atResolve = error.stackTrace.any {
                it.className == "com.pushwoosh.calls.PushwooshCallPlugin" &&
                    it.methodName == "resolveCallEventListener"
            }
            assertTrue(
                "Error must originate at PushwooshCallPlugin.resolveCallEventListener Class.forName and escape its catch(Exception)",
                atResolve
            )
            assertFalse("crash type must be Error, not Exception", error is Exception)
        }
    }

    // Negative control: a class name that does NOT resolve throws ClassNotFoundException — an
    // Exception, which the catch(:112) swallows — so the plugin constructs normally and falls back to
    // the default PushwooshCallEventListener. Proves the repro above measures the Error-vs-Exception
    // escape, not ambient failure of the plugin constructor.
    @Test
    fun construct_unknownListenerClass_swallowedFallsBackToDefault() {
        val context = contextWithListenerMeta("com.does.not.Exist")
        val prefsProvider = Mockito.mock(PrefsProvider::class.java)
        Mockito.mockStatic(AndroidPlatformModule::class.java).use { mocked ->
            mocked.`when`<Context?> { AndroidPlatformModule.getApplicationContext() }.thenReturn(context)
            mocked.`when`<PrefsProvider?> { AndroidPlatformModule.getPrefsProvider() }.thenReturn(prefsProvider)

            val plugin = PushwooshCallPlugin()

            assertTrue(
                "unresolved listener class must fall back to the default PushwooshCallEventListener",
                plugin.callEventListener is PushwooshCallEventListener
            )
        }
    }
}
