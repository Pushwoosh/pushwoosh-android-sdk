package com.pushwoosh;

import android.text.TextUtils;

import com.pushwoosh.appevents.PushwooshDefaultEvents;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.PushRegistrarHelper;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.AppIdChangedEvent;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.event.InitHwidEvent;
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.internal.platform.utils.DeviceUuidGetter;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates the SDK initialization process.
 * <p>
 * This class coordinates the asynchronous fetching of critical data (HWID, AppCode)
 * using a {@link CountDownLatch} to ensure that dependent business logic runs only
 * after a successful initialization. It uses an {@link ExecutorService} to manage
 * background tasks safely.
 */
public class PushwooshStartWorker {
    public static final String TAG = PushwooshStartWorker.class.getSimpleName();
    private final Config config;
    private final RegistrationPrefs preferences;
    private final PushwooshRepository pushwooshRepository;
    private final PushwooshNotificationManager notificationManager;
    private final PushwooshInAppImpl pushwooshInApp;
    private final DeviceRegistrar deviceRegistrar;
    private final PushwooshDefaultEvents pushwooshDefaultEvents;
    private final PushRegistrarHelper pushRegistrarHelper;
    private final DeviceUuidGetter deviceUuidGetter;
    public PushwooshStartWorker(
        Config config,
        RegistrationPrefs registrationPrefs,
        PushwooshRepository pushwooshRepository,
        PushwooshNotificationManager notificationManager,
        PushwooshInAppImpl pushwooshInApp,
        DeviceRegistrar deviceRegistrar,
        PushwooshDefaultEvents pushwooshDefaultEvents,
        PushRegistrarHelper pushRegistrarHelper,
        DeviceUuidGetter deviceUuidGetter) {

        this.config = config;
        this.preferences = registrationPrefs;
        this.pushwooshRepository = pushwooshRepository;
        this.notificationManager = notificationManager;
        this.pushwooshInApp = pushwooshInApp;
        this.deviceRegistrar = deviceRegistrar;
        this.pushwooshDefaultEvents = pushwooshDefaultEvents;
        this.pushRegistrarHelper = pushRegistrarHelper;
        this.deviceUuidGetter = deviceUuidGetter;
    }

    private final ExecutorService sdkExecutor = Executors.newSingleThreadExecutor();

    /**
     * Entry point for the SDK initialization process.
     * Called from {@link PushwooshPlatform} on application startup.
     *
     * @deprecated This method is deprecated and will be removed in a future version.
     *             The new initialization approach uses improved async handling.
     *             Use the updated initialization flow instead.
     */
    @Deprecated
    public void onApplicationCreated() {
        initialize();
    }

    /**
     * Initializes the Pushwoosh SDK asynchronously.
     * <p>
     * This method coordinates the asynchronous fetching of critical data (HWID, AppCode)
     * using a {@link CountDownLatch} to ensure that dependent business logic runs only
     * after successful initialization. The process includes:
     * <ul>
     *   <li>Subscribing to SDK events</li>
     *   <li>Fetching device HWID asynchronously</li>
     *   <li>Fetching application code asynchronously</li>
     *   <li>Starting the SDK when both operations complete</li>
     *   <li>Initializing notification manager and plugins</li>
     * </ul>
     * <p>
     * If initialization fails, the SDK state will be set to error.
     * This method can only be called once - subsequent calls will be ignored.
     */
    private void initialize() {
        PWLog.noise(TAG, "initialize()");

        // cant initialize twice
        if (SdkStateProvider.getInstance().isReady()) {
            PWLog.warn(TAG, "SDK already initialized");
            return;
        }

        // subscribe for pushwoosh internal events
        subscribeForSdkEvents();

        // run async 2 task: hwid and app_code
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicBoolean hasFailed = new AtomicBoolean(false);
        final long initializeStartTime = System.currentTimeMillis();

        fetchDeviceHwidAsync(latch, hasFailed);
        fetchAppCodeAsync(latch, hasFailed);

        // when tasks is both ready => start sdk finally
        sdkExecutor.submit(() -> {
            try {
                PWLog.debug(TAG, "Waiter task is waiting for latch...");
                latch.await();
                PWLog.debug(TAG, "Latch released. Checking for failures...");

                if (hasFailed.get()) {
                    PWLog.error(TAG, "can't initialize sdk, because of failed operations");
                    SdkStateProvider.getInstance().setError();
                } else {
                    try {
                        SdkStateProvider.getInstance().setReady();
                        long initializeDuration = System.currentTimeMillis() - initializeStartTime;
                        PWLog.info(TAG, "Pushwoosh SDK started successfully, duration: " + initializeDuration + "ms");
                        start();
                    } catch (Throwable e) {
                        PWLog.error(TAG, "can't start sdk", e);
                        SdkStateProvider.getInstance().setError();
                    }
                }

            } catch (InterruptedException e) {
                PWLog.error(TAG, "can't initialize sdk", e);
                SdkStateProvider.getInstance().setError();
                Thread.currentThread().interrupt();
            }
        });

        if (!pushRegistrarHelper.initDefaultPushRegistrarInPlugin()) {
            notificationManager.initPushRegistrar();
        }

        notificationManager.initialize();

        initPlugins();
    }

