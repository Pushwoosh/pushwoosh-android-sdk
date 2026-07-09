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

package com.pushwoosh.location.network;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.ContextWrapper;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.location.NearestZonesManager;
import com.pushwoosh.location.geofence.GeofenceTracker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionDeclaredChecker;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;
import com.pushwoosh.location.network.repository.UpdateNearestRepository;
import com.pushwoosh.location.scheduler.ServiceScheduler;
import com.pushwoosh.location.storage.LocationPrefs;
import com.pushwoosh.location.storage.NearestZonesStorage;
import com.pushwoosh.location.tracker.LocationTracker;
import com.pushwoosh.location.tracker.OnGetLastLocationCallback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;
import java.util.concurrent.Executor;

/**
 * Regression guard for crash candidate #16 (crash-updatenearest-callback-async-escape).
 *
 * <p><b>Signal:</b> {@code UpdateNearestRepository.applyNearestJob:66} dispatches the geo-zone
 * result callback on {@code AsyncTask.THREAD_POOL_EXECUTOR} through a GMS Task, which fires the
 * listener <em>after</em> the calling method returns. The Api21 Service path only wraps the
 * synchronous {@code doInBackground()} in a barrier ({@code JobExecutor.execute:156}
 * {@code BackgroundExecutor.execute(...)} whose {@code wrapWithErrorHandling} catches
 * {@code Throwable}). By the time the async callback runs on the raw executor, that barrier has
 * already closed — so a {@code Throwable} thrown from inside the callback escapes uncaught and
 * kills the process. The concrete throw on the crash path is the job-limit
 * {@link IllegalStateException} from {@code GeoLocationServiceSchedulerApi21.scheduleJob:80}
 * ({@code jobScheduler.schedule(...)}, thrown when the host app already holds &ge;100 distinct
 * jobs), reached unconditionally via {@code GetNearestZoneJobApplier}'s result lambda &rarr;
 * {@code NearestZonesManager.updateState:196} &rarr; {@code ServiceScheduler.scheduleNearestGeoZones:55}.
 *
 * <p>This is the same async-escape shape as
 * {@code crash-postevent-callback-async-escape} (raw {@code main.post} outside the barrier) and
 * {@code crash-modalrichmedia-showatlocation-async} ({@code decorView.post} outside try/catch):
 * a throw dispatched onto a non-wrapped executor slips past the callback-shielding barrier.
 *
 * <p><b>The fix (in place):</b> the async dispatch lambda body in
 * {@code UpdateNearestRepository.applyNearestJob:66} is wrapped in try/catch(Throwable) — an
 * equivalent barrier re-attached at the dispatch point, so a throw on the raw executor is caught
 * and logged instead of escaping uncaught. These tests are the inverted regression guard: they
 * assert the throw is now caught (was {@code assertThrows} before the fix).
 *
 * <h3>Stand-ins (faithful, documented per reproduce-crash phases 3 &amp; 5)</h3>
 * <ul>
 *   <li><b>The GMS Task async dispatch</b> is replaced by {@link CapturingLocationTracker}: in the
 *       escape case it <em>captures</em> the callback and fires it later (on the test thread) — after
 *       the barrier has closed; in the barrier-control case it fires the callback synchronously,
 *       inside the barrier. Determinism is substituted for the {@code THREAD_POOL_EXECUTOR} timing;
 *       the escape is faithful because no SDK barrier wraps the deferred callback on either the real
 *       pool thread or the test thread. In production the escaped throw hits the pool thread's
 *       uncaught-exception handler &rarr; process kill.</li>
 *   <li><b>The Service barrier</b> ({@code BackgroundExecutor.execute}'s {@code wrapWithErrorHandling},
 *       catch {@code Throwable}) is modeled by {@link #runInsideServiceBarrier} — a plain
 *       try/catch({@code Throwable}). Behaviorally identical; its absence from the escaped stack is
 *       the proof of escape.</li>
 *   <li><b>The job-limit ISE</b> (environmental: the host already holds &ge;100 jobs — Robolectric's
 *       {@code ShadowJobScheduler} does not enforce the quota) is raised by a mock
 *       {@link JobScheduler} from {@link #jobLimitScheduler}, wired behind {@link #systemServiceOverride}.
 *       It is thrown at
 *       the real {@code GeoLocationServiceSchedulerApi21.scheduleJob:80} call site, through the real
 *       Api21 scheduler + {@code ServiceScheduler} + {@code NearestZonesManager} bodies — so the
 *       escaped stack carries the real crash-line frame.</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
@LooperMode(LooperMode.Mode.LEGACY)
public class UpdateNearestCallbackAsyncEscapeCrashTest {

    @Before
    public void setUp() {
        // Faithful to a normally-initialized SDK: LocationModule.<clinit> succeeds so
        // jobLocationIdProvider() (dereferenced eagerly in provideJobInfo:91) returns the real
        // provider with its hardcoded nearest-service job id.
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
    }

    // ---------------------------------------------------------------------------------------------
    // Stand-in: LocationTracker modeling the GMS Task dispatch on AsyncTask.THREAD_POOL_EXECUTOR.
    // deferred=true  -> capture the callback, fire it later (async escape: after the barrier closes)
    // deferred=false -> fire the callback synchronously (barrier control: inside doInBackground)
    // ---------------------------------------------------------------------------------------------
    private static final class CapturingLocationTracker implements LocationTracker {
        private final boolean deferred;
        private OnGetLastLocationCallback captured;

        CapturingLocationTracker(boolean deferred) {
            this.deferred = deferred;
        }

        @Override
        public void getLocation(Executor executor, OnGetLastLocationCallback callback) {
            if (deferred) {
                captured = callback; // GMS Task fires AFTER applyNearestJob returns
            } else {
                callback.onGetLastLocation(null);
            }
        }

        void fireDeferred() {
            captured.onGetLastLocation(null);
        }

        @Override
        public void getLocation(OnGetLastLocationCallback callback) {
            callback.onGetLastLocation(null);
        }

        @Override
        public void requestLocationUpdates(boolean highAccuracy) {}

        @Override
        public void onDestroy() {}

        @Override
        public boolean isLocationAvailable() {
            return true;
        }
    }

    // Models BackgroundExecutor.execute()'s wrapWithErrorHandling: run doInBackground inside a
    // catch(Throwable). Returns what it caught (null if nothing) — the barrier "swallow" surface.
    private static Throwable runInsideServiceBarrier(Runnable doInBackground) {
        try {
            doInBackground.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    // A mock JobScheduler that raises the platform job-limit IllegalStateException. Wired behind a
    // ContextWrapper (systemServiceOverride) at the real GeoLocationServiceSchedulerApi21.scheduleJob:80
    // call site; returned so tests can verify the crash line was actually reached (non-vacuity).
    private static JobScheduler jobLimitScheduler() {
        JobScheduler throwing = mock(JobScheduler.class);
        when(throwing.schedule(any(JobInfo.class)))
                .thenThrow(new IllegalStateException("Apps may not schedule more than 100 distinct jobs"));
        return throwing;
    }

    private static Context systemServiceOverride(JobScheduler jobScheduler) {
        return new ContextWrapper(RuntimeEnvironment.getApplication()) {
            @Override
            public Object getSystemService(String name) {
                if (Context.JOB_SCHEDULER_SERVICE.equals(name)) {
                    return jobScheduler;
                }
                return super.getSystemService(name);
            }
        };
    }

    // Builds the REAL crash chain (UpdateNearestRepository -> GetNearestZoneJobApplier result lambda
    // -> NearestZonesManager.updateState -> ServiceScheduler -> GeoLocationServiceSchedulerApi21),
    // driven off the given tracker (async escape vs sync) and context (throwing vs benign scheduler).
    private static GetNearestZoneJobApplier buildRealChain(CapturingLocationTracker tracker, Context schedulerContext) {
        NearestZonesStorage storage = mock(NearestZonesStorage.class);
        // Non-null cached zones + forceUpdate=false => the cached branch fires callback.process
        // synchronously (no network), reaching the result lambda that unconditionally schedules.
        when(storage.getAll()).thenReturn(Collections.<com.pushwoosh.location.data.GeoZone>emptyList());

        UpdateNearestRepository repository = new UpdateNearestRepository(storage, tracker);
        ServiceScheduler serviceScheduler = new ServiceScheduler(schedulerContext);

        NearestZonesManager nearestZonesManager = new NearestZonesManager(
                mock(GeofenceTracker.class),
                mock(LocationPrefs.class),
                tracker,
                mock(LocationPermissionChecker.class),
                mock(FineLocationPermissionChecker.class),
                mock(BackgroundLocationPermissionDeclaredChecker.class),
                mock(BackgroundLocationPermissionChecker.class),
                serviceScheduler);

        return new GetNearestZoneJobApplier(nearestZonesManager, repository, tracker);
    }

    // ---------------------------------------------------------------------------------------------
    // REGRESSION GUARD (was CRASH): before the fix, the job-limit ISE thrown from the deferred (async)
    // callback escaped the Service barrier uncaught on the raw executor -> process kill. The
    // dispatch-point guard now catches it: firing the deferred callback must NOT throw.
    // ---------------------------------------------------------------------------------------------
    @Test
    public void asyncCallback_jobLimitISE_caughtByDispatchGuard() {
        JobScheduler scheduler = jobLimitScheduler();
        Context ctx = systemServiceOverride(scheduler);
        CapturingLocationTracker tracker = new CapturingLocationTracker(/*deferred=*/ true);
        GetNearestZoneJobApplier applier = buildRealChain(tracker, ctx);

        // doInBackground -> loadNearestGeoZones -> applyNearestJob dispatches onto the (deferred)
        // executor and returns; the barrier's try closes seeing nothing.
        assertNull(
                "barrier must see nothing: callback deferred past doInBackground's return",
                runInsideServiceBarrier(() -> applier.loadNearestGeoZones(false)));

        // The GMS callback now fires on the raw executor (modeled) -> real chain -> scheduleJob:80
        // throws the job-limit ISE. Pre-fix this escaped uncaught; the dispatch-point guard swallows it.
        tracker.fireDeferred(); // must not throw

        // Non-vacuity: the real chain reached the crash line and the scheduler really threw — the guard
        // caught a genuine ISE, it did not pass by silently skipping the schedule call.
        verify(scheduler).schedule(any(JobInfo.class));
    }

    // ---------------------------------------------------------------------------------------------
    // The guard sits at the DISPATCH POINT, not the outer barrier: even fired synchronously (inside
    // doInBackground), the ISE is caught at the lambda before it can reach the Service barrier — so
    // the barrier sees nothing. Proves the fix covers both dispatch modes and does not depend on the
    // outer barrier's presence/timing.
    // ---------------------------------------------------------------------------------------------
    @Test
    public void syncCallback_jobLimitISE_caughtByDispatchGuardNotBarrier() {
        JobScheduler scheduler = jobLimitScheduler();
        Context ctx = systemServiceOverride(scheduler);
        CapturingLocationTracker tracker = new CapturingLocationTracker(/*deferred=*/ false);
        GetNearestZoneJobApplier applier = buildRealChain(tracker, ctx);

        Throwable caughtByBarrier = runInsideServiceBarrier(() -> applier.loadNearestGeoZones(false));

        assertNull("dispatch-point guard must catch the ISE before the outer barrier sees it", caughtByBarrier);
        verify(scheduler).schedule(any(JobInfo.class));
    }

    // ---------------------------------------------------------------------------------------------
    // NEGATIVE CONTROL: with a benign JobScheduler (schedule succeeds), the deferred callback runs
    // the full chain and does NOT throw. Proves the harness is non-vacuous and the job-limit ISE is
    // a necessary condition — not ambient wiring crashing.
    // ---------------------------------------------------------------------------------------------
    @Test
    public void asyncCallback_schedulerSucceeds_noThrow() {
        Context ctx = RuntimeEnvironment.getApplication(); // real ShadowJobScheduler: schedule() succeeds
        CapturingLocationTracker tracker = new CapturingLocationTracker(/*deferred=*/ true);
        GetNearestZoneJobApplier applier = buildRealChain(tracker, ctx);

        assertNull(runInsideServiceBarrier(() -> applier.loadNearestGeoZones(false)));
        tracker.fireDeferred(); // no throw
    }
}
