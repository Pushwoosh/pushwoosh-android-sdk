package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLooper;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class SetAdvertisingIdTest {
    private PlatformTestManager platformTestManager;
    private PushwooshRepository pushwooshRepository;
    private RegistrationPrefs registrationPrefs;
    private PreferenceStringValue advertisingIdPref;

    @Before
    public void setUp() throws Exception {
        Config configMock = MockConfig.createMock();
        platformTestManager = new PlatformTestManager(configMock);
        platformTestManager.onApplicationCreated();

        registrationPrefs = platformTestManager.getRegistrationPrefsMock();

        // Real PreferenceStringValue with null SharedPreferences (handles gracefully)
        advertisingIdPref = new PreferenceStringValue(null, "pw_advertising_id", "");
        Mockito.doReturn(advertisingIdPref).when(registrationPrefs).advertisingId();

        pushwooshRepository = platformTestManager.getPushwooshRepositoryMock();

        // Inject mocks into Pushwoosh singleton
        Field reg = Pushwoosh.class.getDeclaredField("registrationPrefs");
        reg.setAccessible(true);
        reg.set(Pushwoosh.getInstance(), registrationPrefs);

        Field repo = Pushwoosh.class.getDeclaredField("pushwooshRepository");
        repo.setAccessible(true);
        repo.set(Pushwoosh.getInstance(), pushwooshRepository);

        // Stub sendAdvertisingId to immediately invoke success callback
        doAnswer(invocation -> {
                    Callback<Void, NetworkException> callback = invocation.getArgument(1);
                    if (callback != null) {
                        callback.process(Result.fromData(null));
                    }
                    return null;
                })
                .when(pushwooshRepository)
                .sendAdvertisingId(any(), any());
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void validIdSendsRequest() {
        Pushwoosh.getInstance().setAdvertisingId("test-gaid-123");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(pushwooshRepository).sendAdvertisingId(eq("test-gaid-123"), any());
    }

    @Test
    public void validIdSavesToPrefsOnSuccess() {
        Pushwoosh.getInstance().setAdvertisingId("test-gaid-123");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals("test-gaid-123", advertisingIdPref.get());
    }

    @Test
    public void nullSendsRequest() {
        advertisingIdPref.set("old-gaid");

        Pushwoosh.getInstance().setAdvertisingId(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(pushwooshRepository).sendAdvertisingId(isNull(), any());
    }

    @Test
    public void nullClearsPrefsOnSuccess() {
        advertisingIdPref.set("old-gaid");

        Pushwoosh.getInstance().setAdvertisingId(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals("", advertisingIdPref.get());
    }

    @Test
    public void nullSkipsWhenAlreadyCleared() {
        Pushwoosh.getInstance().setAdvertisingId(null);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(pushwooshRepository, never()).sendAdvertisingId(any(), any());
    }

    @Test
    public void emptyStringTreatedAsNull() {
        advertisingIdPref.set("old-gaid");

        Pushwoosh.getInstance().setAdvertisingId("");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(pushwooshRepository).sendAdvertisingId(isNull(), any());
    }

    @Test
    public void zeroUuidTreatedAsNull() {
        advertisingIdPref.set("old-gaid");

        Pushwoosh.getInstance().setAdvertisingId("00000000-0000-0000-0000-000000000000");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        assertEquals("", advertisingIdPref.get());
        verify(pushwooshRepository).sendAdvertisingId(isNull(), any());
    }

    @Test
    public void duplicateValueSkipsRequest() {
        advertisingIdPref.set("test-gaid-123");

        Pushwoosh.getInstance().setAdvertisingId("test-gaid-123");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(pushwooshRepository, never()).sendAdvertisingId(any(), any());
    }

    @Test
    public void differentValueSendsRequest() {
        advertisingIdPref.set("old-gaid");

        Pushwoosh.getInstance().setAdvertisingId("new-gaid");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(pushwooshRepository).sendAdvertisingId(eq("new-gaid"), any());
    }

    @Test
    public void doesNotSaveToPrefsOnFailure() {
        doAnswer(invocation -> {
                    Callback<Void, NetworkException> callback = invocation.getArgument(1);
                    if (callback != null) {
                        callback.process(Result.fromException(new NetworkException("fail")));
                    }
                    return null;
                })
                .when(pushwooshRepository)
                .sendAdvertisingId(any(), any());

        Pushwoosh.getInstance().setAdvertisingId("test-gaid");
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        verify(pushwooshRepository).sendAdvertisingId(eq("test-gaid"), any());
        assertEquals("", advertisingIdPref.get());
    }
}