    /**
     * Subscribes to various SDK events that need to be handled during the application lifecycle.
     * <p>
     * This method sets up event listeners for:
     * <ul>
     *   <li>{@link ApplicationOpenDetector.ApplicationOpenEvent} - when user opens the application</li>
     *   <li>{@link BootReceiver.DeviceBootedEvent} - when device boots up</li>
     * </ul>
     * <p>
     * It also initializes default Pushwoosh events handling.
     */
    private void subscribeForSdkEvents() {
        // attach ApplicationOpenEvent logic (when user wakeup application this sdk)
        EventBus.subscribe(
                ApplicationOpenDetector.ApplicationOpenEvent.class,
                event -> {
                    PWLog.noise(TAG, "onApplicationOpenEvent()");
                    SdkStateProvider.getInstance().executeOrQueue(() -> {
                        new AppOpenHandler().handle();
                    });
                }
        );

        // attach pushwoosh defaults events
        pushwooshDefaultEvents.init();

        //attach DeviceBootedEvent logic
        EventBus.subscribe(
                BootReceiver.DeviceBootedEvent.class,
                event -> {
                    PWLog.noise(TAG, "onDeviceBootedEvent()");
                    SdkStateProvider.getInstance().executeOrQueue(notificationManager::rescheduleLocalNotifications);
                }
        );

        //attach AppIdChangedEvent logic (sdk or user call setAppId() with new application code)
        EventBus.subscribe(
                AppIdChangedEvent.class,
                event -> {
                    PWLog.noise(TAG, "onAppIdChangedEvent()");
                    SdkStateProvider.getInstance().executeOrQueue(() -> {
                        PWLog.info(TAG, "AppCode was changed from " + event.getOldAppId() + " to " + event.getNewAppId() + ". Updating registration.");
                        deviceRegistrar.updateRegistration();
                    });
                }
        );

        // attach ServerCommunicationStartedEvent
        EventBus.subscribe(
                ServerCommunicationStartedEvent.class,
                event -> {
                   PWLog.noise(TAG, "onServerCommunicationStartedEvent()");
                   SdkStateProvider.getInstance().executeOrQueue(this::fixDefaultUserId);
                }

        );
    }
    /**
     * Asynchronously fetches the device Hardware ID (HWID).
     * <p>
     * This method retrieves the unique device identifier and:
     * <ul>
     *   <li>Stores it in memory and preferences</li>
     *   <li>Sends an {@link InitHwidEvent}</li>
     *   <li>Decrements the initialization latch</li>
     * </ul>
     * <p>
     * If fetching fails, sets the failure flag and decrements the latch.
     *
     * @param latch the countdown latch to signal completion
     * @param hasFailed atomic boolean to signal if operation failed
     */
    private void fetchDeviceHwidAsync(CountDownLatch latch, AtomicBoolean hasFailed) {
        PWLog.noise(TAG, "fetchDeviceHwidAsync()");
        try {
            deviceUuidGetter.getDeviceUUID(value -> {
                PWLog.debug(TAG, "fetched device hwid: " + value);
                try {
                    preferences.hwid().set(value);
                    EventBus.sendEvent(new InitHwidEvent(value));
                } catch (Throwable e) {
                    PWLog.error(TAG, "can't store device hwid", e);
                    hasFailed.set(true);
                }
                latch.countDown();
            });
        } catch (Throwable e) {
            PWLog.error(TAG, "can't fetchDeviceHwid", e);

            hasFailed.set(true);
            latch.countDown();
        }
    }

    /**
     * Asynchronously fetches the application code.
     * <p>
     * This method subscribes to {@link PushwooshNotificationManager.ApplicationIdReadyEvent}
     * to receive the application code when it becomes available. Upon receiving the event:
     * <ul>
     *   <li>Logs the fetched application code</li>
     *   <li>Unsubscribes from the event</li>
     *   <li>Decrements the initialization latch</li>
     * </ul>
     * <p>
     * If subscription fails, sets the failure flag and decrements the latch.
     *
     * @param latch the countdown latch to signal completion
     * @param hasFailed atomic boolean to signal if operation failed
     */
    private void fetchAppCodeAsync(CountDownLatch latch, AtomicBoolean hasFailed) {
        PWLog.noise(TAG, "fetchAppCodeAsync()");
        try {
            EventBus.subscribe(
                    PushwooshNotificationManager.ApplicationIdReadyEvent.class,
                    new EventListener<PushwooshNotificationManager.ApplicationIdReadyEvent>() {
                        @Override
                        public void onReceive(PushwooshNotificationManager.ApplicationIdReadyEvent event) {
                            try {
                                String applicationCode = preferences.applicationId().get();
                                PWLog.debug(TAG, "fetched application code:" + applicationCode);
                            } catch (Throwable e) {
                                PWLog.error(TAG, "can't fetch application code", e);
                                hasFailed.set(true);
                            }
                            EventBus.unsubscribe(
                                    PushwooshNotificationManager.ApplicationIdReadyEvent.class,
                                    this
                            );

                            latch.countDown();
                        }
                    }
            );
        } catch (Throwable e) {
            PWLog.error(TAG, "can't subscribe to fetch app code", e);
            hasFailed.set(true);
            latch.countDown();
        }
    }

