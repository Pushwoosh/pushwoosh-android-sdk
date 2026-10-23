package com.pushwoosh.notification;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.internal.event.AppIdChangedEvent;
import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.event.NotificationPermissionEvent;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.NotificationPermissionActivity;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.RequestPermissionHelper;
import com.pushwoosh.notification.event.DeregistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationErrorEvent;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandleChainProvider;
import com.pushwoosh.notification.handlers.message.user.MessageHandleChainProvider;
import com.pushwoosh.notification.handlers.notification.NotificationOpenHandlerChainProvider;
import com.pushwoosh.repository.AppCodeApplier;
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.tags.TagsBundle;

import java.util.concurrent.atomic.AtomicBoolean;

public class PushwooshNotificationManager {
    private static final String TAG = "NotificationManager";

    public static class ApplicationIdReadyEvent implements Event {
        public ApplicationIdReadyEvent() {
            /*do nothing*/
        }
    }

    private final RegistrationPrefs registrationPrefs;
    private final AppCodeApplier appCodeApplier;
    private final DeviceRegistrar deviceRegistrar;
    private PushRegistrar pushRegistrar;
    private PushMessage launchNotification;
    private final Config config;
    private final AtomicBoolean appIdReadyEventSent = new AtomicBoolean(false);
    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 14;

    public PushwooshNotificationManager(
            Config config,
            PushRegistrar pushRegistrar,
            @NonNull RegistrationPrefs registrationPrefs,
            @NonNull DeviceRegistrar deviceRegistrar) {
        this.config = config;
        this.pushRegistrar = pushRegistrar;
        this.registrationPrefs = registrationPrefs;
        this.appCodeApplier = new AppCodeApplier(registrationPrefs);
        this.deviceRegistrar = deviceRegistrar;
    }

    public void initialize() {
        PWLog.noise(TAG, "initialize()");

        MessageHandleChainProvider.init();
        MessageSystemHandleChainProvider.init();
        NotificationOpenHandlerChainProvider.init();

        String appId = TextUtils.isEmpty(config.getAppId())
                ? registrationPrefs.applicationId().get()
                : config.getAppId();
        PWLog.debug(TAG, "initialized with app id: " + appId);

        if (!TextUtils.isEmpty(appId)) {
            setAppId(appId);
        }
    }

    public void initPushRegistrar() {
        pushRegistrar.init();
    }

    public void setAppId(String appId) {
        setAppId(appId, null);
    }

    /**
     * Applies the application code and, when {@code customBaseUrl} is provided, the explicit base URL.
     * Invalid input (empty application code, malformed URL) is rejected inside
     * {@link AppCodeApplier#apply} and makes the whole call a logged no-op.
     */
    public void setAppId(@NonNull String appId, @Nullable String customBaseUrl) {
        PWLog.noise(TAG, "setAppId()");
        AppCodeApplier.Result result = appCodeApplier.apply(appId, customBaseUrl);
        if (result.isRejected()) {
            return;
        }

        NetworkModule.getRequestManager().updateBaseUrl(result.getBaseUrl());

        AppCodeApplier.PreviousRegistration previous = result.getPreviousRegistration();
        if (previous != null) {
            appIdReadyEventSent.set(false);
            if (previous.wasRegisteredOnServer) {
                deviceRegistrar.unregisterWithServer(previous.pushToken, previous.baseUrl, previous.appCode);
            }
            EventBus.sendEvent(new AppIdChangedEvent(appId, previous.appCode));
        }

        if (!appIdReadyEventSent.get()) {
            EventBus.sendEvent(new ApplicationIdReadyEvent());
            appIdReadyEventSent.set(true);
        }
    }

    /**
     * @deprecated Sender ID is no longer required. This method is a no-op.
     */
    @Deprecated
    public void setSenderId(String senderId) {
        // no-op: senderId is no longer used
    }

