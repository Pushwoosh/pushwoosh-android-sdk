package com.pushwoosh.notification;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

// Regression guard for crash-localnotification-cancel-null-storage: the public
// LocalNotificationRequest.unschedule()/.cancel() route into LocalNotificationReceiver
// .cancelNotification, which used to deref the nullable storage from
// RepositoryModule.getLocalNotificationStorage() without a guard (:182) and NPE onto the caller's
// thread when init never created the storage. cancel():202 had the same unguarded secondary deref.
// Both points now short-circuit on null storage, so the public API is a graceful no-op instead of a
// crash — matching the JavaDoc promise that cancel() "silently succeeds" for an invalid request.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class LocalNotificationCancelNullStorageCrashTest {

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        // Route through the guarded path: application context present (passes the :177-180 context
        // barrier) but the storage never created. PlatformTestManager provides a non-null context and
        // does NOT run RepositoryModule.init, so force the storage field null explicitly (static state
        // can leak a non-null value from a previous test in the same JVM).
        RepositoryModule.setLocalNotificationStorage(null);
    }

    @After
    public void tearDown() {
        // PrefsHelper.tearDownPrefs() dereferences getLocalNotificationStorage() without a guard,
        // so restore a non-null storage before tearing the platform down.
        RepositoryModule.setLocalNotificationStorage(mock(LocalNotificationStorage.class));
        platformTestManager.tearDown();
    }

    // Was assertThrows(NPE) at LocalNotificationReceiver.cancelNotification:182; the null-storage guard
    // now short-circuits so the public call is a graceful no-op.
    @Test
    public void unschedule_nullStorage_gracefulNoOp() {
        assertTrue(
                "guard precondition: application context must be non-null so execution reaches the storage guard",
                AndroidPlatformModule.getApplicationContext() != null);

        new LocalNotificationRequest(42).unschedule();
    }

    // cancel() derefs storage twice on null state — unschedule()->Receiver:182 and again at cancel():202;
    // both short-circuit, so the whole call is a graceful no-op instead of an NPE.
    @Test
    public void cancel_nullStorage_gracefulNoOp() {
        assertTrue(
                "guard precondition: application context must be non-null so execution reaches the storage guard",
                AndroidPlatformModule.getApplicationContext() != null);

        new LocalNotificationRequest(7).cancel();
    }

    // Negative control / happy path: with a real (non-null) storage the guard does not short-circuit and
    // unschedule() still reaches storage.removeLocalNotification. Proves the guard skips only the null
    // case, not the real work.
    @Test
    public void unschedule_nonNullStorage_removesFromStorage() {
        LocalNotificationStorage storage = mock(LocalNotificationStorage.class);
        RepositoryModule.setLocalNotificationStorage(storage);

        new LocalNotificationRequest(42).unschedule();

        verify(storage).removeLocalNotification(42);
    }

    // Negative control for cancel()'s own second guard (:202): with a real (non-null) storage cancel()
    // must fall through to storage.getLocalNotificationShown instead of short-circuiting. Proves an
    // inverted/over-eager guard there — which would silently skip removing an already-displayed
    // notification — would be caught.
    @Test
    public void cancel_nonNullStorage_reachesGetShown() {
        LocalNotificationStorage storage = mock(LocalNotificationStorage.class);
        RepositoryModule.setLocalNotificationStorage(storage);

        new LocalNotificationRequest(7).cancel();

        verify(storage).getLocalNotificationShown(7);
    }
}
