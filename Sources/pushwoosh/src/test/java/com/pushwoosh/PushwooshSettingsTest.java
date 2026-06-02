package com.pushwoosh;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.pushwoosh.internal.event.AppIdChangedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.PushwooshNotificationManager.ApplicationIdReadyEvent;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.testutil.EventListenerWrapper;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushwooshSettingsTest {
    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    //
    // ApplicationId part
    // -----------------------------------------------------------------------

    @Test
    public void setMetaAppIDTest() throws Exception {
        // Preconditions:
        String appIDTest = "Test_AppID";
        String appIDTestMeta = "Test_AppID_Meta";

        Config config = MockConfig.createMock(appIDTestMeta);

        // Steps:
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        platformTestManager.getNotificationManager().setAppId(appIDTest);

        // Postconditions:
        RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();
        assertThat(registrationPrefs.applicationId().get(), is(appIDTest));
    }

    // Tests appID value from setAppId method set in registrationPrefs when AndroidManifest AppId value is not presented
    @Test
    public void setAppIDTest() throws Exception {
        // Preconditions:
        String appIDTest = "Test_AppID";
        String appIDTestMeta = null;

        Config config = MockConfig.createMock(appIDTestMeta);

        // Steps:
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        platformTestManager.getNotificationManager().setAppId(appIDTest);

        // Postconditions:
        RegistrationPrefs registrationPrefs = platformTestManager.getRegistrationPrefs();
        assertThat(registrationPrefs.applicationId().get(), is(appIDTest));
    }

    // Tests application throws IllegalArgumentException and empty string set in registrationPrefs as AppID value
    // when AndroidManifest AppId = "" and setAppID method called with empty string value
    @Test(expected = IllegalArgumentException.class)
    public void setEmptyAppIDTest() throws Exception {
        // Preconditions:
        String appIDTest = "";
        String appIDTestMeta = null;

        Config config = MockConfig.createMock(appIDTestMeta);

        // Steps:
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        platformTestManager.getNotificationManager().setAppId(appIDTest);
    }

    // First-time setAppId after empty manifest must NOT fire AppIdChangedEvent —
    // a fresh install with appCode arriving via runtime API is not a "change".
    @Test
    public void setAppId_firstTimeAfterEmptyManifest_doesNotFireAppIdChangedEvent() throws Exception {
        Config config = MockConfig.createMock(null);
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        platformTestManager.getNotificationManager().setAppId("XXXXX-XXXXX");

        verify(listener, never()).onReceive(any(AppIdChangedEvent.class));
        RegistrationPrefs prefs = platformTestManager.getRegistrationPrefs();
        assertEquals("XXXXX-XXXXX", prefs.applicationId().get());
        assertEquals(
                "https://XXXXX-XXXXX.api.pushwoosh.com/json/1.3/",
                prefs.baseUrl().get());
    }

    // First-time setAppId must NOT trigger unregister: nothing has been registered yet.
    @Test
    public void setAppId_firstTimeAfterEmptyManifest_doesNotUnregister() throws Exception {
        Config config = MockConfig.createMock(null);
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();

        try (MockedStatic<DeviceRegistrar> mocked = Mockito.mockStatic(DeviceRegistrar.class)) {
            platformTestManager.getNotificationManager().setAppId("XXXXX-XXXXX");
            mocked.verify(() -> DeviceRegistrar.unregisterWithServer(anyString(), anyString()), never());
        }
    }

    // Real change of appId must fire AppIdChangedEvent and reset baseUrl to new app's domain.
    @Test
    public void setAppId_realChange_firesAppIdChangedEventAndClears() throws Exception {
        Config config = MockConfig.createMock("OLDAPP-OLDAPP");
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        // ensure the constructor-bootstrap appId reaches setAppId path
        platformTestManager.getNotificationManager().setAppId("OLDAPP-OLDAPP");

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        platformTestManager.getNotificationManager().setAppId("NEWAPP-NEWAPP");

        verify(listener, timeout(500).times(1)).onReceive(any(AppIdChangedEvent.class));
        RegistrationPrefs prefs = platformTestManager.getRegistrationPrefs();
        assertEquals("NEWAPP-NEWAPP", prefs.applicationId().get());
        assertEquals(
                "https://NEWAPP-NEWAPP.api.pushwoosh.com/json/1.3/",
                prefs.baseUrl().get());
    }

    // Idempotent setAppId(same value) must not fire AppIdChangedEvent.
    @Test
    public void setAppId_idempotentSameValue_noSideEffects() throws Exception {
        Config config = MockConfig.createMock("SAME_APP");
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        PushwooshNotificationManager notificationManager = platformTestManager.getNotificationManager();
        notificationManager.setAppId("SAME_APP");

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        notificationManager.setAppId("SAME_APP");

        verify(listener, never()).onReceive(any(AppIdChangedEvent.class));
    }

    // First setAppId must fire ApplicationIdReadyEvent exactly once.
    @Test
    public void setAppId_firstTime_firesApplicationIdReadyEvent() throws Exception {
        Config config = MockConfig.createMock(null);
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();

        EventListener<ApplicationIdReadyEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(ApplicationIdReadyEvent.class, listener);

        platformTestManager.getNotificationManager().setAppId("XXXXX-XXXXX");

        verify(listener, timeout(500).times(1)).onReceive(any(ApplicationIdReadyEvent.class));
    }

    // Idempotent setAppId(same value) must NOT refire ApplicationIdReadyEvent —
    // the appIdReadyEventSent flag gates duplicate emissions.
    @Test
    public void setAppId_idempotentSameValue_doesNotRefireApplicationIdReadyEvent() throws Exception {
        Config config = MockConfig.createMock("SAME_APP");
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        PushwooshNotificationManager notificationManager = platformTestManager.getNotificationManager();
        notificationManager.setAppId("SAME_APP"); // first call fires the event

        EventListener<ApplicationIdReadyEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(ApplicationIdReadyEvent.class, listener);

        notificationManager.setAppId("SAME_APP"); // idempotent — must not refire

        verify(listener, never()).onReceive(any(ApplicationIdReadyEvent.class));
    }

    // Real appId change must refire ApplicationIdReadyEvent — the flag is reset in the change branch.
    @Test
    public void setAppId_realChange_refiresApplicationIdReadyEvent() throws Exception {
        Config config = MockConfig.createMock("OLDAPP-OLDAPP");
        platformTestManager = new PlatformTestManager(config);
        platformTestManager.setUp();
        PushwooshNotificationManager notificationManager = platformTestManager.getNotificationManager();
        notificationManager.setAppId("OLDAPP-OLDAPP"); // first call fires the event

        EventListener<ApplicationIdReadyEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(ApplicationIdReadyEvent.class, listener);

        notificationManager.setAppId("NEWAPP-NEWAPP"); // real change — must refire

        verify(listener, timeout(500).times(1)).onReceive(any(ApplicationIdReadyEvent.class));
    }
}
