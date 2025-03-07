package com.pushwoosh;

import android.text.TextUtils;

import com.pushwoosh.appevents.PushwooshDefaultEvents;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.PushRegistrarHelper;
import com.pushwoosh.internal.event.Emitter;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.event.InitHwidEvent;
import com.pushwoosh.internal.event.ServerCommunicationStartedEvent;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.platform.ApplicationOpenDetector;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.channel.NotificationChannelManager;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PushwooshStartWorker {
    public static final String TAG = PushwooshStartWorker.class.getSimpleName();
    public static final String INIT_TAG = "Pushwoosh";

    private final AtomicBoolean appOpen = new AtomicBoolean(false);
    private final AtomicBoolean appReady = new AtomicBoolean(false);
    private final AtomicReference<String> hwid = new AtomicReference<>("");

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final Config config;
    private final RegistrationPrefs registrationPrefs;
    private final AppVersionProvider appVersionProvider;
    private final PushwooshRepository pushwooshRepository;
    private final PushwooshNotificationManager notificationManager;
    private final PushwooshInAppImpl pushwooshInApp;
    private final DeviceRegistrar deviceRegistrar;
    private final PushwooshDefaultEvents pushwooshDefaultEvents;
    private final PushRegistrarHelper pushRegistrarHelper;
    private final ServerCommunicationManager serverCommunicationManager;

    private EventListener<ServerCommunicationStartedEvent> setUserIdWhenServerCommunicationStartsEvent;

    public PushwooshStartWorker(Config config,
                                RegistrationPrefs registrationPrefs,
                                AppVersionProvider appVersionProvider,
                                PushwooshRepository pushwooshRepository,
                                PushwooshNotificationManager notificationManager,
                                PushwooshInAppImpl pushwooshInApp,
                                DeviceRegistrar deviceRegistrar,
                                PushwooshDefaultEvents pushwooshDefaultEvents,
                                PushRegistrarHelper pushRegistrarHelper,
                                ServerCommunicationManager serverCommunicationManager) {
        this.config = config;
        this.registrationPrefs = registrationPrefs;
        this.appVersionProvider = appVersionProvider;
        this.pushwooshRepository = pushwooshRepository;
        this.notificationManager = notificationManager;
        this.pushwooshInApp = pushwooshInApp;
        this.deviceRegistrar = deviceRegistrar;
        this.pushwooshDefaultEvents = pushwooshDefaultEvents;
        this.pushRegistrarHelper = pushRegistrarHelper;
        this.serverCommunicationManager = serverCommunicationManager;
    }

    public void onApplicationCreated() {
        PWLog.init();
        Subscription<ApplicationOpenDetector.ApplicationOpenEvent> subscriberAppOpen =
                EventBus.subscribe(ApplicationOpenDetector.ApplicationOpenEvent.class, event -> appOpen.set(true));
        Subscription<PushwooshNotificationManager.ApplicationIdReadyEvent> subscriberAppReady =
                EventBus.subscribe(PushwooshNotificationManager.ApplicationIdReadyEvent.class, event -> appReady.set(true));
        Emitter.when(Emitter.forEvent(PushwooshNotificationManager.ApplicationIdReadyEvent.class),
                Emitter.forEvent(InitHwidEvent.class)).bind(event -> onApplicationOpenAndHwidReady());

        if (!pushRegistrarHelper.initDefaultPushRegistrarInPlugin()) {
            notificationManager.initPushRegistrar();
        }

        notificationManager.initialize();

        initHwid(subscriberAppOpen, subscriberAppReady);
        initPlugins();
        migrateGroupChannel();
        pushwooshDefaultEvents.init();
    }

    private void initHwid(Subscription<ApplicationOpenDetector.ApplicationOpenEvent> subscriberAppOpen,
                          Subscription<PushwooshNotificationManager.ApplicationIdReadyEvent> subscriberAppReady) {
        DeviceUtils.getDeviceUUID(hwid -> onGetHwid(hwid, subscriberAppOpen, subscriberAppReady));
    }

    private void onGetHwid(String hwidString,
                           Subscription<ApplicationOpenDetector.ApplicationOpenEvent> subscriberAppOpen,
                           Subscription<PushwooshNotificationManager.ApplicationIdReadyEvent> subscriberAppReady) {
        hwid.set(hwidString);
        registrationPrefs.hwid().set(hwid.get());

        EventBus.sendEvent(new InitHwidEvent(hwid.get()));
        sendAppOpenIfReady();
        subscribeForEvent();

        subscriberAppOpen.unsubscribe();
        subscriberAppReady.unsubscribe();
    }

    private void setUserIdIfEmpty() {
        String userId = registrationPrefs.userId().get();
        if (TextUtils.isEmpty(userId)) {
            userId = registrationPrefs.hwid().get();
            pushwooshInApp.setUserId(userId);
        }
    }

    private void subscribeSetUserIdWhenServerCommunicationStartsEvent() {
        if (setUserIdWhenServerCommunicationStartsEvent != null) {
            return;
        }
        setUserIdWhenServerCommunicationStartsEvent = new EventListener<ServerCommunicationStartedEvent>() {
            @Override
            public void onReceive(ServerCommunicationStartedEvent event) {
                EventBus.unsubscribe(ServerCommunicationStartedEvent.class, this);
                setUserIdIfEmpty();
            }
        };
        EventBus.subscribe(ServerCommunicationStartedEvent.class, setUserIdWhenServerCommunicationStartsEvent);
    }

    private void sendAppOpenIfReady() {
        if (appOpen.get()) {
            appVersionProvider.handleLaunch();
            if (appReady.get()) {
                pushwooshRepository.sendAppOpen();
            }
        }
    }

    private void onApplicationOpenAndHwidReady() {
        // Mandatory log
        android.util.Log.i(INIT_TAG, "Pushwoosh SDK initialized successfully");
        android.util.Log.i(INIT_TAG, "HWID: " + registrationPrefs.hwid().get());
        android.util.Log.i(INIT_TAG, "APP_CODE: " + registrationPrefs.applicationId().get());
        android.util.Log.i(INIT_TAG, "PUSHWOOSH_SDK_VERSION: " + GeneralUtils.SDK_VERSION);
        android.util.Log.i(INIT_TAG, "FIREBASE_PROJECT_ID: " +         registrationPrefs.projectId().get());
        android.util.Log.i(INIT_TAG, "API_TOKEN: " + PushwooshPlatform.getInstance().getConfig().getApiToken());
        android.util.Log.i(INIT_TAG, "PUSH_TOKEN: " + registrationPrefs.pushToken().get());
        if (started.compareAndSet(false, true)) {
            EventBus.subscribe(ApplicationOpenDetector.ApplicationOpenEvent.class, event -> {
                deviceRegistrar.updateRegistration();
                pushwooshInApp.checkForUpdates();
            });
        }
    }

    private void initPlugins() {
        for (Plugin plugin : config.getPlugins()) {
            plugin.init();
        }
    }

    private void subscribeForEvent() {
        EventBus.subscribe(ApplicationOpenDetector.ApplicationOpenEvent.class, event -> onAppOpen());
        if (appOpen.get()) {
            if (appReady.get()) {
                sendAppOpenIfHwidReady();
                registerUserIdWhenAppReady();
            }
            EventBus.subscribe(PushwooshNotificationManager.ApplicationIdReadyEvent.class, event -> {
                sendAppOpenIfHwidReady();
                registerUserIdWhenAppReady();
            });
        } else {
            Emitter.when(Emitter.forEvent(ApplicationOpenDetector.ApplicationOpenEvent.class),
                    Emitter.forEvent(PushwooshNotificationManager.ApplicationIdReadyEvent.class))
                    .bind(event -> onAppReady());
        }
        EventBus.subscribe(BootReceiver.DeviceBootedEvent.class, event -> notificationManager.rescheduleLocalNotifications());
    }

    private void onAppOpen() {
        appVersionProvider.handleLaunch();
        appOpen.set(true);
        if (appReady.get()) {
            sendAppOpenIfHwidReady();
            registerUserIdWhenAppReady();
        }
    }

    private void onAppReady() {
        if (appOpen.get()) {
            sendAppOpenIfHwidReady();
            registerUserIdWhenAppReady();
        }
    }

    private void sendAppOpenIfHwidReady() {
        if (!hwid.get().isEmpty()) {
            pushwooshRepository.sendAppOpen();
        }
    }

    private void registerUserIdWhenAppReady() {
        if (serverCommunicationManager != null && !serverCommunicationManager.isServerCommunicationAllowed()) {
            subscribeSetUserIdWhenServerCommunicationStartsEvent();
        } else {
            setUserIdIfEmpty();
        }
    }

    private void migrateGroupChannel() {
        try {
            NotificationChannelManager channelManager = new NotificationChannelManager(AndroidPlatformModule.getApplicationContext());
            channelManager.migrateGroupChannel();
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to migrate group notifications channel" + e.getMessage());
        }
    }

    public void reset() {
        started.set(false);
    }
}
