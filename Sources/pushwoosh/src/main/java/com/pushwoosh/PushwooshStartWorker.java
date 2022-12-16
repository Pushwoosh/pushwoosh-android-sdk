package com.pushwoosh;

import android.text.TextUtils;
import android.util.Pair;

import com.pushwoosh.appevents.PushwooshDefaultEvents;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.PushRegistrarHelper;
import com.pushwoosh.internal.event.ConfigLoadedEvent;
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
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.channel.NotificationChannelManager;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.HWIDMigration;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PushwooshStartWorker {
    public static final String TAG = PushwooshStartWorker.class.getSimpleName();

    private final AtomicBoolean appOpen = new AtomicBoolean(false);
    private final AtomicBoolean appReady = new AtomicBoolean(false);
    private final AtomicReference<String> hwid = new AtomicReference<>("");
    private final AtomicReference<String> oldHwid = new AtomicReference<>("");

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final Config config;
    private final RegistrationPrefs registrationPrefs;
    private final HWIDMigration HWIDMigration;
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
                                HWIDMigration HWIDMigration,
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
        this.HWIDMigration = HWIDMigration;
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
                Emitter.forEvent(InitHwidEvent.class)).bind(event -> onStart());

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
        PWLog.debug("initHwid");
        DeviceUtils.getDeviceUUID(hwid -> onGetHwid(hwid, subscriberAppOpen, subscriberAppReady));
    }

    private void onGetHwid(String hwidString,
                           Subscription<ApplicationOpenDetector.ApplicationOpenEvent> subscriberAppOpen,
                           Subscription<PushwooshNotificationManager.ApplicationIdReadyEvent> subscriberAppReady) {
        oldHwid.set(registrationPrefs.hwid().get());
        hwid.set(hwidString);
        registrationPrefs.hwid().set(hwid.get());

        EventBus.sendEvent(new InitHwidEvent(hwid.get()));
        Pair<String, String> hwids = new Pair<>(hwid.get(), oldHwid.get());
        appOpenEndTagMigrateIfReady(hwids);
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

    private void appOpenEndTagMigrateIfReady(Pair<String, String> hwids) {
        if (appOpen.get()) {
            appVersionProvider.handleLaunch();
            if (appReady.get()) {
                HWIDMigration.executeMigration(hwids.first, hwids.second);
                pushwooshRepository.sendAppOpen();
            }
        }
    }

    private void onStart() {
        if (started.compareAndSet(false, true)) {
            pushwooshRepository.loadConfig();
            deviceRegistrar.updateRegistration();
            pushwooshInApp.checkForUpdates();
        }
    }

    private void initPlugins() {
        // Mandatory log
        android.util.Log.i("Pushwoosh", "HWID: " + registrationPrefs.hwid().get());

        PWLog.debug("PushwooshModule", "onApplicationCreated");
        PWLog.info(TAG, String.format("This is %s device", DeviceSpecificProvider.getInstance().type()));

        for (Plugin plugin : config.getPlugins()) {
            plugin.init();
        }
    }

    private void subscribeForEvent() {
        EventBus.subscribe(ApplicationOpenDetector.ApplicationOpenEvent.class, event -> onAppOpen());
        PWLog.debug("appOpen:"+appOpen.get()+" onAppReady:"+appReady.get());
        if (appOpen.get()) {
            if (appReady.get()) {
                sendAppOpenEndTagMigrate();
                registerUserIdWhenAppReady();
            }
            EventBus.subscribe(PushwooshNotificationManager.ApplicationIdReadyEvent.class, event -> {
                sendAppOpenEndTagMigrate();
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
        PWLog.debug("onAppOpen");
        appVersionProvider.handleLaunch();
        appOpen.set(true);
        if (appReady.get()) {
            sendAppOpenEndTagMigrate();
            registerUserIdWhenAppReady();
        }
    }

    private void onAppReady() {
        PWLog.debug("onAppReady");
        if (appOpen.get()) {
            sendAppOpenEndTagMigrate();
            registerUserIdWhenAppReady();
        }
    }

    private void sendAppOpenEndTagMigrate() {
        PWLog.debug("sendAppOpenEndTagMigrate");
        if (!hwid.get().isEmpty()) {
            HWIDMigration.executeMigration(hwid.get(), oldHwid.get());
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
