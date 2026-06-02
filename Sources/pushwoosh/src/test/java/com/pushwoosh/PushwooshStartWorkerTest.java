package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.pushwoosh.appevents.PushwooshDefaultEvents;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.PushRegistrarHelper;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.AppIdChangedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.ReverseProxyReadyEvent;
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.internal.platform.utils.DeviceUuidGetter;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class PushwooshStartWorkerTest {
    private PushwooshStartWorker pushwooshStartWorker;

    // Mocked dependencies
    @Mock(answer = RETURNS_DEEP_STUBS)
    private Config configMock;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private RegistrationPrefs registrationPrefsMock;

    @Mock
    private PushwooshRepository pushwooshRepositoryMock;

    @Mock
    private PushwooshNotificationManager notificationManagerMock;

    @Mock
    private PushwooshInAppImpl pushwooshInAppMock;

    @Mock
    private DeviceRegistrar deviceRegistrarMock;

    @Mock
    private PushwooshDefaultEvents pushwooshDefaultEventsMock;

    @Mock
    private PushRegistrarHelper pushRegistrarHelperMock;

    @Mock
    private RequestManager requestManagerMock;

    @Mock
    private Plugin pluginMock1;

    @Mock
    private Plugin pluginMock2;

    private AutoCloseable mocks;

    private void setupConfig() {
        when(configMock.getLogLevel()).thenReturn("NOISE");
    }

    private void setupAndroidPlatformModuleWithFakeContext() {
        Context ctx = spy(RuntimeEnvironment.application);
        when(ctx.getApplicationContext()).thenReturn(ctx);
        AndroidPlatformModule.init(ctx, true);
    }

    private void setupRepository() {
        RepositoryModule.init(configMock, deviceRegistrarMock);
    }

    private void configureFakePreferences() {
        when(registrationPrefsMock.hwid().get()).thenReturn("test-hwid-12345");
        when(registrationPrefsMock.applicationId().get()).thenReturn("test-app-id");
        when(registrationPrefsMock.userId().get()).thenReturn("");
        when(registrationPrefsMock.apiToken().get()).thenReturn("test-api-token");
        when(registrationPrefsMock.baseUrl().get()).thenReturn("https://test.api.pushwoosh.com");
        when(registrationPrefsMock.pushToken().get()).thenReturn("test-push-token");
    }

    @Before
    public void setUp() throws Exception {
        // logs
        ShadowLog.stream = System.out;
        mocks = MockitoAnnotations.openMocks(this);

        // setup config
        setupConfig();

        // configure base preferences for testing
        configureFakePreferences();

        // initialize real AndroidPlatformModule with fake context
        setupAndroidPlatformModuleWithFakeContext();

        // initialize real RepositoryModule with fake Preferences
        setupRepository();

        PWLog.init();

        // Mock PushwooshStartWorker dependencies
        when(pushRegistrarHelperMock.initDefaultPushRegistrarInPlugin()).thenReturn(false);
        DeviceUuidGetter testGetter = callback -> callback.onGetHwid("test-hwid-12345");

        // Create PushwooshStartWorker instance
        pushwooshStartWorker = new PushwooshStartWorker(
                configMock,
                registrationPrefsMock,
                pushwooshRepositoryMock,
                notificationManagerMock,
                pushwooshInAppMock,
                deviceRegistrarMock,
                pushwooshDefaultEventsMock,
                pushRegistrarHelperMock,
                testGetter);

        // Reset SDK state for clean test
        SdkStateProvider.getInstance().resetForTesting();
    }

    @After
    public void tearDown() throws Exception {
        pushwooshStartWorker.shutdown();
        SdkStateProvider.getInstance().resetForTesting();
        mocks.close();
    }

    private void initializeAndRunPushwooshStartWorker() {
        // Initialize
        pushwooshStartWorker.onApplicationCreated();

        // Simulate AppIdReadyEvent from NotificationManager
        EventBus.sendEvent(new PushwooshNotificationManager.ApplicationIdReadyEvent());
    }

    private void ensureInitializingState() {
        // Verify initial state
        assertEquals(
                SdkStateProvider.SdkState.INITIALIZING,
                SdkStateProvider.getInstance().getCurrentState());
        assertFalse(SdkStateProvider.getInstance().isReady());
    }

    private void ensureReadyState() throws InterruptedException {
        // Verify SDK state
        assertEquals(
                SdkStateProvider.SdkState.READY, SdkStateProvider.getInstance().getCurrentState());
        assertTrue(SdkStateProvider.getInstance().isReady());
    }

    private void ensureStarted() throws InterruptedException {
        // setReady() is now posted to main looper, so we need to idle it
        // to process the pending message in Robolectric's LEGACY mode
        for (int i = 0; i < 20; i++) {
            ShadowLooper.idleMainLooper();
            if (SdkStateProvider.getInstance().isReady()) {
                break;
            }
            Thread.sleep(100);
        }

        CountDownLatch latch = new CountDownLatch(1);
        SdkStateProvider.getInstance().executeOrQueue(latch::countDown);

        assertTrue("SDK initialization should be ok", latch.await(2, TimeUnit.SECONDS));
    }

    /**
     * Verifies that SDK successfully initializes and transitions to READY state.
     * Ensures default events are initialized and no unexpected requests are sent.
     */
    @Test
    public void testSuccessfulInitialization() throws InterruptedException {

        // Verify initial state
        ensureInitializingState();

        initializeAndRunPushwooshStartWorker();

        // Verify SDK was started
        ensureStarted();

        // Verify SDK state
        ensureReadyState();

        Mockito.verify(pushwooshDefaultEventsMock, Mockito.times(1)).init();

        // Verify no any calls to request manager
        Mockito.verify(requestManagerMock, Mockito.never()).sendRequest(Mockito.any(), Mockito.any());
    }

    /**
     * Verifies that SDK transitions to ERROR state when HWID fetching fails.
     * Tests failure handling in the device identification process.
     */
    @Test
    public void testInitializationFailureIfHWIDFailed() throws InterruptedException {

        // fake error - we cant get hwid
        DeviceUuidGetter failingGetter = callback -> {
            throw new RuntimeException("HWID fetch failed for testing");
        };

        pushwooshStartWorker = new PushwooshStartWorker(
                configMock,
                registrationPrefsMock,
                pushwooshRepositoryMock,
                notificationManagerMock,
                pushwooshInAppMock,
                deviceRegistrarMock,
                pushwooshDefaultEventsMock,
                pushRegistrarHelperMock,
                failingGetter);

        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();

        boolean errorStateReached = false;
        for (int i = 0; i < 10; i++) {
            if (SdkStateProvider.getInstance().getCurrentState() == SdkStateProvider.SdkState.ERROR) {
                errorStateReached = true;
                break;
            }
            Thread.sleep(100);
        }

        assertTrue("Should be in ERROR state after error", errorStateReached);
        assertFalse("Cant be ready", SdkStateProvider.getInstance().isReady());
    }

    /**
     * Verifies that SDK transitions to ERROR state when application code fetching fails.
     * Tests failure handling in the app configuration process.
     */
    @Test
    public void testInitializationFailureIfAppCodeFailed() throws InterruptedException {

        when(registrationPrefsMock.applicationId().get()).thenThrow(new RuntimeException("App code fetch failed"));

        ensureInitializingState();

        initializeAndRunPushwooshStartWorker();

        boolean errorStateReached = false;
        for (int i = 0; i < 10; i++) {
            if (SdkStateProvider.getInstance().getCurrentState() == SdkStateProvider.SdkState.ERROR) {
                errorStateReached = true;
                break;
            }
            Thread.sleep(100);
        }

        assertTrue("Should be in ERROR state after error", errorStateReached);
        assertFalse("Cant be ready", SdkStateProvider.getInstance().isReady());
    }

    /**
     * Verifies that SDK remains in INITIALIZING state when ApplicationIdReadyEvent is not received.
     * Tests that missing critical events prevent SDK from becoming ready and tasks remain queued.
     */
    @Test
    public void testInitializationStuckWhenAppIdEventNotReceived() throws InterruptedException {

        ensureInitializingState();

        // start initialization but DO NOT send ApplicationIdReadyEvent
        pushwooshStartWorker.onApplicationCreated();

        // wait some time to ensure HWID fetch completes but app code fetch stucks
        Thread.sleep(1000);

        // SDK should remain in INITIALIZING state because latch waits for 2 countdowns but gets only 1
        assertEquals(
                "Should stay INITIALIZING when AppId event missing",
                SdkStateProvider.SdkState.INITIALIZING,
                SdkStateProvider.getInstance().getCurrentState());
        assertFalse("Should not be ready", SdkStateProvider.getInstance().isReady());

        // tasks should be queued, not executed
        CountDownLatch queuedTaskLatch = new CountDownLatch(1);
        SdkStateProvider.getInstance().executeOrQueue(queuedTaskLatch::countDown);

        // task should timeout - not execute because SDK never becomes ready
        assertFalse(
                "Task should remain queued when initialization stucks",
                queuedTaskLatch.await(500, TimeUnit.MILLISECONDS));

        // verify that sending the missing event unblocks everything
        EventBus.sendEvent(new PushwooshNotificationManager.ApplicationIdReadyEvent());

        // idle main looper to process setReady() posted from background thread
        for (int i = 0; i < 20; i++) {
            ShadowLooper.idleMainLooper();
            if (SdkStateProvider.getInstance().isReady()) {
                break;
            }
            Thread.sleep(100);
        }

        // now the queued task should execute
        assertTrue("Task should execute after missing event arrives", queuedTaskLatch.await(2, TimeUnit.SECONDS));

        ensureReadyState();
    }

    /**
     * Verifies that repeated initialization attempts are ignored once SDK is already initialized.
     * Ensures SDK state remains stable and prevents duplicate initialization.
     */
    @Test
    public void testRepeatedInitializationIgnored() throws InterruptedException {

        // first do successful initialization
        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();

        // save current state
        SdkStateProvider.SdkState readyState = SdkStateProvider.getInstance().getCurrentState();

        // try to initialize again
        pushwooshStartWorker.onApplicationCreated();

        // give time for possible processing
        Thread.sleep(500);

        // state should remain READY, repeated initialization is ignored
        assertEquals(
                "Repeated initialization should be ignored",
                readyState,
                SdkStateProvider.getInstance().getCurrentState());
        assertTrue("SDK should stay ready", SdkStateProvider.getInstance().isReady());
    }

    /**
     * Verifies that tasks are queued before initialization and executed after SDK becomes ready.
     * Tests the task queuing mechanism during SDK startup.
     */
    @Test
    public void testTaskQueueingBeforeInitialization() throws InterruptedException {

        ensureInitializingState();

        // When: Try to execute a task BEFORE starting initialization
        CountDownLatch queuedTaskLatch = new CountDownLatch(1);
        SdkStateProvider.getInstance().executeOrQueue(queuedTaskLatch::countDown);

        // This should timeout because SDK is not ready yet
        assertFalse(
                "Task should be queued, not executed immediately", queuedTaskLatch.await(500, TimeUnit.MILLISECONDS));

        // Verify SDK is still INITIALIZING
        ensureInitializingState();

        initializeAndRunPushwooshStartWorker();

        // idle main looper to process setReady() posted from background thread
        for (int i = 0; i < 20; i++) {
            ShadowLooper.idleMainLooper();
            if (SdkStateProvider.getInstance().isReady()) {
                break;
            }
            Thread.sleep(100);
        }

        // Now the queued task should execute when SDK becomes ready
        assertTrue("Queued task should execute after SDK initialization", queuedTaskLatch.await(2, TimeUnit.SECONDS));

        // Verify SDK is now READY
        ensureReadyState();
    }

    /**
     * Verifies that ApplicationOpenEvent triggers all necessary operations like analytics, registration and in-app updates.
     * Tests the complete app open flow after SDK initialization.
     */
    @Test
    public void testApplicationOpenEventHandling() throws InterruptedException {

        // Verify initial state
        ensureInitializingState();

        initializeAndRunPushwooshStartWorker();

        // Simulate ApplicationOpenEvent
        EventBus.sendEvent(new ApplicationOpenDetector.ApplicationOpenEvent());

        ensureStarted();

        // Verify SDK state
        ensureReadyState();

        // Verify 4 requests were sent:
        //  - AppOpen
        //  - setUserId
        //  - registerDevice
        //  - getInApps
        Mockito.verify(pushwooshRepositoryMock, Mockito.timeout(2000).times(1)).sendAppOpen();

        Mockito.verify(pushwooshInAppMock, Mockito.timeout(2000).times(1)).setUserId(Mockito.any());

        Mockito.verify(deviceRegistrarMock, Mockito.timeout(2000).times(1)).updateRegistration();

        Mockito.verify(pushwooshInAppMock, Mockito.timeout(2000).times(1)).checkForUpdates();
    }

    /**
     * Verifies that DeviceBootedEvent triggers rescheduling of local notifications.
     * Tests device reboot handling to maintain notification functionality.
     */
    @Test
    public void testDeviceBootedEventHandling() throws InterruptedException {

        ensureInitializingState();

        initializeAndRunPushwooshStartWorker();

        ensureStarted();
        ensureReadyState();

        // When: Device boots (simulate boot completed event)
        EventBus.sendEvent(new BootReceiver.DeviceBootedEvent());

        // Verify notification manager reschedule was called
        Mockito.verify(notificationManagerMock, Mockito.times(1)).rescheduleLocalNotifications();
    }

    /**
     * Verifies that AppIdChangedEvent triggers device re-registration.
     * Tests handling of application configuration changes.
     */
    @Test
    public void testAppIdChangedEventHandling() throws InterruptedException {

        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();

        EventBus.sendEvent(new AppIdChangedEvent("new-app-id", "old-app-id"));

        // Verify notification manager reschedule was called
        Mockito.verify(deviceRegistrarMock, Mockito.times(1)).updateRegistration();
    }

    /**
     * Verifies that each plugin returned by Config.getPlugins() has init() invoked during SDK initialization.
     * Tests the initPlugins() branch of the initialization flow.
     */
    @Test
    public void testInitPluginsInvokesEachPlugin() throws InterruptedException {
        when(configMock.getPlugins()).thenReturn(Arrays.asList(pluginMock1, pluginMock2));

        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();

        Mockito.verify(pluginMock1, Mockito.times(1)).init();
        Mockito.verify(pluginMock2, Mockito.times(1)).init();
    }

    /**
     * Verifies that when reverse proxy is enabled, SDK stays in INITIALIZING state
     * until ReverseProxyReadyEvent is dispatched, then transitions to READY.
     */
    @Test
    public void testReverseProxyDelaysReadyUntilEventArrives() throws InterruptedException {
        when(configMock.isReverseProxyAllowed()).thenReturn(true);

        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();

        // wait briefly — SDK should NOT become ready without ReverseProxyReadyEvent
        Thread.sleep(500);
        ShadowLooper.idleMainLooper();

        assertEquals(
                "SDK must stay INITIALIZING until reverse proxy is configured",
                SdkStateProvider.SdkState.INITIALIZING,
                SdkStateProvider.getInstance().getCurrentState());
        assertFalse(
                "SDK must not be ready before reverse proxy event",
                SdkStateProvider.getInstance().isReady());

        // dispatch the missing event — initialization should now complete
        EventBus.sendEvent(new ReverseProxyReadyEvent());

        ensureStarted();
        ensureReadyState();
    }

    /**
     * Verifies that ServerCommunicationStartedEvent does NOT overwrite a pre-existing userId.
     * Tests the negative branch of fixDefaultUserId() — when userId is already set, setUserId is not called.
     */
    @Test
    public void testFixDefaultUserIdSkipsWhenUserIdAlreadySet() throws InterruptedException {
        when(registrationPrefsMock.userId().get()).thenReturn("existing-user-id");

        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();

        // dispatch ONLY ServerCommunicationStartedEvent (not ApplicationOpenEvent)
        EventBus.sendEvent(new ServerCommunicationStartedEvent());
        ShadowLooper.idleMainLooper();

        // setUserId should NEVER be called because userId is non-empty
        Mockito.verify(pushwooshInAppMock, Mockito.never()).setUserId(Mockito.anyString());
    }

    /**
     * Verifies that when a plugin provides the default push registrar
     * (pushRegistrarHelper.initDefaultPushRegistrarInPlugin() returns true),
     * notificationManager.initPushRegistrar() is NOT called, but initialize() still is.
     */
    @Test
    public void testInitPushRegistrarSkippedWhenPluginProvidesIt() throws InterruptedException {
        when(pushRegistrarHelperMock.initDefaultPushRegistrarInPlugin()).thenReturn(true);

        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();

        Mockito.verify(notificationManagerMock, Mockito.never()).initPushRegistrar();
        Mockito.verify(notificationManagerMock, Mockito.times(1)).initialize();
    }

    // Pair of the test above: when no plugin provides the registrar, notificationManager.initPushRegistrar()
    // must be invoked. Without explicit verify, mutating the `!helper.initDefaultPushRegistrarInPlugin()` guard
    // or removing the initPushRegistrar() call survives — default-mock coverage is not enough.
    @Test
    public void testInitPushRegistrarCalledWhenPluginDoesNotProvideIt() throws InterruptedException {
        when(pushRegistrarHelperMock.initDefaultPushRegistrarInPlugin()).thenReturn(false);

        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();

        Mockito.verify(notificationManagerMock, Mockito.times(1)).initPushRegistrar();
        Mockito.verify(notificationManagerMock, Mockito.times(1)).initialize();
    }

    // Verifies that initialize() exits early when SDK is already READY without re-running setup.
    @Test
    public void testInitializeNoOpWhenAlreadyReady() throws InterruptedException {
        SdkStateProvider.getInstance().setReady();

        pushwooshStartWorker.onApplicationCreated();
        Thread.sleep(200);

        Mockito.verify(pushwooshDefaultEventsMock, Mockito.never()).init();
        Mockito.verify(notificationManagerMock, Mockito.never()).initialize();
        Mockito.verify(notificationManagerMock, Mockito.never()).initPushRegistrar();
    }

    // Verifies that ServerCommunicationStartedEvent triggers fixDefaultUserId via setUserId(hwid) when userId is empty.
    @Test
    public void testServerCommunicationStartedTriggersFixDefaultUserId() throws InterruptedException {
        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();

        Mockito.reset(pushwooshInAppMock);

        EventBus.sendEvent(new ServerCommunicationStartedEvent());
        ShadowLooper.idleMainLooper();

        Mockito.verify(pushwooshInAppMock, Mockito.times(1)).setUserId("test-hwid-12345");
    }

    // Verifies that fetched HWID is stored in preferences via preferences.hwid().set(value).
    @Test
    public void testFetchedHwidStoredInPreferences() throws InterruptedException {
        DeviceUuidGetter customGetter = callback -> callback.onGetHwid("custom-hwid-value");

        pushwooshStartWorker = new PushwooshStartWorker(
                configMock,
                registrationPrefsMock,
                pushwooshRepositoryMock,
                notificationManagerMock,
                pushwooshInAppMock,
                deviceRegistrarMock,
                pushwooshDefaultEventsMock,
                pushRegistrarHelperMock,
                customGetter);

        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();

        Mockito.verify(registrationPrefsMock.hwid(), Mockito.atLeastOnce()).set("custom-hwid-value");
    }

    // Table-driven coverage of prettyApiToken: null passthrough, length-6 boundary (as-is),
    // length-7 boundary (just above threshold — obfuscation kicks in), long token (typical obfuscation).
    @Test
    public void prettyApiToken_parameterized() {
        String[][] cases = {
            {null, null},
            {"abcdef", "abcdef"},
            {"abcdefg", "abcd......bcdefg"},
            {"12345abcdefghij67890", "1234......j67890"},
        };
        for (String[] c : cases) {
            String input = c[0];
            String expected = c[1];
            String actual = pushwooshStartWorker.prettyApiToken(input);
            assertEquals("for input=" + input, expected, actual);
        }
    }
}