    /**
     * Starts the SDK after successful initialization.
     * <p>
     * This method is called when both HWID and application code have been fetched successfully.
     * Currently, it only prints the initialization message with SDK status and configuration details.
     */
    private void start() {
        PWLog.noise(TAG, "start()");
        try {
            printInitializingMessage();
        } catch (Throwable e) {
            PWLog.error(TAG, "an error occurred during start", e);
        }
    }

    /**
     * Prints detailed SDK initialization status and configuration information.
     * <p>
     * Logs the following information:
     * <ul>
     *   <li>SDK status and version</li>
     *   <li>Base URL configuration</li>
     *   <li>Application code and Firebase project ID</li>
     *   <li>Device HWID and push token</li>
     *   <li>Obfuscated API token</li>
     * </ul>
     */
    private void printInitializingMessage() {
        PWLog.noise(TAG, "printInitializingMessage()");

        String sdkVersion = GeneralUtils.SDK_VERSION;
        String deviceHwid = preferences.hwid().get();
        String applicationCode = preferences.applicationId().get();
        String fcmProjectId = preferences.projectId().get();
        String apiToken = prettyApiToken(preferences.apiToken().get());
        String pushToken = preferences.pushToken().get();
        String baseURL = preferences.baseUrl().get();

        PWLog.info(TAG, "PUSHWOOSH SDK STATUS: " + SdkStateProvider.getInstance().getCurrentState());
        PWLog.info(TAG, "PUSHWOOSH SDK VERSION: " + sdkVersion);
        PWLog.info(TAG, "PUSHWOOSH BASE URL: " + baseURL);

        PWLog.info(TAG, "APP CODE: " + applicationCode);
        PWLog.info(TAG, "FIREBASE PROJECT ID: " + fcmProjectId);
        PWLog.info(TAG, "HWID: " + deviceHwid);
        PWLog.info(TAG, "PUSH TOKEN: " + pushToken);

        PWLog.info(TAG, "API TOKEN: " + apiToken);
    }

    /**
     * Creates a pretty-printed version of the API token by obfuscating the middle part.
     * <p>
     * Shows only the first 4 characters and last 6 characters of the token,
     * replacing the middle with dots for security purposes.
     *
     * @param value the original API token
     * @return obfuscated token string, or original value if too short or null
     */
    private String prettyApiToken(String value) {
        if (value != null && value.length() > 6) {
            return value.substring(0, 4) + "......" + value.substring(value.length() - 6);
        }
        return value;
    }

    private void fixDefaultUserId() {
        PWLog.noise(TAG, "fixDefaultUserId()");
        String userId = preferences.userId().get();
        if (TextUtils.isEmpty(userId)) {
            String deviceHwid = preferences.hwid().get();
            pushwooshInApp.setUserId(deviceHwid);
        }
    }

    private class AppOpenHandler {
        /**
         * Handles all operations that should be performed when the application is opened.
         */
        private void handle() {
            fixDefaultUserId();
            sendAppOpenEvent();
            updateDeviceRegistration();
            loadInApps();
        }

        /**
         * Sends an application open event to the Pushwoosh server.
         * <p>
         * This helps track application usage statistics and user engagement.
         */
        private void sendAppOpenEvent() {
            PWLog.noise(TAG, "sendAppOpenEvent()");
            pushwooshRepository.sendAppOpen();
        }

        /**
         * Triggers device registration update.
         * <p>
         * Updates the device registration with the server, ensuring that
         * any changes in device state or configuration are synchronized.
         */
        private void updateDeviceRegistration() {
            PWLog.noise(TAG, "registerDevice()");
            deviceRegistrar.updateRegistration();
        }

        /**
         * Initiates loading of in-app messages.
         * <p>
         * Checks for any new in-app messages that should be displayed to the user
         * and updates the local cache accordingly.
         */
        private void loadInApps() {
            PWLog.noise(TAG, "loadInApps()");
            pushwooshInApp.checkForUpdates();
        }

    }

    /**
     * Initializes plugins.
     */
    private void initPlugins() {
        for (Plugin plugin : config.getPlugins()) {
            plugin.init();
        }
    }

    /**
     * Shuts down the internal {@link ExecutorService} gracefully.
     * Should be called if the SDK needs to be completely destroyed.
     */
    public void shutdown() {
        PWLog.noise(TAG, "shutdown()");

        if (sdkExecutor != null && !sdkExecutor.isShutdown()) {
            PWLog.debug(TAG, "shutting down SDK ExecutorService.");
            sdkExecutor.shutdown();

            try {
                if (!sdkExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    PWLog.warn(TAG, "ExecutorService didn't terminate gracefully, forcing shutdown");
                    sdkExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                PWLog.error(TAG, "interrupted during shutdown", e);
                sdkExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
