package com.pushwoosh;

import android.app.Activity;

import com.pushwoosh.appevents.PushwooshDefaultEvents;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.inapp.PushwooshInAppServiceImpl;
import com.pushwoosh.inapp.businesscases.BusinessCasesManager;
import com.pushwoosh.inapp.view.strategy.ResourceViewStrategyFactory;
import com.pushwoosh.internal.PushRegistrarHelper;
import com.pushwoosh.internal.command.CommandApplayer;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.app.AppInfoProvider;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.TimeProvider;
import com.pushwoosh.internal.utils.UUIDFactory;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessageFactory;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.handlers.notification.PushStatNotificationOpenHandler;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.SendTagsProcessor;
import com.pushwoosh.richmedia.RichMediaController;
import com.pushwoosh.richmedia.RichMediaFactory;
import com.pushwoosh.richmedia.RichMediaStyle;
import com.pushwoosh.richmedia.animation.RichMediaAnimationSlideBottom;

public class PushwooshPlatform {
    private static boolean notified = false;
    private com.pushwoosh.internal.utils.UUIDFactory UUIDFactory;
    private PushMessageFactory pushMessageFactory;

    public static void notifyNotInitialized() {
        if (!notified) {
            PWLog.warn(TAG, "Pushwoosh library not initialized. All method calls will be ignored");
            notified = true;
        }
    }

    private static final String TAG = "PushwooshPlatform";

    private static PushwooshPlatform instance;

    private final Config config;
    private final PushwooshNotificationManager notificationManager;
    private final PushwooshRepository pushwooshRepository;
    private final RegistrationPrefs registrationPrefs;
    private final PushwooshInAppImpl pushwooshInApp;
    private final BusinessCasesManager businessCasesManager;
    private final ServerCommunicationManager serverCommunicationManager;
    private NotificationServiceExtension notificationServiceExtension;
    private GDPRManager gdprManager;
    private RichMediaController richMediaController;
    private AppVersionProvider appVersionProvider;
    private PushwooshStartWorker pushwooshStartWorker;
    private DeviceRegistrar deviceRegistrar;
    private RichMediaStyle richMediaStyle;
    private Activity topActivity;

    private CommandApplayer commandApplayer;
    private PushStatNotificationOpenHandler pushStatNotificationOpenHandler;
    private PushwooshDefaultEvents pushwooshDefaultEvents;
    private PushRegistrarHelper pushRegistrarHelper;
    
    public PushMessageFactory getPushMessageFactory() {
        return pushMessageFactory;
    }
    
    public PushStatNotificationOpenHandler pushStatNotificationOpenHandler() {
        return pushStatNotificationOpenHandler;
    }

    public static class Builder {
        private Config config;
        private PushRegistrar pushRegistrar;

        public Builder setConfig(Config config) {
            this.config = config;
            return this;
        }

        public Builder setPushRegistrar(PushRegistrar pushRegistrar) {
            this.pushRegistrar = pushRegistrar;
            return this;
        }

        public PushwooshPlatform build() {
            instance = new PushwooshPlatform(this);
            return instance;
        }
    }

