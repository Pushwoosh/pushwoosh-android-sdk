package com.pushwoosh.internal.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.CreateTestDeviceRequest;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceLongValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Field;
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
    private PreferenceLongValue knockTimePref;

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

        when(registrationPrefs.lastKnockPatternTime()).thenReturn(knockTimePref);
        when(knockTimePref.get()).thenReturn(0L);

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
    public void testTriggerAfter6KnocksWithin30Seconds() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid-123");

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            assertEquals(0, detector.getCount());
            verify(requestManager).sendRequest(any(), any());
        });
    }

    @Test
    public void testNoTriggerWhen6KnocksExceed30Seconds() {
        for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
            fakeClock.addAndGet(7000);
            detector.onForeground();
        }
        assertEquals(KnockPatternDetector.REQUIRED_KNOCKS, detector.getCount());
    }

    @Test
    public void testRepeatableTrigger() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        wireKnockTimePrefToInMemoryStore();

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            assertEquals(0, detector.getCount());

            fakeClock.addAndGet(KnockPatternDetector.COOLDOWN_MS + 1000);
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            assertEquals(0, detector.getCount());

            verify(requestManager, Mockito.times(2)).sendRequest(any(), any());
        });
    }

    @Test
    public void testCooldownBlocksSecondTriggerWithinOneHour() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        wireKnockTimePrefToInMemoryStore();

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            verify(requestManager, Mockito.times(1)).sendRequest(any(), any());

            fakeClock.addAndGet(30L * 60L * 1000L);
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            verify(requestManager, Mockito.times(1)).sendRequest(any(), any());
        });
    }

    @Test
    public void testCooldownBoundaryAtExactlyOneHourAllowsTrigger() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        wireKnockTimePrefToInMemoryStore();

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            long lastTriggerTime = fakeClock.get();
            verify(requestManager, Mockito.times(1)).sendRequest(any(), any());

            fakeClock.set(
                    lastTriggerTime + KnockPatternDetector.COOLDOWN_MS - KnockPatternDetector.REQUIRED_KNOCKS * 1000L);
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            verify(requestManager, Mockito.times(2)).sendRequest(any(), any());
        });
    }

    @Test
    public void testCooldownUsesSubtractionWithLargeTimestamps() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        wireKnockTimePrefToInMemoryStore();

        fakeClock.set(2L * KnockPatternDetector.COOLDOWN_MS);

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            verify(requestManager, Mockito.times(1)).sendRequest(any(), any());

            fakeClock.addAndGet(60_000);
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }
            verify(requestManager, Mockito.times(1)).sendRequest(any(), any());
        });
    }

    @Test
    public void testAutoCreatedFlagIsTrueForKnockTrigger() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            CreateTestDeviceRequest captured = captureRequest();
            Boolean autoCreated = readPrivateField(captured, "mAutoCreated");
            assertTrue(autoCreated);
        });
    }

    private void wireKnockTimePrefToInMemoryStore() {
        AtomicLong stored = new AtomicLong(0L);
        when(knockTimePref.get()).thenAnswer(inv -> stored.get());
        Mockito.doAnswer(inv -> {
                    stored.set(inv.getArgument(0));
                    return null;
                })
                .when(knockTimePref)
                .set(Mockito.anyLong());
    }

    @Test
    public void testKnockPatternEnablesNoiseLogLevel() throws Exception {
        PreferenceStringValue logLevelPref = Mockito.mock(PreferenceStringValue.class);
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        when(registrationPrefs.logLevel()).thenReturn(logLevelPref);

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            verify(logLevelPref).set("NOISE");
        });
    }

    @Test
    public void testLongPackageNameTruncatesDescriptionTo64() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");

        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 80; i++) {
            longName.append('a');
        }
        when(appInfoProvider.getPackageName()).thenReturn(longName.toString());

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            CreateTestDeviceRequest captured = captureRequest();
            String description = readPrivateField(captured, "mDesc");
            assertEquals(64, description.length());
        });
    }

    @Test
    public void testDeviceModelDisabledUsesFallbackName() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        when(collectModelPref.get()).thenReturn(false);

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            CreateTestDeviceRequest captured = captureRequest();
            String name = readPrivateField(captured, "mName");
            assertEquals("Android Device", name);
        });
    }

    @Test
    public void testCircularBufferEvaluatesLastSixKnocks() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");

        withStandardMocks(mocks -> {
            for (int i = 0; i < 4; i++) {
                fakeClock.addAndGet(10_000);
                detector.onForeground();
            }
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            verify(requestManager).sendRequest(any(), any());
        });
    }

    @Test
    public void testCountSaturatesAndDoesNotTriggerWithStaleKnocks() {
        for (int i = 0; i < 15; i++) {
            fakeClock.addAndGet(10_000);
            detector.onForeground();
        }
        assertEquals(KnockPatternDetector.REQUIRED_KNOCKS, detector.getCount());
    }

    @Test
    public void testTriggerExactlyAtWindowBoundary() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");

        withStandardMocks(mocks -> {
            fakeClock.set(1_000_000);
            detector.onForeground();
            for (int i = 1; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(6_000);
                detector.onForeground();
            }

            verify(requestManager).sendRequest(any(), any());
            assertEquals(0, detector.getCount());
        });
    }

    @Test
    public void testNoTriggerJustOverWindowBoundary() throws Exception {
        withStandardMocks(mocks -> {
            fakeClock.set(1_000_000);
            detector.onForeground();
            for (int i = 1; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(6_001);
                detector.onForeground();
            }

            verify(requestManager, never()).sendRequest(any(), any());
            assertEquals(KnockPatternDetector.REQUIRED_KNOCKS, detector.getCount());
        });
    }

    @Test
    public void testClipboardReceivesHwidAfterKnock() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("clip-hwid-xyz");

        Context context = RuntimeEnvironment.application;
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("seed", "seed-value"));

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            ClipData clip = clipboard.getPrimaryClip();
            assertNotNull("clipboard must contain a clip", clip);
            assertTrue("clip must have at least one item", clip.getItemCount() > 0);
            assertEquals("clip-hwid-xyz", clip.getItemAt(0).getText().toString());
        });
    }

    @Test
    public void testKnockUpdatesGlobalLogLevelToNoise() throws Exception {
        PreferenceStringValue logLevelPref = Mockito.mock(PreferenceStringValue.class);
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        when(registrationPrefs.logLevel()).thenReturn(logLevelPref);

        PWLog.updateLogLevel(PWLog.Level.INFO.name());
        assertFalse("precondition: noise must be disabled before knock", PWLog.isLoggable(null, PWLog.VERBOSE));

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            assertTrue("global log level must be NOISE after knock", PWLog.isLoggable(null, PWLog.VERBOSE));
        });
    }

    @Test
    public void testDeviceNameWhenCollectModelEnabled() throws Exception {
        when(registrationPrefs.hwid()).thenReturn(hwidPref);
        when(hwidPref.get()).thenReturn("test-hwid");
        when(collectModelPref.get()).thenReturn(true);

        withStandardMocks(mocks -> {
            for (int i = 0; i < KnockPatternDetector.REQUIRED_KNOCKS; i++) {
                fakeClock.addAndGet(1000);
                detector.onForeground();
            }

            CreateTestDeviceRequest captured = captureRequest();
            String name = readPrivateField(captured, "mName");
            assertNotNull(name);
            assertNotEquals("Android Device", name);
            assertTrue("device name must be non-empty, got: " + name, name.length() > 0);
        });
    }

    private static final class StaticMocks {
        final MockedStatic<RepositoryModule> repo;
        final MockedStatic<NetworkModule> net;
        final MockedStatic<AndroidPlatformModule> platform;

        StaticMocks(
                MockedStatic<RepositoryModule> repo,
                MockedStatic<NetworkModule> net,
                MockedStatic<AndroidPlatformModule> platform) {
            this.repo = repo;
            this.net = net;
            this.platform = platform;
        }
    }

    @FunctionalInterface
    private interface MockBody {
        void run(StaticMocks mocks) throws Exception;
    }

    private void withStandardMocks(MockBody body) throws Exception {
        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<NetworkModule> netMock = Mockito.mockStatic(NetworkModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {
            platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);
            platformMock.when(AndroidPlatformModule::getAppInfoProvider).thenReturn(appInfoProvider);
            repoMock.when(RepositoryModule::getRegistrationPreferences).thenReturn(registrationPrefs);
            repoMock.when(RepositoryModule::getNotificationPreferences).thenReturn(notificationPrefs);
            netMock.when(NetworkModule::getRequestManager).thenReturn(requestManager);
            body.run(new StaticMocks(repoMock, netMock, platformMock));
        }
    }

    private CreateTestDeviceRequest captureRequest() {
        ArgumentCaptor<CreateTestDeviceRequest> captor = ArgumentCaptor.forClass(CreateTestDeviceRequest.class);
        verify(requestManager).sendRequest(captor.capture(), any());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private static <T> T readPrivateField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(target);
    }
}
