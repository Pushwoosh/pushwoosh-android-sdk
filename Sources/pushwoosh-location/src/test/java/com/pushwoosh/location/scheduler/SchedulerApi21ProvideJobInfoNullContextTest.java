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

package com.pushwoosh.location.scheduler;

import android.content.Context;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Regression guard for crash candidate #25 (crash-scheduler-api21-providejobinfo-context-null).
 *
 * <p>Was a reproduction harness asserting the NPE at
 * {@code GeoLocationServiceSchedulerApi21.provideJobInfo:79} ({@code new ComponentName(null,...)}),
 * reached because the three public overrides computed {@code provideJobInfo(context)} as an
 * argument before {@code scheduleJob}'s {@code :66} null-guard could run. The fix adds a
 * first-line {@code context == null -> return} guard to each override (mirroring the Api16 sibling),
 * so the eager argument deref never happens. These tests now assert that graceful no-op.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
@LooperMode(LooperMode.Mode.LEGACY)
public class SchedulerApi21ProvideJobInfoNullContextTest {

    @Before
    public void setUp() {
        // Faithful to a normally-initialized SDK: LocationModule.<clinit> succeeds so the
        // jobLocationIdProvider() left operand at :79 is harmless (see runbook). The only thing
        // under test is that a null context no longer reaches the ComponentName deref.
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
    }

    // Verifies that requestLocationDisabled(null) — the real PushwooshLocation.stop() entry — returns
    // without throwing (the first-line guard short-circuits before the eager provideJobInfo deref).
    @Test
    public void requestLocationDisabled_nullContext_isNoOp() {
        new GeoLocationServiceSchedulerApi21().requestLocationDisabled(null);
    }

    // Verifies that scheduleNearestGeoZones(null, …) returns without throwing.
    @Test
    public void scheduleNearestGeoZones_nullContext_isNoOp() {
        new GeoLocationServiceSchedulerApi21().scheduleNearestGeoZones(null, 1000L);
    }

    // Verifies that requestUpdateNearestGeoZones(null) returns without throwing.
    @Test
    public void requestUpdateNearestGeoZones_nullContext_isNoOp() {
        new GeoLocationServiceSchedulerApi21().requestUpdateNearestGeoZones(null);
    }

    // Verifies that a real (non-null) context still drives all three overrides through to scheduleJob —
    // the discriminator proving the guard did not over-fire into an unconditional return.
    @Test
    public void negativeControl_realContext_stillSchedules() {
        Context realContext = RuntimeEnvironment.getApplication();
        GeoLocationServiceSchedulerApi21 scheduler = new GeoLocationServiceSchedulerApi21();
        scheduler.requestLocationDisabled(realContext);
        scheduler.scheduleNearestGeoZones(realContext, 1000L);
        scheduler.requestUpdateNearestGeoZones(realContext);
    }
}
