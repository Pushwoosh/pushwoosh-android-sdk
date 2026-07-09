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

package com.pushwoosh.location.geofence;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.location.Location;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.storage.GeoZoneStorage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;

/**
 * Reproduction harness for crash candidate #17 (crash-geofencetracker-pushgeozones-cme).
 *
 * <p><b>Signal:</b> {@code GeofenceTrackerImp.pushGeoZones} is a plain, unsynchronized
 * {@link ArrayList}. Two independent async entries mutate and read it without a shared lock:
 * <ul>
 *   <li><b>Iterator (main-looper):</b> {@code GeofenceTrackerImp.locationUpdated}
 *       {@code for (GeoZone geoZone : pushGeoZones)} — GMS {@code LocationCallback} is delivered on
 *       the main looper, so this iteration runs on main, uncaught by any try/catch on the whole
 *       {@code onLocationResult -> GoogleLocationTracker -> wrapper -> locationUpdated} chain.</li>
 *   <li><b>Structural mutator (background):</b> {@code GeofenceTrackerImp.updatePushGeoZones}
 *       {@code pushGeoZones.remove(geoZone)} — reached from a scheduled geozone-refresh job whose
 *       result callback escapes onto {@code AsyncTask.THREAD_POOL_EXECUTOR}
 *       (the async-escape rooted at {@code UpdateNearestRepository.applyNearestJob}, same root as
 *       crash-updatenearest-callback-async-escape) &rarr; {@code NearestZonesManager.updateZones}
 *       calls {@code geofenceTracker.updateZones} <em>directly, without hopping to main</em> &rarr;
 *       {@code updateZones -> updatePushGeoZones -> pushGeoZones.remove}.</li>
 * </ul>
 * When the background {@code remove} bumps the list's {@code modCount} while the main iteration is
 * in progress, the next {@code Itr.next()} in {@code locationUpdated} trips fail-fast and throws
 * an uncaught {@link ConcurrentModificationException} on the main thread &rarr; process kill.
 *
 * <h3>Stand-in (faithful, documented per reproduce-crash phases 3 &amp; 5)</h3>
 * The crash is a two-thread timing race (background mutator overlapping the main iterator). Per
 * phase 5, the racy timing is replaced by a deterministic stand-in rather than won by hand:
 * {@link TriggeringZone} — the first element of {@code pushGeoZones} — invokes the <b>real</b>
 * {@code tracker.updateZones(...)} from inside its {@code distanceTo(...)} (called from the
 * {@code locationUpdated} loop body), i.e. exactly in the middle of the main iteration. The
 * structural removal therefore flows through the real mutator {@code updatePushGeoZones}, and the
 * CME is thrown at the real crash site {@code locationUpdated}. Only the <em>window opener</em> (a
 * background thread happening to run mid-iteration) is substituted for determinism; the outcome —
 * the exact throw at the exact site — is faithful. In production the same {@code modCount} bump
 * happens on {@code THREAD_POOL_EXECUTOR} and the uncaught CME hits the main-thread handler.
 *
 * <h3>Regression guard (inverted)</h3>
 * The fix makes {@code pushGeoZones} a snapshot-on-iterate {@link java.util.concurrent.CopyOnWriteArrayList},
 * so the main iteration runs over a frozen snapshot and the concurrent background {@code remove} no
 * longer trips fail-fast. These tests therefore assert the <em>graceful</em> outcome the fix
 * introduced: the interleaved scenario completes with no throw, while the concurrent remove still
 * takes effect on the live list mid-iteration (non-vacuity — the exact {@code modCount} bump that
 * used to crash). The tests populate the production-created list via {@code addAll} instead of
 * reassigning it, so reverting the field back to a plain {@code ArrayList} makes the CME — and these
 * tests — return.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
@LooperMode(LooperMode.Mode.LEGACY)
public class GeofenceTrackerPushGeoZonesCmeCrashTest {

    @Before
    public void setUp() {
        // GeofenceTrackerImp's constructor subscribes on the EventBus and logs via PWLog; a
        // normally-initialized platform keeps those wired the way production has them.
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
    }

    // A GeoZone with a deterministic distanceTo — no dependency on Robolectric's Location math.
    private static final class StubZone extends GeoZone {
        private final int distance;

        StubZone(String name, long range, int distance) {
            super(name, 0.0, 0.0, range, 0L);
            this.distance = distance;
        }

        @Override
        public int distanceTo(Location location) {
            return distance;
        }
    }

    // Stand-in for "a background thread fires the geozone-refresh mutator mid-iteration": the first
    // element of pushGeoZones runs the REAL tracker.updateZones(...) from inside distanceTo (called
    // from the locationUpdated loop body), then reports "far" so the loop does not break and proceeds
    // to the next Itr.next() — which trips fail-fast in locationUpdated.
    private static final class TriggeringZone extends GeoZone {
        private final Runnable onDistance;
        private boolean fired;

        TriggeringZone(String name, Runnable onDistance) {
            super(name, 0.0, 0.0, 100L, 0L);
            this.onDistance = onDistance;
        }

        @Override
        public int distanceTo(Location location) {
            if (!fired) {
                fired = true;
                onDistance.run();
            }
            return Integer.MAX_VALUE;
        }
    }

    private static GeoZoneStorage storageWith(GeoZone... zones) {
        GeoZoneStorage storage = mock(GeoZoneStorage.class);
        when(storage.getGeoZones()).thenReturn(Arrays.asList(zones));
        return storage;
    }

    private static GeofenceTrackerImp newTracker(GeoZoneStorage storage) {
        return new GeofenceTrackerImp(
                /*geofencer*/ null,
                mock(GeoZonesUpdater.class),
                storage,
                /*locationTracker*/ null,
                mock(FineLocationPermissionChecker.class));
    }

    private static String stackToString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    // Outcome of one interleaved run: whatever the iteration threw (null if it completed), the
    // tracker (to inspect the live pushGeoZones afterwards), and the zone the background mutator
    // removes mid-iteration.
    private static final class ScenarioResult {
        final Throwable thrown;
        final GeofenceTrackerImp tracker;
        final GeoZone removedZone;

        ScenarioResult(Throwable thrown, GeofenceTrackerImp tracker, GeoZone removedZone) {
            this.thrown = thrown;
            this.tracker = tracker;
            this.removedZone = removedZone;
        }
    }

    // Builds a fresh tracker whose main iteration (locationUpdated) is interleaved with the real
    // background mutator (updateZones -> updatePushGeoZones), and runs it.
    private static ScenarioResult runInterleavedScenario() {
        Location loc = new Location("");

        // near zone: it is in both geoZones (via storage) and pushGeoZones, and within range, so
        // updatePushGeoZones structurally removes it.
        StubZone near = new StubZone("Z_near", 100L, 0);
        // far zone: an extra element so that after the removal cursor != size and Itr.next() is
        // still called (dodges the ArrayList "remove second-to-last" fail-fast blind spot).
        StubZone far = new StubZone("Z_far", 100L, Integer.MAX_VALUE);

        GeoZoneStorage storage = storageWith(near); // constructor seeds geoZones = { near }
        GeofenceTrackerImp tracker = newTracker(storage);

        TriggeringZone trigger = new TriggeringZone(
                "Z_trigger", () -> tracker.updateZones(Collections.<GeoZone>singletonList(near), loc));

        // Populate the production-created list rather than reassigning it, so the test exercises
        // whatever collection type the field uses in production (revert the field to ArrayList and
        // this regression guard goes red again).
        tracker.pushGeoZones.addAll(Arrays.asList(trigger, near, far));

        try {
            tracker.locationUpdated(loc);
            return new ScenarioResult(null, tracker, near);
        } catch (Throwable t) {
            return new ScenarioResult(t, tracker, near);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // FIX: the background structural remove (updatePushGeoZones) fires while the main iteration
    // (locationUpdated) is in progress, but pushGeoZones is now a snapshot-on-iterate
    // CopyOnWriteArrayList, so the iteration completes gracefully instead of throwing CME.
    // ---------------------------------------------------------------------------------------------
    @Test
    public void interleavedRemove_completesWithoutCme() {
        ScenarioResult result = runInterleavedScenario();

        assertNull(
                "interleaved remove must not surface as a crash on the iteration thread, got: "
                        + (result.thrown == null ? "null" : stackToString(result.thrown)),
                result.thrown);

        // Non-vacuity: the background mutator actually fired mid-iteration and structurally removed
        // its zone from the live list — the exact modCount bump that used to trip fail-fast. The
        // iteration still completed, proving the snapshot absorbed the concurrent modification.
        assertFalse(
                "the concurrent remove must have taken effect on the live list mid-iteration",
                result.tracker.pushGeoZones.contains(result.removedZone));
    }

    // ---------------------------------------------------------------------------------------------
    // DETERMINISM: 3 consecutive fresh runs, graceful every time (mirrors the 3/3 repro loop).
    // ---------------------------------------------------------------------------------------------
    @Test
    public void interleavedRemove_survivesThreeConsecutiveRuns() {
        for (int i = 1; i <= 3; i++) {
            ScenarioResult result = runInterleavedScenario();
            assertNull(
                    "run " + i + "/3 must complete without CME, got: "
                            + (result.thrown == null ? "null" : stackToString(result.thrown)),
                    result.thrown);
            assertFalse(
                    "run " + i + "/3: the concurrent remove must have taken effect mid-iteration",
                    result.tracker.pushGeoZones.contains(result.removedZone));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // BARRIER CONTROL: the SAME mutation and the SAME iteration, but serialized (mutation runs to
    // completion BEFORE the iteration starts) — i.e. what real main-looper serialization gives.
    // A fresh iterator is created after the modification, so fail-fast never trips: no CME. This
    // isolates the interleaving as the necessary condition (remove the overlap -> no crash).
    // ---------------------------------------------------------------------------------------------
    @Test
    public void mutationSerializedBeforeIteration_noCme() {
        Location loc = new Location("");
        StubZone near = new StubZone("Z_near", 100L, 0);
        StubZone far = new StubZone("Z_far", 100L, Integer.MAX_VALUE);
        StubZone head = new StubZone("Z_head", 100L, Integer.MAX_VALUE); // plain: no trigger

        GeoZoneStorage storage = storageWith(near);
        GeofenceTrackerImp tracker = newTracker(storage);
        tracker.pushGeoZones.addAll(Arrays.asList(head, near, far));

        // mutation fully completes first (removes near, bumps modCount, returns)...
        tracker.updateZones(Collections.<GeoZone>singletonList(near), loc);
        // ...then a fresh iteration runs with no overlapping structural change.
        Throwable thrown;
        try {
            tracker.locationUpdated(loc);
            thrown = null;
        } catch (Throwable t) {
            thrown = t;
        }

        assertNull("serialized mutation + iteration must not trip fail-fast, got: " + thrown, thrown);
    }
}
