package com.pushwoosh.notification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.pushwoosh.internal.event.AppIdChangedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.notification.PushwooshNotificationManager.ApplicationIdReadyEvent;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryTestManager;
import com.pushwoosh.testutil.EventListenerWrapper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class PushwooshNotificationManagerTest {

    private DeviceRegistrar deviceRegistrar;
    private RequestManager requestManager;
    private RegistrationPrefs registrationPrefs;

    @Before
    public void setUp() {
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
        deviceRegistrar = mock(DeviceRegistrar.class);
        requestManager = mock(RequestManager.class);
        NetworkModule.setRequestManager(requestManager);
    }

    @After
    public void tearDown() {
        if (registrationPrefs != null) {
            RepositoryTestManager.destroyRegistrationPrefs(registrationPrefs);
            registrationPrefs = null;
        }
        NetworkModule.setRequestManager(null);
        EventBus.clearSubscribersMap();
    }

    // Real RegistrationPrefs (tests assert on applicationId()/baseUrl()), mock DeviceRegistrar —
    // the seam this constructor exists for. The same mock goes into both prefs and manager.
    private PushwooshNotificationManager createManager(String configAppId) {
        Config config = MockConfig.createMock(configAppId);
        registrationPrefs = RepositoryTestManager.createRegistrationPrefs(config, deviceRegistrar);
        return new PushwooshNotificationManager(config, mock(PushRegistrar.class), registrationPrefs, deviceRegistrar);
    }

    //
    // ApplicationId part
    // -----------------------------------------------------------------------

    @Test
    public void setMetaAppIDTest() {
        PushwooshNotificationManager manager = createManager("Test_AppID_Meta");

        manager.setAppId("Test_AppID");

        assertThat(registrationPrefs.applicationId().get(), is("Test_AppID"));
    }

    // Tests appID value from setAppId method set in registrationPrefs when AndroidManifest AppId value is not presented
    @Test
    public void setAppIDTest() {
        PushwooshNotificationManager manager = createManager(null);

        manager.setAppId("Test_AppID");

        assertThat(registrationPrefs.applicationId().get(), is("Test_AppID"));
    }

    // Empty application code is rejected inside applyAppCode: logged no-op, nothing persisted.
    @Test
    public void setEmptyAppIDTest() {
        PushwooshNotificationManager manager = createManager(null);

        manager.setAppId("");

        assertThat(registrationPrefs.applicationId().get(), is(""));
        assertThat(registrationPrefs.baseUrl().get(), is(""));
    }

    // First-time setAppId after empty manifest must NOT fire AppIdChangedEvent —
    // a fresh install with appCode arriving via runtime API is not a "change".
    @Test
    public void setAppId_firstTimeAfterEmptyManifest_doesNotFireAppIdChangedEvent() {
        PushwooshNotificationManager manager = createManager(null);

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        manager.setAppId("XXXXX-XXXXX");

        verify(listener, never()).onReceive(any(AppIdChangedEvent.class));
        assertEquals("XXXXX-XXXXX", registrationPrefs.applicationId().get());
        assertEquals(
                "https://XXXXX-XXXXX.api.pushwoosh.com/json/1.3/",
                registrationPrefs.baseUrl().get());
    }

    // First-time setAppId must NOT trigger unregister: nothing has been registered yet.
    @Test
    public void setAppId_firstTimeAfterEmptyManifest_doesNotUnregister() {
        PushwooshNotificationManager manager = createManager(null);

        manager.setAppId("XXXXX-XXXXX");

        verify(deviceRegistrar, never()).unregisterWithServer(any(), any(), any());
    }

    // Real change of appId must fire AppIdChangedEvent and reset baseUrl to new app's domain.
    @Test
    public void setAppId_realChange_firesAppIdChangedEventAndClears() {
        PushwooshNotificationManager manager = createManager("OLDAPP-OLDAPP");
        // ensure the constructor-bootstrap appId reaches setAppId path
        manager.setAppId("OLDAPP-OLDAPP");

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        manager.setAppId("NEWAPP-NEWAPP");

        verify(listener, timeout(500).times(1)).onReceive(any(AppIdChangedEvent.class));
        assertEquals("NEWAPP-NEWAPP", registrationPrefs.applicationId().get());
        assertEquals(
                "https://NEWAPP-NEWAPP.api.pushwoosh.com/json/1.3/",
                registrationPrefs.baseUrl().get());
    }

    // Idempotent setAppId(same value) must not fire AppIdChangedEvent.
    @Test
    public void setAppId_idempotentSameValue_noSideEffects() {
        PushwooshNotificationManager manager = createManager("SAME_APP");
        manager.setAppId("SAME_APP");

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        manager.setAppId("SAME_APP");

        verify(listener, never()).onReceive(any(AppIdChangedEvent.class));
    }

    // Same app code with a NEW base URL is an address migration, not a registration-target change:
    // the url is applied and requests move over, but the device stays registered — no unregister,
    // no forceRegister, no AppIdChangedEvent, no ApplicationIdReadyEvent refire.
    @Test
    public void setAppId_sameAppIdNewBaseUrl_appliesUrlWithoutChangeCycle() {
        PushwooshNotificationManager manager = createManager("SAME_APP");
        manager.setAppId("SAME_APP");

        registrationPrefs.pushToken().set("token-1");
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.isRegisteredForPush().set(true);

        EventListener<AppIdChangedEvent> changedListener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, changedListener);
        EventListener<ApplicationIdReadyEvent> readyListener = EventListenerWrapper.spy();
        EventBus.subscribe(ApplicationIdReadyEvent.class, readyListener);

        manager.setAppId("SAME_APP", "https://api.example.com/json/1.3/");

        verify(deviceRegistrar, never()).unregisterWithServer(any(), any(), any());
        verify(changedListener, never()).onReceive(any(AppIdChangedEvent.class));
        verify(readyListener, never()).onReceive(any(ApplicationIdReadyEvent.class));
        assertEquals(
                "https://api.example.com/json/1.3/", registrationPrefs.baseUrl().get());
        assertThat(registrationPrefs.forceRegister().get(), is(false));
    }

    // Regression for the false region-change: the server writes pw_base_url too (rotation,
    // set_base_url push command). An app that calls setAppId(code, url) on every start right
    // after such an override must NOT run the change cycle — the address is simply taken back.
    @Test
    public void setAppId_afterServerBaseUrlOverride_noChangeCycle() {
        PushwooshNotificationManager manager = createManager(null);
        manager.setAppId("SAME_APP", "https://api.example.com/json/1.3/");

        registrationPrefs.pushToken().set("token-1");
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.isRegisteredForPush().set(true);
        // server rotation and the set_base_url command land through this same single write point
        registrationPrefs.updateBaseUrl("https://rotated.example.com/json/1.3/");

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        manager.setAppId("SAME_APP", "https://api.example.com/json/1.3/");

        verify(deviceRegistrar, never()).unregisterWithServer(any(), any(), any());
        verify(listener, never()).onReceive(any(AppIdChangedEvent.class));
        assertEquals(
                "https://api.example.com/json/1.3/", registrationPrefs.baseUrl().get());
    }

    // New app code with an explicit url must persist that url, not the computed default of the new code.
    @Test
    public void setAppId_newAppIdWithBaseUrl_appliesCustomUrlNotDefault() {
        PushwooshNotificationManager manager = createManager("OLDAPP-OLDAPP");
        manager.setAppId("OLDAPP-OLDAPP");

        manager.setAppId("NEWAPP-NEWAPP", "https://api.example.com/json/1.3/");

        assertEquals("NEWAPP-NEWAPP", registrationPrefs.applicationId().get());
        assertEquals(
                "https://api.example.com/json/1.3/", registrationPrefs.baseUrl().get());
    }

    // Multi-app: switching the app code on the same host is a real registration-target change —
    // the full cycle runs (unregister the old code on the current url, force re-register,
    // AppIdChangedEvent) even though the base URL stays the same.
    @Test
    public void setAppId_newAppIdSameBaseUrl_runsFullChangeCycle() {
        PushwooshNotificationManager manager = createManager(null);
        manager.setAppId("OLDAPP-OLDAPP", "https://api.example.com/json/1.3/");

        registrationPrefs.pushToken().set("token-1");
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.isRegisteredForPush().set(true);

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        manager.setAppId("NEWAPP-NEWAPP", "https://api.example.com/json/1.3/");

        verify(deviceRegistrar).unregisterWithServer("token-1", "https://api.example.com/json/1.3/", "OLDAPP-OLDAPP");
        verify(listener, timeout(500).times(1)).onReceive(any(AppIdChangedEvent.class));
        assertEquals("NEWAPP-NEWAPP", registrationPrefs.applicationId().get());
        assertEquals(
                "https://api.example.com/json/1.3/", registrationPrefs.baseUrl().get());
        assertTrue(registrationPrefs.forceRegister().get());
    }

    // When code and URL change together, unregister must target the PREVIOUS url — the backend
    // the device is actually registered on — not the incoming one.
    @Test
    public void setAppId_newAppIdNewBaseUrl_unregistersOnOldUrl() {
        PushwooshNotificationManager manager = createManager(null);
        manager.setAppId("OLDAPP-OLDAPP", "https://old.example.com/json/1.3/");

        registrationPrefs.pushToken().set("token-1");
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.isRegisteredForPush().set(true);

        manager.setAppId("NEWAPP-NEWAPP", "https://new.example.com/json/1.3/");

        verify(deviceRegistrar).unregisterWithServer("token-1", "https://old.example.com/json/1.3/", "OLDAPP-OLDAPP");
        assertEquals(
                "https://new.example.com/json/1.3/", registrationPrefs.baseUrl().get());
    }

    // Pins the intended order (spec: the change cycle is one block AFTER the retarget): whatever
    // the cycle triggers must already see the request manager pointing at the new base URL. The
    // unregister itself is immune — it carries the previous URL explicitly — but the order is a
    // design decision, not an accident of master.
    @Test
    public void setAppId_realChange_retargetsRequestManagerBeforeChangeCycle() {
        PushwooshNotificationManager manager = createManager(null);
        manager.setAppId("OLDAPP-OLDAPP", "https://old.example.com/json/1.3/");

        registrationPrefs.pushToken().set("token-1");
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.isRegisteredForPush().set(true);

        manager.setAppId("NEWAPP-NEWAPP", "https://new.example.com/json/1.3/");

        InOrder inOrder = Mockito.inOrder(requestManager, deviceRegistrar);
        inOrder.verify(requestManager).updateBaseUrl("https://new.example.com/json/1.3/");
        inOrder.verify(deviceRegistrar)
                .unregisterWithServer("token-1", "https://old.example.com/json/1.3/", "OLDAPP-OLDAPP");
    }

    // The manager itself must reject an invalid explicit URL before any side effect: the change
    // cycle must not run half-way (unregister + removeAppId) for a pair that can never be applied.
    @Test
    public void setAppId_invalidBaseUrlDirectManagerCall_ignoresWholeCall() {
        PushwooshNotificationManager manager = createManager(null);
        manager.setAppId("OLDAPP-OLDAPP", "https://api.example.com/json/1.3/");

        registrationPrefs.pushToken().set("token-1");
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.isRegisteredForPush().set(true);

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        manager.setAppId("NEWAPP-NEWAPP", "not-a-url");

        verify(deviceRegistrar, never()).unregisterWithServer(any(), any(), any());
        verify(listener, never()).onReceive(any(AppIdChangedEvent.class));
        assertEquals("OLDAPP-OLDAPP", registrationPrefs.applicationId().get());
        assertEquals(
                "https://api.example.com/json/1.3/", registrationPrefs.baseUrl().get());
    }

    // The rejected verdict must stop the manager before it acts on it: a rejected pair carries no
    // base URL to retarget to, and firing ApplicationIdReadyEvent here would latch
    // appIdReadyEventSent and let the SDK go READY with no application code at all.
    @Test
    public void setAppId_rejectedPair_doesNotRetargetOrFireReadyEvent() {
        PushwooshNotificationManager manager = createManager(null);

        EventListener<ApplicationIdReadyEvent> readyListener = EventListenerWrapper.spy();
        EventBus.subscribe(ApplicationIdReadyEvent.class, readyListener);

        manager.setAppId("NEWAPP-NEWAPP", "not-a-url");

        verify(requestManager, never()).updateBaseUrl(any());
        verify(readyListener, never()).onReceive(any(ApplicationIdReadyEvent.class));
    }

    // First set of the pair on a fresh install is not a change: no unregister, no AppIdChangedEvent.
    @Test
    public void setAppId_firstSetWithBaseUrl_noChangeCycle() {
        PushwooshNotificationManager manager = createManager(null);

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);

        manager.setAppId("XXXXX-XXXXX", "https://api.example.com/json/1.3/");

        verify(deviceRegistrar, never()).unregisterWithServer(any(), any(), any());
        verify(listener, never()).onReceive(any(AppIdChangedEvent.class));
        assertEquals("XXXXX-XXXXX", registrationPrefs.applicationId().get());
        assertEquals(
                "https://api.example.com/json/1.3/", registrationPrefs.baseUrl().get());
    }

    // A url switch must repoint the in-memory request manager too, not only the persisted value:
    // otherwise the running process keeps sending requests to the previous region until a restart.
    @Test
    public void setAppId_sameAppIdNewBaseUrl_repointsRequestManager() {
        PushwooshNotificationManager manager = createManager("SAME_APP");
        manager.setAppId("SAME_APP");
        Mockito.clearInvocations(requestManager);

        manager.setAppId("SAME_APP", "https://api.example.com/json/1.3/");

        verify(requestManager).updateBaseUrl("https://api.example.com/json/1.3/");
    }

    // Cold start re-applies the persisted pair: initialize() feeds the stored code back through
    // setAppId(code) with no explicit url, so the custom url must survive and reach the request manager
    // — without unregistering the device from the region it is already registered on.
    @Test
    public void initialize_persistedCustomBaseUrl_reappliesUrlWithoutChangeCycle() {
        PushwooshNotificationManager manager = createManager(null);
        manager.setAppId("SAME_APP", "https://api.example.com/json/1.3/");

        registrationPrefs.pushToken().set("token-1");
        registrationPrefs.registeredOnServer().set(true);
        registrationPrefs.isRegisteredForPush().set(true);

        EventListener<AppIdChangedEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(AppIdChangedEvent.class, listener);
        Mockito.clearInvocations(requestManager);

        manager.initialize();

        verify(deviceRegistrar, never()).unregisterWithServer(any(), any(), any());
        verify(listener, never()).onReceive(any(AppIdChangedEvent.class));
        assertEquals(
                "https://api.example.com/json/1.3/", registrationPrefs.baseUrl().get());
        verify(requestManager).updateBaseUrl("https://api.example.com/json/1.3/");
    }

    // First setAppId must fire ApplicationIdReadyEvent exactly once.
    @Test
    public void setAppId_firstTime_firesApplicationIdReadyEvent() {
        PushwooshNotificationManager manager = createManager(null);

        EventListener<ApplicationIdReadyEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(ApplicationIdReadyEvent.class, listener);

        manager.setAppId("XXXXX-XXXXX");

        verify(listener, timeout(500).times(1)).onReceive(any(ApplicationIdReadyEvent.class));
    }

    // Idempotent setAppId(same value) must NOT refire ApplicationIdReadyEvent —
    // the appIdReadyEventSent flag gates duplicate emissions.
    @Test
    public void setAppId_idempotentSameValue_doesNotRefireApplicationIdReadyEvent() {
        PushwooshNotificationManager manager = createManager("SAME_APP");
        manager.setAppId("SAME_APP"); // first call fires the event

        EventListener<ApplicationIdReadyEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(ApplicationIdReadyEvent.class, listener);

        manager.setAppId("SAME_APP"); // idempotent — must not refire

        verify(listener, never()).onReceive(any(ApplicationIdReadyEvent.class));
    }

    // Real appId change must refire ApplicationIdReadyEvent — the flag is reset in the change branch.
    @Test
    public void setAppId_realChange_refiresApplicationIdReadyEvent() {
        PushwooshNotificationManager manager = createManager("OLDAPP-OLDAPP");
        manager.setAppId("OLDAPP-OLDAPP"); // first call fires the event

        EventListener<ApplicationIdReadyEvent> listener = EventListenerWrapper.spy();
        EventBus.subscribe(ApplicationIdReadyEvent.class, listener);

        manager.setAppId("NEWAPP-NEWAPP"); // real change — must refire

        verify(listener, timeout(500).times(1)).onReceive(any(ApplicationIdReadyEvent.class));
    }
}
