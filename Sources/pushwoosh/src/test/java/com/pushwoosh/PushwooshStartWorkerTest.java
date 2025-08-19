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
import com.pushwoosh.internal.PushRegistrarHelper;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.AppIdChangedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.internal.platform.utils.DeviceUuidGetter;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.UUIDFactory;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class PushwooshStartWorkerTest {
    private PushwooshStartWorker pushwooshStartWorker;

    // Mocked dependencies
    @Mock(answer = RETURNS_DEEP_STUBS) private Config configMock;
    @Mock(answer = RETURNS_DEEP_STUBS) private RegistrationPrefs registrationPrefsMock;
    @Mock private PushwooshRepository pushwooshRepositoryMock;
    @Mock private PushwooshNotificationManager notificationManagerMock;
    @Mock private PushwooshInAppImpl pushwooshInAppMock;
    @Mock private DeviceRegistrar deviceRegistrarMock;
    @Mock private PushwooshDefaultEvents pushwooshDefaultEventsMock;
    @Mock private PushRegistrarHelper pushRegistrarHelperMock;
    @Mock private RequestManager requestManagerMock;

    private void setupConfig() {
        when(configMock.getLogLevel()).thenReturn("NOISE");
    }

    private void setupAndroidPlatformModuleWithFakeContext() {
        Context ctx = spy(RuntimeEnvironment.application);
        when(ctx.getApplicationContext()).thenReturn(ctx);
        AndroidPlatformModule.init(ctx, true);
    }

    private void setupRepository(){
        RepositoryModule.init(configMock, new UUIDFactory(), deviceRegistrarMock);
    }

    private void configureFakePreferences() {
        when(registrationPrefsMock.hwid().get()).thenReturn("test-hwid-12345");
        when(registrationPrefsMock.applicationId().get()).thenReturn("test-app-id");
        when(registrationPrefsMock.userId().get()).thenReturn("");
        when(registrationPrefsMock.apiToken().get()).thenReturn("test-api-token");
        when(registrationPrefsMock.baseUrl().get()).thenReturn("https://test.api.pushwoosh.com");
        when(registrationPrefsMock.projectId().get()).thenReturn("test-project-id");
        when(registrationPrefsMock.pushToken().get()).thenReturn("test-push-token");
    }

    @Before
    public void setUp() throws Exception {
        // logs
        ShadowLog.stream = System.out;
        MockitoAnnotations.openMocks(this);

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
                testGetter
        );

        // Reset SDK state for clean test
        SdkStateProvider.getInstance().resetForTesting();
    }

    @After
    public void tearDown() throws Exception {
        pushwooshStartWorker.shutdown();
        SdkStateProvider.getInstance().resetForTesting();
    }

    private void initializeAndRunPushwooshStartWorker() {
        // Initialize
        pushwooshStartWorker.onApplicationCreated();

        // Simulate AppIdReadyEvent from NotificationManager
        EventBus.sendEvent(new PushwooshNotificationManager.ApplicationIdReadyEvent());
    }

    private void ensureInitializingState() {
        // Verify initial state
        assertEquals(SdkStateProvider.SdkState.INITIALIZING, SdkStateProvider.getInstance().getCurrentState());
        assertFalse(SdkStateProvider.getInstance().isReady());
    }

    private void ensureReadyState() throws InterruptedException {
        // Verify SDK state
        assertEquals(SdkStateProvider.SdkState.READY, SdkStateProvider.getInstance().getCurrentState());
        assertTrue(SdkStateProvider.getInstance().isReady());
    }

    private void ensureStarted() throws InterruptedException {
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

        Mockito.verify(
                pushwooshDefaultEventsMock,
                Mockito.times(1)
        ).init();

        // Verify no any calls to request manager
        Mockito.verify(
                requestManagerMock,
                Mockito.never()
        ).sendRequest(Mockito.any(), Mockito.any());
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
                failingGetter
        );

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
        assertEquals("Should stay INITIALIZING when AppId event missing",
                SdkStateProvider.SdkState.INITIALIZING,
                SdkStateProvider.getInstance().getCurrentState());
        assertFalse("Should not be ready", SdkStateProvider.getInstance().isReady());

        // tasks should be queued, not executed
        CountDownLatch queuedTaskLatch = new CountDownLatch(1);
        SdkStateProvider.getInstance().executeOrQueue(queuedTaskLatch::countDown);

        // task should timeout - not execute because SDK never becomes ready
        assertFalse("Task should remain queued when initialization stucks",
                queuedTaskLatch.await(500, TimeUnit.MILLISECONDS));

        // verify that sending the missing event unblocks everything
        EventBus.sendEvent(new PushwooshNotificationManager.ApplicationIdReadyEvent());

        // now the queued task should execute
        assertTrue("Task should execute after missing event arrives",
                queuedTaskLatch.await(2, TimeUnit.SECONDS));

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
        assertEquals("Repeated initialization should be ignored", 
                readyState, SdkStateProvider.getInstance().getCurrentState());
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
        assertFalse("Task should be queued, not executed immediately",
                queuedTaskLatch.await(500, TimeUnit.MILLISECONDS));

        // Verify SDK is still INITIALIZING
        ensureInitializingState();

        initializeAndRunPushwooshStartWorker();

        // Now the queued task should execute when SDK becomes ready
        assertTrue("Queued task should execute after SDK initialization",
                queuedTaskLatch.await(2, TimeUnit.SECONDS));

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
        Mockito.verify(
                pushwooshRepositoryMock,
                Mockito.timeout(2000).times(1)
        ).sendAppOpen();

        Mockito.verify(
                pushwooshInAppMock,
                Mockito.timeout(2000).times(1)
        ).setUserId(Mockito.any());

        Mockito.verify(
                deviceRegistrarMock,
                Mockito.timeout(2000).times(1)
        ).updateRegistration();

        Mockito.verify(
                pushwooshInAppMock,
                Mockito.timeout(2000).times(1)
        ).checkForUpdates();

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
        Mockito.verify(
                notificationManagerMock,
                Mockito.times(1)
        ).rescheduleLocalNotifications();
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
        Mockito.verify(
                deviceRegistrarMock,
                Mockito.times(1)
        ).updateRegistration();
    }

    @Test
    public void testServerCommunicationStartedEventHandling() throws InterruptedException {
        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();

        // Simulate ApplicationOpenEvent
        EventBus.sendEvent(new ApplicationOpenDetector.ApplicationOpenEvent());
        ensureStarted();
        ensureReadyState();

        // If this logic will be removed from PushwooshStartWorker then remove this test
        EventBus.sendEvent(new ServerCommunicationStartedEvent());

        Mockito.verify(
                pushwooshInAppMock,
                Mockito.atLeastOnce()
        ).setUserId(registrationPrefsMock.hwid().get());

    }

    /**
     * Verifies that shutdown() correctly terminates the internal ExecutorService.
     * Tests graceful shutdown with timeout handling and repeated shutdown safety.
     */
    @Test
    public void testShutdown() throws InterruptedException {
        
        ensureInitializingState();
        initializeAndRunPushwooshStartWorker();
        ensureStarted();
        ensureReadyState();
        
        // shutdown should complete without exceptions
        pushwooshStartWorker.shutdown();
        
        // repeated shutdown calls should be safe and not throw exceptions
        pushwooshStartWorker.shutdown();
        pushwooshStartWorker.shutdown();
        
        // verify that we can still create new worker after shutdown (no lingering state)
        PushwooshStartWorker newWorker = new PushwooshStartWorker(
                configMock,
                registrationPrefsMock,
                pushwooshRepositoryMock,
                notificationManagerMock,
                pushwooshInAppMock,
                deviceRegistrarMock,
                pushwooshDefaultEventsMock,
                pushRegistrarHelperMock,
                callback -> callback.onGetHwid("test-hwid-new")
        );
        
        // new worker should initialize normally
        newWorker.onApplicationCreated();
        EventBus.sendEvent(new PushwooshNotificationManager.ApplicationIdReadyEvent());
        
        // cleanup new worker
        newWorker.shutdown();
    }
}
