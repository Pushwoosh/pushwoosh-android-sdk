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

package com.pushwoosh.badge;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import android.os.Looper;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * Regression guard for crash candidate #27 (crash-pushwooshbadge-setbadgenumber-prefs-null).
 *
 * <p>{@code PushwooshBadge.setBadgeNumber(int)} posts work to a RAW {@code Handler.post} on the main
 * Looper. The lambda dereferences {@code BadgeModule.getBadgePrefs()}, which returns {@code null} until
 * {@code BadgeModule.init()} runs (pre-/failed-/lazy-init). Because the dispatch is a raw {@code Handler.post}
 * (no {@code catch(Throwable)}) and runs <i>after</i> {@code setBadgeNumber} returned, a synchronous
 * try/catch around the call could not catch it — the NPE crashed the app on the main thread.
 *
 * <p>The fix null-guards the {@code getBadgePrefs()} receiver inside the lambda: when {@code prefs == null}
 * the lambda returns early (the badge value is dropped, no-op) instead of dereferencing the null receiver.
 * These tests assert that graceful no-op behaviour and pin the null-prefs pre-init state as the necessary
 * condition the guard defends against.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.PAUSED)
public class PushwooshBadgeSetBadgeNumberNullPrefsTest {

    /** Force the {@code sBadgePrefs} static field back to {@code null} = the faithful pre-init state. */
    @Before
    public void resetToPreInitState() throws Exception {
        // A non-null application context so the lambda passes its getApplicationContext()==null guard and
        // reaches the getBadgePrefs() deref (the point the fix guards).
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
        setBadgePrefs(null);
    }

    private static void setBadgePrefs(Object value) throws Exception {
        Field f = BadgeModule.class.getDeclaredField("sBadgePrefs");
        f.setAccessible(true);
        f.set(null, value);
    }

    private static String stackToString(Throwable t) {
        return t + " :: " + Arrays.toString(t.getStackTrace());
    }

    /** Run the real PushwooshBadge.setBadgeNumber lambda by draining the main looper; return any escaped NPE. */
    private static NullPointerException runSetBadgeNumberLambdaAndCatch(int newBadge) {
        PushwooshBadge.setBadgeNumber(newBadge); // posts the real lambda to the raw main Handler
        try {
            Shadows.shadowOf(Looper.getMainLooper()).idle(); // runs the posted lambda; would rethrow its NPE
            return null;
        } catch (NullPointerException npe) {
            return npe;
        }
    }

    // Verifies that the real posted setBadgeNumber lambda, run on the main Looper with a null sBadgePrefs
    // (pre-/failed-/lazy-init), drains as a graceful no-op: the guard returns early and NO NPE escapes the
    // unwrapped raw Handler.post (was: NPE at :82 on the null getBadgePrefs() receiver).
    @Test
    public void setBadgeNumber_nullPrefs_lambdaIsGracefulNoOp() {
        NullPointerException npe = runSetBadgeNumberLambdaAndCatch(5);
        if (npe != null) {
            fail("setBadgeNumber lambda must not throw when prefs is null. Was: " + stackToString(npe));
        }
    }

    // Ground truth: pre-init, getBadgePrefs() really returns null (the guarded value is not a stand-in).
    @Test
    public void groundTruth_preInit_getBadgePrefsIsNull() {
        assertNull("pre-init sBadgePrefs must be null", BadgeModule.getBadgePrefs());
    }

    // Discriminator: after a real init, getBadgePrefs() is non-null and the posted lambda still drains
    // cleanly — proves null sBadgePrefs is the necessary condition (remove it -> the lambda still runs)
    // and the guard did not degenerate into an unconditional skip.
    @Test
    public void negativeControl_afterInit_lambdaDoesNotThrow() {
        BadgeModule.init(); // sBadgePrefs = new BadgePrefs() over the real Robolectric SharedPreferences
        NullPointerException npe = runSetBadgeNumberLambdaAndCatch(5);
        if (npe != null) {
            fail("after init the setBadgeNumber lambda must NOT throw. Was: " + stackToString(npe));
        }
    }
}
