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

package com.pushwoosh.internal.platform.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.PowerManager;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.platform.resource.ResourceProvider;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class DeviceUtilsTest {

    @Mock
    private RegistrationPrefs registrationPrefs;

    @Mock
    private PreferenceStringValue deviceIdPref;

    @Mock
    private ResourceProvider resourceProvider;

    @Mock
    private ManagerProvider managerProvider;

    @Mock
    private AppInfoProvider appInfoProvider;

    @Mock
    private KeyguardManager keyguardManager;

    @Mock
    private PowerManager powerManager;

    @Mock
    private ActivityManager activityManager;

    private AutoCloseable mocks;

    private String originalManufacturer;
    private String originalModel;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(registrationPrefs.deviceId()).thenReturn(deviceIdPref);
        originalManufacturer = Build.MANUFACTURER;
        originalModel = Build.MODEL;
    }

    @After
    public void tearDown() throws Exception {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", originalManufacturer);
        ReflectionHelpers.setStaticField(Build.class, "MODEL", originalModel);
        mocks.close();
    }

    // ============================================================
    // DeviceUUID (abstract) — tested via TestDeviceUUID subclass
    // ============================================================

    // Verifies that getUUID() returns cached value on second call without invoking tryGetUUID() again.
    @Test
    public void getUUID_secondCall_returnsCachedValue() {
        TestDeviceUUID primary = new TestDeviceUUID("abc-123-valid");

        String first = primary.getUUID();
        String second = primary.getUUID();

        assertEquals("abc-123-valid", first);
        assertEquals("abc-123-valid", second);
        assertEquals(1, primary.tryGetUUIDCounter.get());
    }

    // Verifies that getUUID() falls back when primary returns an empty string.
    @Test
    public void getUUID_emptyPrimaryWithFallback_returnsFallback() {
        TestDeviceUUID primary = new TestDeviceUUID("");
        TestDeviceUUID fallback = new TestDeviceUUID("fallback-uuid");
        primary.setFallback(fallback);

        assertEquals("fallback-uuid", primary.getUUID());
    }

    // Verifies that getUUID() falls back when primary returns a zero/dash-only UUID.
    @Test
    public void getUUID_zerosAndDashesUUID_treatedAsBadAndFallsBack() {
        TestDeviceUUID primary = new TestDeviceUUID("00000000-0000-0000-0000-000000000000");
        TestDeviceUUID fallback = new TestDeviceUUID("good");
        primary.setFallback(fallback);

        assertEquals("good", primary.getUUID());
    }

    // Verifies that all known bad Android IDs trigger fallback regardless of case.
    @Test
    public void getUUID_knownBadAndroidIds_triggerFallback() {
        List<String> badIds = Arrays.asList("9774d56d682e549c", "1234567", "abcdef", "dead00beef", "unknown");

        for (String badId : badIds) {
            String upperCased = badId.toUpperCase();
            TestDeviceUUID primary = new TestDeviceUUID(upperCased);
            TestDeviceUUID fallback = new TestDeviceUUID("safe");
            primary.setFallback(fallback);

            assertEquals("bad id: " + badId, "safe", primary.getUUID());
        }
    }

    // Verifies that bad UUID without fallback is returned as is and cached.
    @Test
    public void getUUID_badUUIDWithoutFallback_returnsBadValueAndCaches() {
        TestDeviceUUID primary = new TestDeviceUUID("unknown");

        assertEquals("unknown", primary.getUUID());
        assertEquals("unknown", primary.getUUID());
        assertEquals(1, primary.tryGetUUIDCounter.get());
    }

    // Verifies that getUUID(listener) returns cached value without invoking tryGetUUID again.
    @Test
    public void getUUIDWithListener_cachedValue_returnedViaCallback() {
        TestDeviceUUID primary = new TestDeviceUUID("cached");
        primary.getUUID();

        AtomicReference<String> received = new AtomicReference<>();
        primary.getUUID(received::set);

        assertEquals("cached", received.get());
        assertEquals(1, primary.tryGetUUIDCounter.get());
    }

    // Verifies that getUUID(listener) on empty cache invokes async tryGetUUID and caches result.
    @Test
    public void getUUIDWithListener_emptyCache_invokesAsyncTryGetUUIDAndCaches() {
        TestDeviceUUID primary = new TestDeviceUUID("fresh", true);

        AtomicReference<String> received = new AtomicReference<>();
        primary.getUUID(received::set);

        assertEquals("fresh", received.get());
        assertEquals("fresh", primary.getUUID());
    }

    // Verifies that getUUID(listener) on bad UUID with fallback delivers fallback and does not cache primary.
    @Test
    public void getUUIDWithListener_badUUIDWithFallback_returnsFallbackWithoutCaching() {
        TestDeviceUUID primary = new TestDeviceUUID("", true);
        TestDeviceUUID fallback = new TestDeviceUUID("fallback-async");
        primary.setFallback(fallback);

        AtomicReference<String> received = new AtomicReference<>();
        primary.getUUID(received::set);

        assertEquals("fallback-async", received.get());
        // Primary cache must not be set — next sync call goes through tryGetUUID again and still hits fallback.
        assertEquals("fallback-async", primary.getUUID());
        assertTrue("tryGetUUID must be called again for non-cached primary", primary.tryGetUUIDCounter.get() >= 2);
    }

    // ============================================================
    // DeviceRandomUUID — tested via reflection (private static class)
    // ============================================================

    // Verifies that DeviceRandomUUID returns existing deviceId from prefs without writing.
    @Test
    public void deviceRandomUUID_existingId_returnsItWithoutWriting() throws Exception {
        when(deviceIdPref.get()).thenReturn("existing-id");

        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            repo.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);

            DeviceUtils.DeviceUUID randomUuid = newDeviceRandomUUID();
            String result = randomUuid.getUUID();

            assertEquals("existing-id", result);
            verify(deviceIdPref, never()).set(anyString());
        }
    }

    // Verifies that DeviceRandomUUID generates a UUID and stores it when prefs are empty.
    @Test
    public void deviceRandomUUID_emptyPrefs_generatesAndStoresUUID() throws Exception {
        when(deviceIdPref.get()).thenReturn("");

        try (MockedStatic<RepositoryModule> repo = mockStatic(RepositoryModule.class)) {
            repo.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);

            DeviceUtils.DeviceUUID randomUuid = newDeviceRandomUUID();
            String result = randomUuid.getUUID();

            assertNotNull(result);
            // Must be a valid UUID string — throws if not.
            UUID.fromString(result);

            ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
            verify(deviceIdPref).set(captor.capture());
            assertEquals(result, captor.getValue());
        }
    }

    // ============================================================
    // DeviceUtils — top-level methods
    // ============================================================

    // Verifies that getDeviceName() returns capitalized model when model starts with manufacturer.
    @Test
    public void getDeviceName_modelStartsWithManufacturer_returnsCapitalizedModel() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "Samsung");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "Samsung Galaxy S10");

        assertEquals("Samsung Galaxy S10", DeviceUtils.getDeviceName());
    }

    // Verifies that getDeviceName() prefixes capitalized manufacturer to model otherwise.
    @Test
    public void getDeviceName_modelDoesNotStartWithManufacturer_prefixesCapitalizedManufacturer() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "google");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "Pixel 8");

        assertEquals("Google Pixel 8", DeviceUtils.getDeviceName());
    }

    // Verifies that getDeviceName() leaves already-capitalized manufacturer untouched.
    @Test
    public void getDeviceName_alreadyCapitalizedManufacturer_keepsAsIs() {
        ReflectionHelpers.setStaticField(Build.class, "MANUFACTURER", "OnePlus");
        ReflectionHelpers.setStaticField(Build.class, "MODEL", "7T");

        assertEquals("OnePlus 7T", DeviceUtils.getDeviceName());
    }

    // Verifies that isTablet() returns true when screenLayout has SCREENLAYOUT_SIZE_XLARGE bit.
    @Test
    public void isTablet_xlargeLayout_returnsTrue() {
        Configuration config = new Configuration();
        config.screenLayout = Configuration.SCREENLAYOUT_SIZE_XLARGE | Configuration.SCREENLAYOUT_LONG_YES;
        when(resourceProvider.getConfiguration()).thenReturn(config);

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);

            assertTrue(DeviceUtils.isTablet());
        }
    }

    // Verifies that isTablet() returns false for a non-xlarge layout.
    @Test
    public void isTablet_normalLayout_returnsFalse() {
        Configuration config = new Configuration();
        config.screenLayout = Configuration.SCREENLAYOUT_SIZE_NORMAL;
        when(resourceProvider.getConfiguration()).thenReturn(config);

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);

            assertFalse(DeviceUtils.isTablet());
        }
    }

    // Verifies that isTablet() returns false when getConfiguration() returns null.
    @Test
    public void isTablet_nullConfiguration_returnsFalse() {
        when(resourceProvider.getConfiguration()).thenReturn(null);

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getResourceProvider).thenReturn(resourceProvider);

            assertFalse(DeviceUtils.isTablet());
        }
    }

    // Verifies that isAppOnForeground() returns false when KeyguardManager is null.
    @Test
    public void isAppOnForeground_nullKeyguardManager_returnsFalse() {
        when(managerProvider.getKeyguardManager()).thenReturn(null);

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);

            assertFalse(DeviceUtils.isAppOnForeground());
        }
    }

    // Verifies that isAppOnForeground() returns false when lock screen is showing.
    @Test
    public void isAppOnForeground_lockScreenShowing_returnsFalse() {
        when(managerProvider.getKeyguardManager()).thenReturn(keyguardManager);
        when(keyguardManager.inKeyguardRestrictedInputMode()).thenReturn(true);

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);

            assertFalse(DeviceUtils.isAppOnForeground());
        }
    }

    // Verifies that isAppOnForeground() returns false when the screen is not interactive.
    @Test
    public void isAppOnForeground_screenNotInteractive_returnsFalse() {
        when(managerProvider.getKeyguardManager()).thenReturn(keyguardManager);
        when(keyguardManager.inKeyguardRestrictedInputMode()).thenReturn(false);
        when(managerProvider.getPowerManager()).thenReturn(powerManager);
        when(powerManager.isInteractive()).thenReturn(false);

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);

            assertFalse(DeviceUtils.isAppOnForeground());
        }
    }

    // Verifies that isAppOnForeground() returns true when our package is running in foreground.
    @Test
    public void isAppOnForeground_ourProcessInForeground_returnsTrue() {
        prepareKeyguardPowerOk();
        ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
        info.processName = "com.pushwoosh.test";
        info.importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        when(managerProvider.getActivityManager()).thenReturn(activityManager);
        when(activityManager.getRunningAppProcesses()).thenReturn(Collections.singletonList(info));
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            platform.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);

            assertTrue(DeviceUtils.isAppOnForeground());
        }
    }

    // Verifies that isAppOnForeground() returns false when our process is not foreground-importance.
    @Test
    public void isAppOnForeground_ourProcessBackgroundImportance_returnsFalse() {
        prepareKeyguardPowerOk();
        ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
        info.processName = "com.pushwoosh.test";
        info.importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
        when(managerProvider.getActivityManager()).thenReturn(activityManager);
        when(activityManager.getRunningAppProcesses()).thenReturn(Collections.singletonList(info));
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            platform.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);

            assertFalse(DeviceUtils.isAppOnForeground());
        }
    }

    // Verifies that isAppOnForeground() returns false when only foreign packages are in foreground.
    @Test
    public void isAppOnForeground_otherPackageInForeground_returnsFalse() {
        prepareKeyguardPowerOk();
        ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
        info.processName = "com.other.app";
        info.importance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
        when(managerProvider.getActivityManager()).thenReturn(activityManager);
        when(activityManager.getRunningAppProcesses()).thenReturn(Collections.singletonList(info));
        when(appInfoProvider.getPackageName()).thenReturn("com.pushwoosh.test");

        try (MockedStatic<AndroidPlatformModule> platform = mockStatic(AndroidPlatformModule.class)) {
            platform.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProvider);
            platform.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);

            assertFalse(DeviceUtils.isAppOnForeground());
        }
    }

    // ============================================================
    // Helpers
    // ============================================================

    private void prepareKeyguardPowerOk() {
        when(managerProvider.getKeyguardManager()).thenReturn(keyguardManager);
        when(keyguardManager.inKeyguardRestrictedInputMode()).thenReturn(false);
        when(managerProvider.getPowerManager()).thenReturn(powerManager);
        when(powerManager.isInteractive()).thenReturn(true);
    }

    private DeviceUtils.DeviceUUID newDeviceRandomUUID() throws Exception {
        Class<?> cls = Class.forName("com.pushwoosh.internal.platform.utils.DeviceUtils$DeviceRandomUUID");
        Constructor<?> ctor = cls.getDeclaredConstructor();
        ctor.setAccessible(true);
        return (DeviceUtils.DeviceUUID) ctor.newInstance();
    }

    /**
     * Test-only DeviceUUID. When {@code async} is false (default), {@link #tryGetUUID()} is used.
     * When {@code async} is true, {@link #tryGetUUID(TryGetUuidCallback)} is overridden and delivers
     * the configured value through the callback (simulating async DeviceSharedUUID behaviour).
     */
    private static class TestDeviceUUID extends DeviceUtils.DeviceUUID {
        final AtomicInteger tryGetUUIDCounter = new AtomicInteger();
        private final String value;
        private final boolean async;

        TestDeviceUUID(String value) {
            this(value, false);
        }

        TestDeviceUUID(String value, boolean async) {
            this.value = value;
            this.async = async;
        }

        @Override
        protected String tryGetUUID() {
            tryGetUUIDCounter.incrementAndGet();
            return value;
        }

        @Override
        protected void tryGetUUID(TryGetUuidCallback callback) {
            if (async) {
                tryGetUUIDCounter.incrementAndGet();
                callback.onGetUuid(value);
            } else {
                super.tryGetUUID(callback);
            }
        }
    }
}
