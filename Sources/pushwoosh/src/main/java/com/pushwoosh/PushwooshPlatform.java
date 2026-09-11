package com.pushwoosh;

import android.app.Activity;
import android.content.res.Resources;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.appevents.PushwooshDefaultEvents;
import com.pushwoosh.inapp.PushwooshInAppImpl;
import com.pushwoosh.inapp.PushwooshInAppServiceImpl;
import com.pushwoosh.inapp.view.strategy.ResourceViewStrategyFactory;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.utils.AppVersionProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.UUIDFactory;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessageFactory;
import com.pushwoosh.notification.PushwooshNotificationManager;
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

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public class PushwooshPlatform {
    private static final AtomicBoolean notified = new AtomicBoolean(false);
    private com.pushwoosh.internal.utils.UUIDFactory UUIDFactory;
    private PushMessageFactory pushMessageFactory;

    public static void notifyNotInitialized() {
        if (notified.compareAndSet(false, true)) {
            PWLog.warn(TAG, "Pushwoosh library not initialized. All method calls will be ignored");
        }
    }

    private static final String TAG = "PushwooshPlatform";

    private static PushwooshPlatform instance;

    private final Config config;
    private final PushwooshNotificationManager notificationManager;
    private final PushwooshRepository pushwooshRepository;
    private final RegistrationPrefs registrationPrefs;
    private final PushwooshInAppImpl pushwooshInApp;
    private final ServerCommunicationManager serverCommunicationManager;
    private NotificationServiceExtension notificationServiceExtension;
    private RichMediaController richMediaController;
    private AppVersionProvider appVersionProvider;
    private PushwooshStartWorker pushwooshStartWorker;
    private DeviceRegistrar deviceRegistrar;
    private RichMediaStyle richMediaStyle;
    private Activity topActivity;
    // Written from Activity lifecycle callbacks (main), read by RichMediaColorSchemeResolver on
    // whatever thread the app called PushwooshInAppUi.present() from.
    private volatile WeakReference<Activity> hostActivity;
    private PushwooshDefaultEvents pushwooshDefaultEvents;

    public PushMessageFactory getPushMessageFactory() {
        return pushMessageFactory;
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
        deviceRegistrar = new DeviceRegistrar();
        RepositoryModule.init(config, deviceRegistrar);
        PWLog.init();
        registrationPrefs = RepositoryModule.getRegistrationPreferences();
        serverCommunicationManager = new ServerCommunicationManager();

        NetworkModule.init(registrationPrefs, serverCommunicationManager, config.isReverseProxyAllowed());

        notificationManager =
                new PushwooshNotificationManager(config, builder.pushRegistrar, registrationPrefs, deviceRegistrar);
        pushwooshInApp = new PushwooshInAppImpl(new PushwooshInAppServiceImpl(), serverCommunicationManager);
        pushMessageFactory = new PushMessageFactory();

        appVersionProvider =
                new AppVersionProvider(AndroidPlatformModule.getPrefsProvider().providePrefs("PWAppVersion"));

        RequestManager requestManager = NetworkModule.getRequestManager();
        SendTagsProcessor sendTagsProcessor = new SendTagsProcessor();
        NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
        pushwooshRepository =
                new PushwooshRepository(requestManager, sendTagsProcessor, registrationPrefs, notificationPrefs);

        richMediaStyle = new RichMediaStyle(0, new RichMediaAnimationSlideBottom());
        richMediaController =
                new RichMediaController(new ResourceViewStrategyFactory(), new RichMediaFactory(), richMediaStyle);

        pushwooshDefaultEvents = new PushwooshDefaultEvents();

        pushwooshStartWorker = new PushwooshStartWorker(
                config,
                registrationPrefs,
                pushwooshRepository,
                notificationManager,
                pushwooshInApp,
                deviceRegistrar,
                pushwooshDefaultEvents,
                DeviceUtils::getDeviceUUID);
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

    AppVersionProvider getAppVersionProvider() {
        return appVersionProvider;
    }

    public RegistrationPrefs getRegistrationPrefs() {
        return registrationPrefs;
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

    // One method on purpose: a private static helper here would be swallowed by MockedStatic
    // in tests that call the real isHostCandidate.
    public static boolean isHostCandidate(@NonNull Activity activity) {
        Resources.Theme theme = activity.getTheme();
        TypedValue value = new TypedValue();
        boolean translucent =
                theme.resolveAttribute(android.R.attr.windowIsTranslucent, value, true) && value.data != 0;
        boolean floating = theme.resolveAttribute(android.R.attr.windowIsFloating, value, true) && value.data != 0;
        return !translucent && !floating;
    }

    @Nullable public Activity getHostActivity() {
        WeakReference<Activity> ref = hostActivity;
        return ref == null ? null : ref.get();
    }

    public void setHostActivity(@Nullable Activity activity) {
        hostActivity = activity == null ? null : new WeakReference<>(activity);
    }
}
