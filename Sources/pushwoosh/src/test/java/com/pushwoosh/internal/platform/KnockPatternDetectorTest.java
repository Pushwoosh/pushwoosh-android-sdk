package com.pushwoosh.internal.platform;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

import java.util.concurrent.atomic.AtomicLong;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class KnockPatternDetectorTest {

    @Mock
    private RegistrationPrefs registrationPrefs;

    @Mock
    private NotificationPrefs notificationPrefs;

    @Mock
    private PreferenceStringValue hwidPref;

    @Mock
    private PreferenceBooleanValue collectModelPref;

    @Mock
    private AppInfoProvider appInfoProvider;

    @Mock
    private RequestManager requestManager;

    private AtomicLong fakeClock;
    private KnockPatternDetector detector;
    private AutoCloseable mocks;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        mocks = MockitoAnnotations.openMocks(this);
        fakeClock = new AtomicLong(1000);
        detector = new KnockPatternDetector(fakeClock::get);

        when(notificationPrefs.isCollectingDeviceModelAllowed()).thenReturn(collectModelPref);
        when(collectModelPref.get()).thenReturn(true);

        when(appInfoProvider.getPackageName()).thenReturn("com.test.app");

        SdkStateProvider.getInstance().resetForTesting();
        SdkStateProvider.getInstance().setReady();
    }

    @After
    public void tearDown() throws Exception {
        SdkStateProvider.getInstance().resetForTesting();
        PWLog.updateLogLevel(PWLog.Level.INFO.name());
        mocks.close();
    }

    @Test
    public void testNoTriggerWithFewerThan6Knocks() {
        for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS - 1; i++) {
            fakeClock.addAndGet(1000);
            detector.onForeground();
        }
        assertEquals(KnockPatternDetector.REQUIRED_KNOCKS - 1, detector.getCount());
    }

    @Test
    public void testTriggerAfter6KnocksWithin30Seconds() {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid-123");

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {

            Context context = RuntimeEnvironment.application;
            platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
            repoMock.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefs);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);

            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            // counter resets after trigger
            assertEquals(0, detector.getCount());
            verify(requestManager).sendRequest(any(), any());
        }
    }

    @Test
    public void testNoTriggerWhen6KnocksExceed30Seconds() {
        for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
            fakeClock.addAndGet(7000); // 7s intervals, 5 gaps * 7s = 35s > 30s
            detector.onForeground();
        }
        // count stays at max but no trigger
        assertEquals(KnockPatternDetector.REQUIRED_KNOCKS, detector.getCount());
    }

    @Test
    public void testRepeatableTrigger() {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {

            Context context = RuntimeEnvironment.application;
            platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
            repoMock.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefs);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);

            // first trigger
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            assertEquals(0, detector.getCount());

            // second trigger after some time
            fakeClock.addAndGet(60_000);
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            assertEquals(0, detector.getCount());

            // request sent twice
            verify(requestManager, Mockito.times(2)).sendRequest(any(), any());
        }
    }

    @Test
    public void testHandlesNullContext() {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {

            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
            platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(null);

            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            // should trigger but not crash
            assertEquals(0, detector.getCount());
        }
    }

    @Test
    public void testHandlesEmptyHwid() {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("");

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class)) {
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);

            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            // should trigger but bail out early without crash
            assertEquals(0, detector.getCount());
        }
    }

    @Test
    public void testHandlesNullRequestManager() {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {

            Context context = RuntimeEnvironment.application;
            platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
            repoMock.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefs);
            netMock.when(NetworkModule::getRequestManager).thenReturn(null);

            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            // counter resets after trigger, no crash despite null requestManager
            assertEquals(0, detector.getCount());
        }
    }

    @Test
    public void testKnockPatternEnablesNoiseLogLevel() {
        PreferenceStringValue logLevelPref = Mockito.mock(PreferenceStringValue.class);
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        when(registrationPrefs.logLevel()).thenReturn(logLevelPref);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {

            Context context = RuntimeEnvironment.application;
            platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
            repoMock.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefs);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);

            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            verify(logLevelPref).set("NOISE");
        }
    }

    @Test
    public void testExactBoundary30Seconds() {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {

            Context context = RuntimeEnvironment.application;
            platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(context);
            platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
            repoMock.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefs);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);

            // 6 knocks exactly 30s apart: first at t=1000, last at t=31000, span = 30000ms = WINDOW_MS
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(KnockPatternDetector.WINDOW_MS / KnockPatternDetector.REQUIRED_KNOCKS);
                detector.onForeground();
            }

            assertEquals(0, detector.getCount());
        }
    }
}