    private PushwooshPlatform(Builder builder) {
        UUIDFactory = new UUIDFactory();
        config = builder.config;
        commandApplayer = new CommandApplayer();
        pushStatNotificationOpenHandler = new PushStatNotificationOpenHandler(commandApplayer);
        deviceRegistrar = new DeviceRegistrar();
        RepositoryModule.init(config, UUIDFactory, deviceRegistrar);
        registrationPrefs = RepositoryModule.getRegistrationPreferences();
        serverCommunicationManager = new ServerCommunicationManager();

        NetworkModule.init(registrationPrefs, serverCommunicationManager);

        notificationManager = new PushwooshNotificationManager(builder.pushRegistrar, config);
        pushwooshInApp = new PushwooshInAppImpl(new PushwooshInAppServiceImpl(), serverCommunicationManager);
        pushMessageFactory = new PushMessageFactory();

        PrefsProvider prefsProvider = AndroidPlatformModule.getPrefsProvider();
        AppInfoProvider appInfoProvider = AndroidPlatformModule.getAppInfoProvider();
        TimeProvider timeProvide = AndroidPlatformModule.getTimeProvide();
        appVersionProvider = new AppVersionProvider(AndroidPlatformModule.getPrefsProvider().providePrefs("PWAppVersion"));
        businessCasesManager = new BusinessCasesManager(prefsProvider, appInfoProvider, timeProvide, appVersionProvider);

        RequestManager requestManager = NetworkModule.getRequestManager();
        SendTagsProcessor sendTagsProcessor = new SendTagsProcessor();
        NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
        RequestStorage requestStorage = RepositoryModule.getRequestStorage();
        pushwooshRepository = new PushwooshRepository(requestManager, sendTagsProcessor, registrationPrefs, notificationPrefs, requestStorage, serverCommunicationManager);

        gdprManager = new GDPRManager(pushwooshRepository, notificationManager, pushwooshInApp);
        richMediaStyle = new RichMediaStyle(0, new RichMediaAnimationSlideBottom());
        richMediaController = new RichMediaController(
                new ResourceViewStrategyFactory(),
                new RichMediaFactory(),
                InAppModule.getInAppFolderProvider(),
                richMediaStyle);

        pushwooshDefaultEvents = new PushwooshDefaultEvents();

        pushRegistrarHelper = new PushRegistrarHelper(config.getPluginProvider(), notificationManager);

        pushwooshStartWorker = new PushwooshStartWorker(
                config,
                registrationPrefs,
                appVersionProvider,
                pushwooshRepository,
                notificationManager,
                pushwooshInApp,
                deviceRegistrar,
                pushwooshDefaultEvents,
                pushRegistrarHelper,
                serverCommunicationManager);
    }

    public static PushwooshPlatform getInstance() {
        return instance;
    }

    public Config getConfig() {
        return config;
    }

    public PushwooshNotificationManager notificationManager() {
        return notificationManager;
    }

    public PushwooshRepository pushwooshRepository() {
        return pushwooshRepository;
    }

    public PushwooshInAppImpl pushwooshInApp() {
        return pushwooshInApp;
    }

    public RichMediaController getRichMediaController() {
        return richMediaController;
    }

    public BusinessCasesManager getBusinessCasesManager() {
        return businessCasesManager;
    }

    public GDPRManager getGdprManager() {
        return gdprManager;
    }

    public UUIDFactory getUUIDFactory() {
        return UUIDFactory;
    }

    public NotificationServiceExtension notificationService() {
        if (notificationServiceExtension == null) {
            try {
                Class<?> clazz = config.getNotificationService();
                if (clazz != null) {
                    notificationServiceExtension = (NotificationServiceExtension) clazz.newInstance();
                } else {
                    notificationServiceExtension = new NotificationServiceExtension();
                }
            } catch (Exception e) {
                PWLog.exception(e);
                notificationServiceExtension = new NotificationServiceExtension();
            }
        }

        return notificationServiceExtension;
    }

    public RichMediaStyle getRichMediaStyle() {
        return richMediaStyle;
    }

    public void onApplicationCreated() {
        pushwooshStartWorker.onApplicationCreated();
    }



    public void reset() {
        pushwooshStartWorker.reset();
    }

    AppVersionProvider getAppVersionProvider() {
        return appVersionProvider;
    }

    public RegistrationPrefs getRegistrationPrefs() {
        return registrationPrefs;
    }

    public PushRegistrarHelper getPushRegistrarHelper() {
        return pushRegistrarHelper;
    }

    public ServerCommunicationManager getServerCommunicationManager() {
        return serverCommunicationManager;
    }

    public Activity getTopActivity() {
        return topActivity;
    }

    public void setTopActivity(Activity topActivity) {
        this.topActivity = topActivity;
    }
}
