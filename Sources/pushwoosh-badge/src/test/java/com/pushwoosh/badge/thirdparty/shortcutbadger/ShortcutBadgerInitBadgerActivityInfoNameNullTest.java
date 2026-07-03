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

package com.pushwoosh.badge.thirdparty.shortcutbadger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;

/**
 * Regression guard for crash candidate #31
 * (crash-shortcutbadger-initbadger-activityinfo-name-null).
 *
 * <p><b>Fixed defect.</b> {@code ShortcutBadger.initBadger:231} null-checked {@code resolveInfo}
 * (short-circuit {@code ||}) but dereferenced {@code resolveInfo.activityInfo} and
 * {@code resolveInfo.activityInfo.name} WITHOUT a null-check. When {@code resolveActivity(HOME)}
 * returned a non-null {@code ResolveInfo} whose {@code activityInfo}/{@code activityInfo.name} was
 * null (a non-standard launcher / ROM), {@code name.toLowerCase()} threw an NPE in {@code initBadger}.
 * That NPE escaped unwrapped: {@code initBadger} is called at {@code applyCountOrThrow:120}, BEFORE
 * the {@code try} at {@code :126}, so it never became a {@code ShortcutBadgeException} and slipped
 * past {@code applyCount:104}'s {@code catch(ShortcutBadgeException)} — crashing the host on the
 * first badge call in the process (and on the main thread via the raw {@code Handler.post} of
 * {@code PushwooshBadge.setBadgeNumber}).
 *
 * <p><b>The fix.</b> The {@code :231} guard now also returns false when {@code activityInfo == null}
 * or {@code activityInfo.name == null}, treating an unresolvable/nameless HOME activity as "no default
 * launcher" — the same outcome the line already produced for a "resolver" name or a null
 * {@code resolveInfo}. {@code initBadger} returns false, {@code applyCountOrThrow:122-123} throws a
 * {@code ShortcutBadgeException}, and {@code applyCount:104} swallows it → {@code applyCount} returns
 * {@code false} (graceful no-op) instead of escaping an NPE.
 *
 * <p>These tests assert that graceful behaviour through the public {@code ShortcutBadger.applyCount}
 * entry (the path the crash escaped on the first badge call): {@code applyCount} returns {@code false}
 * and no {@code NullPointerException} escapes for a null {@code activityInfo.name} or a null
 * {@code activityInfo}. The negative controls (a real HOME name; a null launch intent) are the
 * discriminators showing the guard did not become an unconditional return: a real name must still run
 * the (formerly safe) {@code :231} deref without an NPE, and the null-launch-intent path proves the
 * {@code applyCount} wrapper genuinely catches {@code ShortcutBadgeException} — so the original NPE
 * escape was a real defect (NPE ≠ {@code ShortcutBadgeException}), not a missing wrapper everywhere.
 *
 * <p>Bad value (faithful, not a surrogate): {@code activityInfo.name == null} on a non-null HOME
 * {@code ResolveInfo} is exactly what {@code PackageManager.resolveActivity} returns on a non-standard
 * launcher. It is delivered by a Mockito-mocked {@link PackageManager} behind a {@link ContextWrapper}
 * (Robolectric's {@code ShadowPackageManager} cannot be steered for this case — it keys its override
 * map on {@code Intent} identity and synthesizes a non-null {@code activityInfo.name}). The real
 * {@code ShortcutBadger} bytecode runs end to end; only the OS-returned bad {@code ResolveInfo} is mocked.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class ShortcutBadgerInitBadgerActivityInfoNameNullTest {

    private static final String PKG = "com.pushwoosh.badge.test";

    /**
     * Reset {@code private static Badger sShortcutBadger} (+ siblings) to {@code null} before every test:
     * {@code initBadger} (and therefore the guarded path) runs only on the FIRST badge call in the process
     * ({@code applyCountOrThrow:119 if (sShortcutBadger == null)}). The shared Robolectric sandbox would
     * otherwise carry a non-null badger set by the negative control into later tests and skip the path.
     */
    @Before
    public void resetSShortcutBadger() throws Exception {
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
        setStatic("sShortcutBadger", null);
        setStatic("sComponentName", null);
        setStatic("sIsBadgeCounterSupported", null);
    }

    private static void setStatic(String fieldName, Object value) throws Exception {
        Field f = ShortcutBadger.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    // ---------------------------------------------------------------------------------------------
    // Context factory: a ContextWrapper whose getPackageManager() returns a mock PM that drives
    // initBadger exactly to its (formerly crashing) bad-value branch.
    //   - getLaunchIntentForPackage(pkg) -> non-null (else initBadger:220 returns false early)
    //   - resolveActivity(HOME, ...)     -> the supplied (bad) ResolveInfo
    // ---------------------------------------------------------------------------------------------

    /** Build a context whose PM returns a HOME ResolveInfo with the given (possibly null) activityInfo.name. */
    private static Context contextWithHomeName(String homeName) {
        ResolveInfo home = new ResolveInfo();
        home.activityInfo = new ActivityInfo();
        home.activityInfo.packageName = "com.example.launcher";
        home.activityInfo.name = homeName; // null = the bad value the :231 guard now tolerates
        return contextWithHomeResolveInfo(home, /*launchIntentNonNull=*/ true);
    }

    /** Build a context whose PM returns a HOME ResolveInfo whose activityInfo itself is null. */
    private static Context contextWithNullActivityInfo() {
        ResolveInfo home = new ResolveInfo();
        home.activityInfo = null; // the :231 activityInfo deref the guard now tolerates
        return contextWithHomeResolveInfo(home, /*launchIntentNonNull=*/ true);
    }

    private static Context contextWithHomeResolveInfo(ResolveInfo home, boolean launchIntentNonNull) {
        PackageManager pm = mock(PackageManager.class);

        if (launchIntentNonNull) {
            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.setComponent(new android.content.ComponentName(PKG, PKG + ".LauncherActivity"));
            when(pm.getLaunchIntentForPackage(anyString())).thenReturn(launch);
        } else {
            when(pm.getLaunchIntentForPackage(anyString())).thenReturn(null);
        }

        when(pm.resolveActivity(any(Intent.class), anyInt())).thenReturn(home);

        Context base = RuntimeEnvironment.getApplication();
        return new ContextWrapper(base) {
            @Override
            public PackageManager getPackageManager() {
                return pm;
            }

            @Override
            public String getPackageName() {
                return PKG;
            }

            @Override
            public Context getApplicationContext() {
                return this;
            }
        };
    }

    // ---------------------------------------------------------------------------------------------
    // Direct entry: real ShortcutBadger.applyCount(context, n) with a null-name HOME activity. The fixed
    // :231 guard treats the nameless HOME activity as "no default launcher": initBadger returns false,
    // applyCountOrThrow throws ShortcutBadgeException, applyCount swallows it -> returns false. No NPE
    // escapes. (Pre-fix this threw an NPE out of initBadger:231 unwrapped.)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void applyCount_nullActivityInfoName_isGracefulNoOp() {
        Context ctx = contextWithHomeName(/*homeName=*/ null);
        // Must not throw NPE; returns false (graceful "no default launcher" no-op).
        assertFalse(
                "null activityInfo.name must degrade to applyCount==false, not an NPE",
                ShortcutBadger.applyCount(ctx, 1));
    }

    // Variant: resolveInfo.activityInfo itself is null. The same :231 line derefs activityInfo (before
    // .name); the fixed guard tolerates it too -> graceful false, no NPE.
    @Test
    public void applyCount_nullActivityInfo_isGracefulNoOp() {
        Context ctx = contextWithNullActivityInfo();
        assertFalse(
                "null activityInfo must degrade to applyCount==false, not an NPE", ShortcutBadger.applyCount(ctx, 1));
    }

    // ---------------------------------------------------------------------------------------------
    // Negative control 1 — a HOME activity with a real non-null name reaches initBadger:231 with a name
    // that does not contain "resolver", so the guard does NOT short-circuit to false. This discriminates
    // the fix from an unconditional `return false`: with a real name initBadger proceeds past :231. The
    // assertion is that NO NullPointerException escapes — a partial revert that made the guard always
    // return false would still pass the COVER tests above (they expect false) but would not be caught
    // there; here the real-name path must run the (formerly safe) :231 deref without an NPE.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void negativeControl_realActivityInfoName_noNpeEscapes() {
        Context ctx = contextWithHomeName(/*homeName=*/ "com.example.launcher.HomeActivity");
        try {
            ShortcutBadger.applyCount(ctx, 1); // real name -> :231 deref is fine, must not throw NPE
        } catch (NullPointerException npe) {
            fail("with a real HOME name no NPE must escape initBadger:231: " + npe);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Negative control 2 — no launch intent: initBadger:220 returns false, applyCountOrThrow throws
    // ShortcutBadgeException, which applyCount:104 SWALLOWS -> returns false, no NPE escapes. Confirms the
    // wrapper genuinely catches ShortcutBadgeException and the HOME path must be reached (non-null launch
    // intent) for the guarded branch to matter.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void negativeControl_noLaunchIntent_swallowedNoNpe() {
        ResolveInfo home = new ResolveInfo();
        home.activityInfo = new ActivityInfo();
        home.activityInfo.name = null;
        Context ctx = contextWithHomeResolveInfo(home, /*launchIntentNonNull=*/ false);
        assertFalse(
                "no launcher -> applyCount returns false (ShortcutBadgeException swallowed)",
                ShortcutBadger.applyCount(ctx, 1));
    }
}
