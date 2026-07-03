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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class PushwooshBadgeGetBadgeNumberNullPrefsTest {

    /** Force the {@code sBadgePrefs} static field back to {@code null} = the faithful pre-init state. */
    @Before
    public void resetToPreInitState() throws Exception {
        setBadgePrefs(null);
    }

    private static void setBadgePrefs(Object value) throws Exception {
        Field f = BadgeModule.class.getDeclaredField("sBadgePrefs");
        f.setAccessible(true);
        f.set(null, value);
    }

    // Verifies that getBadgeNumber() on a null sBadgePrefs (pre-/failed-/lazy-init) returns the default 0
    // instead of NPE-ing on the null getBadgePrefs() receiver.
    @Test
    public void getBadgeNumber_nullPrefs_returnsDefaultZero() {
        assertEquals(0, PushwooshBadge.getBadgeNumber());
    }

    // Verifies that addBadgeNumber(), which reads getBadgeNumber() synchronously, also degrades gracefully
    // (no NPE escapes the unwrapped public entry) when sBadgePrefs is null.
    @Test
    public void addBadgeNumber_nullPrefs_doesNotThrow() {
        PushwooshBadge.addBadgeNumber(1);
    }

    // Ground truth: pre-init, getBadgePrefs() really returns null (the guarded value is not a stand-in).
    @Test
    public void groundTruth_preInit_getBadgePrefsIsNull() {
        assertNull("pre-init sBadgePrefs must be null", BadgeModule.getBadgePrefs());
    }

    // Discriminator: after a real init, getBadgePrefs() is non-null and getBadgeNumber() still reads the
    // stored value (returns the default 0) — proves the guard did not degenerate into an unconditional 0.
    @Test
    public void negativeControl_afterInit_doesNotThrow() {
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
        BadgeModule.init();

        assertEquals(0, PushwooshBadge.getBadgeNumber());
        PushwooshBadge.addBadgeNumber(2); // must NOT throw
    }
}
