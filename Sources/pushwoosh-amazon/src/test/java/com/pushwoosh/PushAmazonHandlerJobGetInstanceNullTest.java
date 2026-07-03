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

package com.pushwoosh;

import static org.junit.Assert.fail;

import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.notification.PushwooshNotificationManager;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Regression guard for crash candidate #17 (crash-amazon-handlerjob-registration-getinstance-null).
//
// The fix wrapped the three PushAmazonHandlerJob registration callbacks in try/catch(Exception),
// restoring symmetry with the legacy PushAmazonIntentService (which wraps all four). When
// getInstance()==null the deref inside NotificationRegistrarHelper still throws — but the JobService
// now swallows+logs it instead of letting it escape to the platform.
//
// Verifies that:
//  - the reach callbacks (onUnregistered / onRegistrationError) on the real JobService no longer
//    let the NPE escape (graceful swallow, like the legacy sibling);
//  - the internal deref in NotificationRegistrarHelper STILL throws (the necessary condition is
//    unchanged — the fix is the callback wrap, not a helper guard).
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PushAmazonHandlerJobGetInstanceNullTest {

    private static final String CRASH_CLASS = "com.pushwoosh.internal.utils.NotificationRegistrarHelper";

    private MockedStatic<PushwooshPlatform> pushwooshPlatformStatic;

    @After
    public void tearDown() {
        if (pushwooshPlatformStatic != null) {
            pushwooshPlatformStatic.close();
            pushwooshPlatformStatic = null;
        }
    }

    private void forceGetInstanceNull() {
        pushwooshPlatformStatic = Mockito.mockStatic(PushwooshPlatform.class);
        pushwooshPlatformStatic.when(PushwooshPlatform::getInstance).thenReturn(null);
    }

    private static boolean hasFrame(Throwable t, String className, String methodName) {
        for (StackTraceElement frame : t.getStackTrace()) {
            if (className.equals(frame.getClassName()) && methodName.equals(frame.getMethodName())) {
                return true;
            }
        }
        return false;
    }

    private static String stackToString(Throwable t) {
        return t + " :: " + java.util.Arrays.toString(t.getStackTrace());
    }

    /** Build the live JobService bypassing the throwing amazon-stub super-ctor. */
    private static PushAmazonHandlerJob newJobBypassingStubCtor() {
        return Mockito.mock(PushAmazonHandlerJob.class, Mockito.CALLS_REAL_METHODS);
    }

    // ---- graceful-swallow guard through the REAL reach: PushAmazonHandlerJob.onUnregistered ----
    // Verifies that the wrapped callback no longer lets the getInstance()==null NPE escape.

    @Test
    public void reach_onUnregistered_getInstanceNull_swallowed() {
        forceGetInstanceNull();
        PushAmazonHandlerJob job = newJobBypassingStubCtor();
        // The deref still NPEs inside NotificationRegistrarHelper, but the wrapped callback swallows it.
        job.onUnregistered(null, "regId");
    }

    // ---- graceful-swallow guard through the REAL reach: PushAmazonHandlerJob.onRegistrationError ----
    // Verifies that the wrapped callback no longer lets the getInstance()==null NPE escape.

    @Test
    public void reach_onRegistrationError_getInstanceNull_swallowed() {
        forceGetInstanceNull();
        PushAmazonHandlerJob job = newJobBypassingStubCtor();
        // The deref still NPEs inside NotificationRegistrarHelper, but the wrapped callback swallows it.
        job.onRegistrationError(null, "errorId");
    }

    // ---- necessary-condition proof: the internal deref STILL throws (fix is the wrap, not a helper guard) ----

    @Test
    public void crashPoint_onUnregisteredFromRemoteNotifications_stillThrows() {
        forceGetInstanceNull();
        try {
            NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications("regId");
            fail("expected NullPointerException from getInstance().notificationManager() deref");
        } catch (NullPointerException e) {
            org.junit.Assert.assertTrue(
                    "NPE must originate at " + CRASH_CLASS + ".onUnregisteredFromRemoteNotifications; "
                            + stackToString(e),
                    hasFrame(e, CRASH_CLASS, "onUnregisteredFromRemoteNotifications"));
        }
    }

    @Test
    public void crashPoint_onFailedToRegisterForRemoteNotifications_stillThrows() {
        forceGetInstanceNull();
        try {
            NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications("errorId");
            fail("expected NullPointerException from getInstance().notificationManager() deref");
        } catch (NullPointerException e) {
            org.junit.Assert.assertTrue(
                    "NPE must originate at " + CRASH_CLASS + ".onFailedToRegisterForRemoteNotifications; "
                            + stackToString(e),
                    hasFrame(e, CRASH_CLASS, "onFailedToRegisterForRemoteNotifications"));
        }
    }

    // ---- negative control A: instance != null -> no NPE (the null is the necessary condition) ----

    @Test
    public void negativeControl_realInstance_noThrow() {
        PushwooshPlatform platform = Mockito.mock(PushwooshPlatform.class);
        PushwooshNotificationManager manager = Mockito.mock(PushwooshNotificationManager.class);
        Mockito.when(platform.notificationManager()).thenReturn(manager);

        pushwooshPlatformStatic = Mockito.mockStatic(PushwooshPlatform.class);
        pushwooshPlatformStatic.when(PushwooshPlatform::getInstance).thenReturn(platform);

        // With a non-null instance the deref completes and delegates to the manager; no throw.
        NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications("regId");
        NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications("errorId");
        Mockito.verify(manager).onUnregisteredFromRemoteNotifications("regId");
        Mockito.verify(manager).onFailedToRegisterForRemoteNotifications("errorId");
    }

    // ---- negative control B: the legacy IntentService sibling SWALLOWS the same NPE ----
    // Proves the defect is the MISSING try/catch on the JobService, not the null itself: the exact
    // same getInstance()==null state, fed through the wrapped legacy callback, does NOT escape.

    @Test
    public void negativeControl_legacyIntentService_swallowsNpe() {
        forceGetInstanceNull();
        PushAmazonIntentService legacy = Mockito.mock(PushAmazonIntentService.class, Mockito.CALLS_REAL_METHODS);
        // No throw: PushAmazonIntentService.onUnregistered wraps the body in try/catch(Exception).
        legacy.onUnregistered("regId");
        legacy.onRegistrationError("errorId");
    }
}
