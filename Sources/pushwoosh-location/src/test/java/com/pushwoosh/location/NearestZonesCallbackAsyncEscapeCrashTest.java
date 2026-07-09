/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.location.geofence.GeofenceTracker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionDeclaredChecker;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.location.scheduler.ServiceScheduler;
import com.pushwoosh.location.storage.LocationPrefs;
import com.pushwoosh.location.tracker.LocationTracker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

/**
 * Regression guard for crash candidate #18 (crash-nearestzones-notifycallback-async-escape).
 *
 * <p><b>Signal:</b> {@code NearestZonesManager.successNotifyCallback}/{@code failedNotifyCallback} used to
 * deliver the host location callback through a raw {@code handler.post(() -> callback.get().process(...))},
 * where {@code handler} was a bare
 * {@code new Handler(Looper.getMainLooper())} — <b>not</b>
 * {@code BackgroundExecutor.main}. Every other callback delivery in the SDK is shielded by
 * {@code BackgroundExecutor.main}'s {@code wrapWithErrorHandling}, which catches {@code Throwable}
 * ({@code BackgroundExecutor.java:62-69}). The posted body ran in a fresh main-Looper message
 * <em>after</em> the synchronous half ({@code successProvidingLocation}/{@code failedProvidingLocation},
 * driven in prod by {@code GoogleLocationTracker.successRequestLocation:150}/{@code failedRequestLocation:155}
 * &rarr; the {@code LocationTrackerCallbackWrapper}) had already returned — so it carried no try/catch, and
 * a host callback whose {@code process()} threw escaped uncaught on the main Looper &rarr; process kill.
 *
 * <p><b>Fix:</b> both deliveries now route through {@code BackgroundExecutor.main} (the same
 * {@code Throwable}-catching barrier every sibling callback uses), so the host throw is absorbed on the main
 * Looper instead of escaping. The two escape tests below now assert that graceful outcome — the drain does
 * not throw, yet the callback was still invoked — mirroring the barrier control.
 *
 * <p>This is the same async-escape shape as {@code crash-postevent-callback-async-escape} (raw
 * {@code main.post} outside the barrier) and {@code crash-updatenearest-callback-async-escape}
 * (raw {@code THREAD_POOL_EXECUTOR} outside the barrier): a throw dispatched onto a non-wrapped
 * executor slips past the callback-shielding barrier.
 *
 * <h3>Stand-ins (faithful, documented per reproduce-crash phases 3 &amp; 5)</h3>
 * <ul>
 *   <li><b>The GMS location-fix delivery</b> is replaced by calling the real
 *       {@code LocationTrackerCallback} overrides directly ({@link NearestZonesManager#successProvidingLocation()}
 *       / {@link NearestZonesManager#failedProvidingLocation()}) — the exact synchronous half that the
 *       {@code LocationTrackerCallbackWrapper} invokes in production. Only the way the fix arrives is
 *       short-circuited; the raw {@code handler.post} + host-callback invocation is real SDK code.</li>
 *   <li><b>The race</b> (the posted body runs in a later main-Looper message, after the synchronous half
 *       returns) is made deterministic with {@code ShadowLooper.pauseMainLooper()} before the trigger and
 *       {@code idleMainLooper()} after — substituting determinism for the message-queue timing. The
 *       <em>outcome</em> (an uncaught throw draining on the main Looper) is faithful; production's main
 *       Looper has no SDK-level uncaught handler, so the process dies.</li>
 *   <li><b>The intended shield</b> ({@code BackgroundExecutor.main}'s {@code wrapWithErrorHandling},
 *       catch {@code Throwable}) is modeled by {@link #runInsideMainBarrier} — a plain try/catch({@code
 *       Throwable}). This class has no shielded sibling delivery path (the raw {@code handler.post} is the
 *       <em>only</em> way these callbacks are delivered), so the barrier control demonstrates what the
 *       shield <em>would</em> have done to the identical throw, rather than replaying a real sibling.</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
@LooperMode(LooperMode.Mode.LEGACY)
public class NearestZonesCallbackAsyncEscapeCrashTest {

    // A host location callback whose process() optionally throws (the HOST-DEPENDENT precondition:
    // a real app callback touching a dead Activity / null view / stale reference), counting invocations.
    private static final class ThrowingHostCallback implements Callback<Void, LocationNotAvailableException> {
        private final boolean throwOnProcess;
        private int invocations = 0;

        ThrowingHostCallback(boolean throwOnProcess) {
            this.throwOnProcess = throwOnProcess;
        }

        @Override
        public void process(Result<Void, LocationNotAvailableException> result) {
            invocations++;
            if (throwOnProcess) {
                throw new RuntimeException("host callback touched a dead Activity");
            }
        }
    }

    // Builds a real NearestZonesManager with mocked deps. geolocationStarted() is stubbed non-null so
    // start()/stop() (which call .set(...)) don't NPE before control reaches the crash point.
    private static NearestZonesManager buildManager() {
        LocationPrefs prefs = mock(LocationPrefs.class);
        when(prefs.geolocationStarted()).thenReturn(mock(PreferenceBooleanValue.class));
        return new NearestZonesManager(
                mock(GeofenceTracker.class),
                prefs,
                mock(LocationTracker.class),
                mock(LocationPermissionChecker.class),
                mock(FineLocationPermissionChecker.class),
                mock(BackgroundLocationPermissionDeclaredChecker.class),
                mock(BackgroundLocationPermissionChecker.class),
                mock(ServiceScheduler.class));
    }

    // Models BackgroundExecutor.main()'s wrapWithErrorHandling: run the callback body inside a
    // catch(Throwable). Returns what it caught (null if nothing) — the barrier "swallow" surface.
    private static Throwable runInsideMainBarrier(Runnable body) {
        try {
            body.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // GRACEFUL (success path): the host throw dispatched via BackgroundExecutor.main is absorbed by its
    // Throwable barrier instead of escaping the main Looper. The synchronous half (successProvidingLocation)
    // only enqueues the deferred hop and returns; draining it later invokes the host callback (whose throw
    // is swallowed) and does NOT crash. Before the fix this drain escaped uncaught -> process kill.
    // ---------------------------------------------------------------------------------------------
    @Test
    public void successCallback_hostThrows_swallowedByBarrier() {
        NearestZonesManager manager = buildManager();
        ThrowingHostCallback host = new ThrowingHostCallback(/*throwOnProcess=*/ true);
        manager.start(host); // registers WeakReference<callback>; host held strongly below

        ShadowLooper.pauseMainLooper();
        manager.successProvidingLocation(); // real synchronous half -> successNotifyCallback -> BackgroundExecutor.main
        assertEquals("body deferred to the main Looper: host not invoked yet", 0, host.invocations);

        ShadowLooper.idleMainLooper(); // drains the deferred hop; the barrier swallows the throw -> no escape

        // non-vacuous: the callback WAS delivered (and its throw swallowed by the barrier), not silently dropped.
        assertEquals("host callback invoked exactly once", 1, host.invocations);
    }

    // ---------------------------------------------------------------------------------------------
    // GRACEFUL (failed path, twin): the failure-callback delivery uses the same BackgroundExecutor.main
    // hop, so the identical throw is swallowed by the barrier from failedNotifyCallback.
    // ---------------------------------------------------------------------------------------------
    @Test
    public void failedCallback_hostThrows_swallowedByBarrier() {
        NearestZonesManager manager = buildManager();
        ThrowingHostCallback host = new ThrowingHostCallback(/*throwOnProcess=*/ true);
        manager.start(host);

        ShadowLooper.pauseMainLooper();
        manager.failedProvidingLocation(); // stop() + failedNotifyCallback -> BackgroundExecutor.main
        assertEquals("body deferred to the main Looper: host not invoked yet", 0, host.invocations);

        ShadowLooper.idleMainLooper(); // drains the deferred hop; the barrier swallows the throw -> no escape

        assertEquals("host callback invoked exactly once", 1, host.invocations);
    }

    // ---------------------------------------------------------------------------------------------
    // BARRIER CONTROL: the SAME throwing host callback, run inside a catch(Throwable) barrier (what
    // BackgroundExecutor.main would have wrapped it in), IS swallowed. Same throw, opposite outcome —
    // the raw handler.post (bypassing the shield) is exactly what defeats the guard.
    // ---------------------------------------------------------------------------------------------
    @Test
    public void sameThrow_insideMainBarrier_swallowed() {
        ThrowingHostCallback host = new ThrowingHostCallback(/*throwOnProcess=*/ true);

        Throwable caught = runInsideMainBarrier(() -> host.process(Result.fromData(null)));

        assertTrue("shielded throw must be swallowed, got: " + caught, caught instanceof RuntimeException);
        assertEquals("host callback invoked exactly once", 1, host.invocations);
    }

    // ---------------------------------------------------------------------------------------------
    // NON-VACUITY CONTROL: with a host callback that does NOT throw, draining the same posted body
    // does not crash. Proves the escape test measures the host throw, not ambient wiring.
    // ---------------------------------------------------------------------------------------------
    @Test
    public void successCallback_hostDoesNotThrow_noEscape() {
        NearestZonesManager manager = buildManager();
        ThrowingHostCallback host = new ThrowingHostCallback(/*throwOnProcess=*/ false);
        manager.start(host);

        ShadowLooper.pauseMainLooper();
        manager.successProvidingLocation();
        ShadowLooper.idleMainLooper(); // drains the post; must not throw

        assertEquals("host callback invoked exactly once", 1, host.invocations);
    }
}