    public static void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        Context context = AndroidPlatformModule.getApplicationContext();
        RequestPermissionHelper.requestPermissionsForClass(
                NotificationPermissionActivity.class, context, new String[] {"android.permission.POST_NOTIFICATIONS"});
    }

    public void registerSMSNumber(String phoneNumber) {
        registerChannelNumber(phoneNumber, DeviceRegistrar.PLATFORM_SMS);
    }

    public void registerWhatsappNumber(String phoneNumber) {
        registerChannelNumber(phoneNumber, DeviceRegistrar.PLATFORM_WHATSAPP);
    }

    private void registerChannelNumber(String phoneNumber, int platform) {
        deviceRegistrar.registerWithServer(phoneNumber, null, platform, result -> {
            if (result.isSuccess()) {
                PWLog.info(TAG, "Registered phone number");
                return;
            }

            String errorDescription =
                    result.getException() == null ? "" : result.getException().getMessage();
            if (TextUtils.isEmpty(errorDescription)) {
                errorDescription = "Pushwoosh registration error";
            }

            PWLog.error(TAG, "Phone number registration error: " + errorDescription);
        });
    }

    public void registerForPushNotifications(
            Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback,
            boolean shouldRequestPermission,
            TagsBundle tags) {
        PWLog.noise(TAG, "registerForPushNotifications()");

        EventBus.subscribe(NotificationPermissionEvent.class, new EventListener<NotificationPermissionEvent>() {
            @Override
            public void onReceive(NotificationPermissionEvent event) {
                boolean notificationsAllowed = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsAllowed =
                            event.getGrantedPermissions().contains("android.permission.POST_NOTIFICATIONS");
                }
                registerForPushesInternal(callback, notificationsAllowed, tags);
            }
        });

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                registerForPushesInternal(callback, true, tags);
            } else if (ContextCompat.checkSelfPermission(
                            AndroidPlatformModule.getApplicationContext(), "android.permission.POST_NOTIFICATIONS")
                    != PackageManager.PERMISSION_GRANTED) {
                // check if user has manually denied notification permission, if not - request permission
                if (!registrationPrefs.hasUserDeniedNotificationPermission().get() && shouldRequestPermission) {
                    requestNotificationPermission();
                    // permission denied - register for pushes silently with notificationsAllowed == false
                } else {
                    registerForPushesInternal(callback, false, tags);
                }
                // permission already granted - register silently with notificationsAllowed == true and
                // set hasUserDeniedNotificationPermission to false in case permission was granted from app settings
            } else {
                registrationPrefs.hasUserDeniedNotificationPermission().set(false);
                registerForPushesInternal(callback, true, tags);
            }
        } catch (Exception e) {
            PWLog.exception(e);
            EventBus.sendEvent(new RegistrationErrorEvent(e.getMessage()));
        }
    }

    public void registerExistingToken(
            String token,
            Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        PWLog.noise(TAG, "RegisterExistingToken");
        RegistrationCallbackHolder.setCallback(callback);
        onTokenReceived(token, null);
    }

    private void registerForPushesInternal(Callback callback, boolean notificationsAllowed, TagsBundle tags) {
        PWLog.noise(TAG, "registerForPushesInternal()");

        try {
            registrationPrefs.isRegisteredForPush().set(true);
            RegistrationCallbackHolder.setCallback(callback);

            pushRegistrar.checkDevice(registrationPrefs.applicationId().get());

            final String pushToken = registrationPrefs.pushToken().get();

            long regDate = registrationPrefs.lastPushRegistration().get();
            long currentTime = System.currentTimeMillis();

            if (TextUtils.isEmpty(pushToken) || (currentTime - regDate) > EXPIRATION_TIME) {
                pushRegistrar.registerPW(tags);
            } else {
                EventBus.sendEvent(new RegistrationSuccessEvent(
                        new RegisterForPushNotificationsResultData(pushToken, notificationsAllowed)));
            }
        } catch (Exception e) {
            PWLog.exception(e);
            EventBus.sendEvent(new RegistrationErrorEvent(e.getMessage()));
        }
    }

    public void unregisterForPushNotifications(Callback<String, UnregisterForPushNotificationException> callback) {
        UnregistrationCallbackHolder.setCallback(callback);
        registrationPrefs.isRegisteredForPush().set(false);
        deviceRegistrar.cancelScheduledRegistration();
        pushRegistrar.unregisterPW();
    }

    public String getPushToken() {
        String token = registrationPrefs.pushToken().get();
        if (!TextUtils.isEmpty(token)) {
            return token;
        }

        return null;
    }

    public PushMessage getLaunchNotification() {
        return launchNotification;
    }

    public void clearLaunchNotification() {
        launchNotification = null;
    }

    @SuppressWarnings("WeakerAccess")
    public void setLaunchNotification(PushMessage launchNotification) {
        this.launchNotification = launchNotification;
    }

    public void onTokenReceived(@NonNull String pushToken, @Nullable String tagsJson) {
        PWLog.noise(TAG, String.format("onTokenReceived: %s", pushToken));
        if (DeviceSpecificProvider.getInstance() == null) {
            PWLog.error(TAG, "can't register device: no push transport module");
            EventBus.sendEvent(new RegistrationErrorEvent("can't register device: no push transport module"));
            return;
        }
        // written before scheduling on purpose: FCM/HMS onNewToken dedup relies on it in any SDK state
        registrationPrefs.pushToken().set(pushToken);
        deviceRegistrar.scheduleRegistration(tagsJson);
    }

    public void onFailedToRegisterForRemoteNotifications(String error) {
        EventBus.sendEvent(new RegistrationErrorEvent(error));
    }

    public void onUnregisteredFromRemoteNotifications(String pushToken) {
        registrationPrefs.clearPushRegistrationInfo();
        deviceRegistrar.unregisterWithServer(pushToken);
    }

    public void onFailedToUnregisterFromRemoteNotifications(String error) {
        EventBus.sendEvent(new DeregistrationErrorEvent(error));
    }

    public void setPushRegistrar(PushRegistrar pushRegistrar) {
        if (pushRegistrar != null) {
            this.pushRegistrar = pushRegistrar;
            initPushRegistrar();
        }
    }
}
