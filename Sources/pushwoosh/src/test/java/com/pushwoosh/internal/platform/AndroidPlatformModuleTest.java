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

package com.pushwoosh.internal.platform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@Config(manifest = Config.NONE)
public class AndroidPlatformModuleTest {

    private static final String[] STATE_FIELDS = {
        "managerProvider",
        "appInfoProvider",
        "resourceProvider",
        "prefsProvider",
        "receiverProvider",
        "applicationOpenDetector",
        "context",
        "prefsMigration",
        "timeProvider",
        "permissionController",
        "applicationState"
    };

    public static void changePrefsProvider(PrefsProvider prefsProvider) {
        try {
            final AndroidPlatformModule instance = AndroidPlatformModule.getInstance();
            final Field prefsProviderField = AndroidPlatformModule.class.getDeclaredField("prefsProvider");
            prefsProviderField.setAccessible(true);
            prefsProviderField.set(instance, prefsProvider);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Application application;

    @Before
    public void setUp() {
        resetState();
        application = RuntimeEnvironment.getApplication();
    }

    @After
    public void tearDown() {
        resetState();
    }

    // Verifies that init with a valid Context populates all platform providers and isInit becomes true.
    @Test
    public void init_validContext_populatesProvidersAndMarksInitialized() {
        AndroidPlatformModule.init(application);

        assertTrue(AndroidPlatformModule.isInit());
        assertSame(application, AndroidPlatformModule.getApplicationContext());
        assertNotNull(AndroidPlatformModule.getManagerProvider());
        assertNotNull(AndroidPlatformModule.getAppInfoProvider());
        assertNotNull(AndroidPlatformModule.getResourceProvider());
        assertNotNull(AndroidPlatformModule.getPrefsProvider());
        assertNotNull(AndroidPlatformModule.getReceiverProvider());
        assertNotNull(AndroidPlatformModule.getApplicationOpenDetector());
        assertNotNull(AndroidPlatformModule.getTimeProvide());
    }

    // Verifies that init(null) on a fresh module is a no-op and providers stay unset.
    @Test
    public void init_nullContextOnFreshState_doesNothing() {
        AndroidPlatformModule.init(null);

        assertFalse(AndroidPlatformModule.isInit());
        assertNull(AndroidPlatformModule.getApplicationContext());
        assertNull(AndroidPlatformModule.getManagerProvider());
        assertNull(AndroidPlatformModule.getAppInfoProvider());
        assertNull(AndroidPlatformModule.getResourceProvider());
        assertNull(AndroidPlatformModule.getPrefsProvider());
        assertNull(AndroidPlatformModule.getReceiverProvider());
        assertNull(AndroidPlatformModule.getApplicationOpenDetector());
        assertNull(AndroidPlatformModule.getTimeProvide());
    }

    // Verifies that init(null) after a successful init does not wipe previously installed providers.
    @Test
    public void init_nullContextAfterValidInit_keepsExistingState() {
        AndroidPlatformModule.init(application);
        Context originalContext = AndroidPlatformModule.getApplicationContext();
        ManagerProvider originalManagerProvider = AndroidPlatformModule.getManagerProvider();

        AndroidPlatformModule.init(null);

        assertTrue(AndroidPlatformModule.isInit());
        assertSame(originalContext, AndroidPlatformModule.getApplicationContext());
        assertSame(originalManagerProvider, AndroidPlatformModule.getManagerProvider());
    }

    // Verifies that a second init without force does not recreate providers (idempotent).
    @Test
    public void init_secondCallWithoutForce_isIdempotent() {
        AndroidPlatformModule.init(application);
        ManagerProvider firstManagerProvider = AndroidPlatformModule.getManagerProvider();

        AndroidPlatformModule.init(application);

        assertSame(firstManagerProvider, AndroidPlatformModule.getManagerProvider());
    }

    // Verifies that init with force=true rebuilds providers from scratch.
    @Test
    public void init_secondCallWithForceTrue_reinitializesProviders() {
        AndroidPlatformModule.init(application);
        ManagerProvider firstManagerProvider = AndroidPlatformModule.getManagerProvider();

        AndroidPlatformModule.init(application, true);

        assertNotSame(firstManagerProvider, AndroidPlatformModule.getManagerProvider());
    }

    // Verifies that isInit returns false when the WeakReference exists but its referent was GC-cleared.
    @Test
    public void isInit_whenWeakReferenceClearedByGc_returnsFalse() throws Exception {
        setSingletonField("context", new WeakReference<Context>(null));

        assertFalse(AndroidPlatformModule.isInit());
        assertNull(AndroidPlatformModule.getApplicationContext());
    }

    private static void resetState() {
        AndroidPlatformModule instance = AndroidPlatformModule.getInstance();
        for (String fieldName : STATE_FIELDS) {
            try {
                Field field = AndroidPlatformModule.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(instance, null);
            } catch (Exception e) {
                throw new RuntimeException("Failed to reset field " + fieldName, e);
            }
        }
    }

    private static void setSingletonField(String fieldName, Object value) throws Exception {
        Field field = AndroidPlatformModule.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(AndroidPlatformModule.getInstance(), value);
    }
}
