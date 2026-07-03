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

import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

// Regression guard for crash candidate #18 (crash-amazon-handlerjob-onregistered-prefs-null).
//
// The fix wrapped PushAmazonHandlerJob.onRegistered in try/catch(Exception), restoring symmetry with
// the legacy PushAmazonIntentService. When getRegistrationPreferences()==null the guard deref inside
// NotificationRegistrarHelper.isRegisteredForRemoteNotifications still throws — but the JobService
// callback now swallows+logs it instead of letting it escape to the platform.
//
// Verifies that:
//  - the real JobService onRegistered no longer lets the prefs-null NPE escape (graceful swallow);
//  - the internal guard deref STILL throws (the necessary condition is unchanged — the fix is the
//    callback wrap, not a helper guard).
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PushAmazonHandlerJobOnRegisteredNullPrefsTest {

    private static final String CRASH_CLASS = "com.pushwoosh.internal.utils.NotificationRegistrarHelper";
    private static final String CRASH_METHOD = "isRegisteredForRemoteNotifications";

    private MockedStatic<RepositoryModule> repositoryModuleStatic;

    @After
    public void tearDown() {
        if (repositoryModuleStatic != null) {
            repositoryModuleStatic.close();
            repositoryModuleStatic = null;
        }
    }

    private void forceRegistrationPrefsNull() {
        repositoryModuleStatic = Mockito.mockStatic(RepositoryModule.class);
        repositoryModuleStatic
                .when(RepositoryModule::getRegistrationPreferences)
                .thenReturn(null);
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

    // ---- ground-truth the load-bearing fact: forced getRegistrationPreferences() really is null ----

    // ---- graceful-swallow guard through the REAL reach: PushAmazonHandlerJob.onRegistered ----
    // Verifies that the wrapped callback no longer lets the prefs-null NPE escape.

    @Test
    public void reach_onRegistered_nullPrefs_swallowed() {
        forceRegistrationPrefsNull();
        PushAmazonHandlerJob job = newJobBypassingStubCtor();
        // The guard deref still NPEs inside NotificationRegistrarHelper, but the wrapped callback swallows it.
        job.onRegistered(null, "regId");
    }

    // ---- necessary-condition proof: the internal guard deref STILL throws (fix is the wrap, not a helper guard) ----

    @Test
    public void crashPoint_onRegisteredForRemoteNotifications_stillThrows() {
        forceRegistrationPrefsNull();
        try {
            NotificationRegistrarHelper.onRegisteredForRemoteNotifications("regId", null);
            fail("expected NullPointerException from getRegistrationPreferences().isRegisteredForPush() deref");
        } catch (NullPointerException e) {
            org.junit.Assert.assertTrue(
                    "NPE must originate at " + CRASH_CLASS + "." + CRASH_METHOD + "; " + stackToString(e),
                    hasFrame(e, CRASH_CLASS, CRASH_METHOD));
        }
    }

    // ---- negative control A: non-null prefs -> no NPE at the guard (null prefs is the necessary cond) ----
    // With a non-null RegistrationPrefs whose isRegisteredForPush().get()==false, the guard at :63
    // returns early at :67 cleanly, never reaching the #18 getInstance() deref. This isolates #19's
    // crash to the getRegistrationPreferences()==null state, distinct from #18.

    @Test
    public void negativeControl_nonNullPrefs_notRegistered_noThrow() {
        RegistrationPrefs prefs = Mockito.mock(RegistrationPrefs.class);
        PreferenceBooleanValue registeredForPush = Mockito.mock(PreferenceBooleanValue.class);
        Mockito.when(registeredForPush.get()).thenReturn(false);
        Mockito.when(prefs.isRegisteredForPush()).thenReturn(registeredForPush);

        repositoryModuleStatic = Mockito.mockStatic(RepositoryModule.class);
        repositoryModuleStatic
                .when(RepositoryModule::getRegistrationPreferences)
                .thenReturn(prefs);

        // Guard passes (device not registered) -> early return at :67; no NPE, no getInstance() touch.
        NotificationRegistrarHelper.onRegisteredForRemoteNotifications("regId", null);
        Mockito.verify(registeredForPush).get();
    }

    // ---- negative control B: the legacy IntentService sibling SWALLOWS the same NPE ----
    // Proves the defect is the MISSING try/catch on the JobService, not the null itself: the exact
    // same getRegistrationPreferences()==null state, fed through the wrapped legacy onRegistered, does
    // NOT escape.

    @Test
    public void negativeControl_legacyIntentService_swallowsNpe() {
        forceRegistrationPrefsNull();
        PushAmazonIntentService legacy = Mockito.mock(PushAmazonIntentService.class, Mockito.CALLS_REAL_METHODS);
        // No throw: PushAmazonIntentService.onRegistered wraps the body in try/catch(Exception).
        legacy.onRegistered("regId");
    }
}
