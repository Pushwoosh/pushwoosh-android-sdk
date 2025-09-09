package com.pushwoosh.notification;

import static com.pushwoosh.repository.DeviceRegistrar.areNotificationsEnabled;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import com.pushwoosh.PushwooshWorkManagerHelper;
import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.AppIdChangedEvent;
import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.EventListener;
import com.pushwoosh.internal.event.NotificationPermissionEvent;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.registrar.ExistingTokenRegistrarWorker;
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
import com.pushwoosh.repository.DeviceRegistrar;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;

import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class PushwooshNotificationManager {
    private static final String TAG = "NotificationManager";

    public static class ApplicationIdReadyEvent implements Event {
        public ApplicationIdReadyEvent() {/*do nothing*/}
    }

    private final RegistrationPrefs registrationPrefs;
    private PushRegistrar pushRegistrar;
    private PushMessage launchNotification;
    private Config config;
    private AtomicBoolean pushesRescheduled = new AtomicBoolean(false);
    private AtomicBoolean appIdReadyEventSent = new AtomicBoolean(false);
    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24 * 14;
    public PushwooshNotificationManager(PushRegistrar pushRegistrar, Config config) {
        this.config = config;
        this.pushRegistrar = pushRegistrar;
        registrationPrefs = RepositoryModule.getRegistrationPreferences();
    }

    public void initialize() {
        PWLog.noise(TAG, "initialize()");

        MessageHandleChainProvider.init();
        MessageSystemHandleChainProvider.init();
        NotificationOpenHandlerChainProvider.init();

        String appId = TextUtils.isEmpty(config.getAppId()) ? registrationPrefs.applicationId().get() : config.getAppId();
        String projectId = DeviceSpecificProvider.getInstance().projectId();
        PWLog.debug(TAG, "initialized with app id: " + appId + ", sender id: " + projectId);;

        if (!TextUtils.isEmpty(projectId)) {
            setSenderId(projectId);
        }

        if (!TextUtils.isEmpty(appId)) {
            setAppId(appId);
        }
    }

    public void initPushRegistrar() {
        pushRegistrar.init();
    }

    public void setAppId(String appId) {
        PWLog.noise(TAG, "setAppId()");
        if (TextUtils.isEmpty(appId)) {
            throw new IllegalArgumentException("Application id is empty");
        }

        String oldAppId = registrationPrefs.applicationId().get();
        boolean needUpdateUrl = false;

        if (!Objects.equals(oldAppId, appId)) {
            appIdReadyEventSent.set(false);
            if (registrationPrefs.registeredOnServer().get()) {
                DeviceRegistrar.unregisterWithServer(registrationPrefs.pushToken().get(), registrationPrefs.baseUrl().get());
            }

            new ClearRequestStorageTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            registrationPrefs.removeAppId();
            needUpdateUrl = true;
            registrationPrefs.forceRegister().set(registrationPrefs.isRegisteredForPush().get());
            registrationPrefs.setAppId(appId);
            EventBus.sendEvent(new AppIdChangedEvent(appId, oldAppId));
        }

        registrationPrefs.setAppId(appId);
        if (needUpdateUrl) {
            RequestManager requestManager = NetworkModule.getRequestManager();
            if (requestManager != null) {
                requestManager.updateBaseUrl(registrationPrefs.baseUrl().get());
            }
        }
        if (!appIdReadyEventSent.get()) {
            EventBus.sendEvent(new ApplicationIdReadyEvent());
            appIdReadyEventSent.set(true);
        }
    }

    public void setSenderId(String senderId) {
        if (TextUtils.isEmpty(senderId)) {
            throw new IllegalArgumentException("Sender id is empty");
        }

        String oldSenderId = registrationPrefs.projectId().get();
        boolean needRegister = false;
        if (!TextUtils.equals(oldSenderId, senderId) && !TextUtils.isEmpty(oldSenderId)) {
            PWLog.info(TAG, "Sender ID changed, clearing token");
            if (!registrationPrefs.pushToken().get().isEmpty()) {
                needRegister = true;
            }
            registrationPrefs.removeSenderId();
        }

        registrationPrefs.projectId().set(senderId);

        if (needRegister) {
            pushRegistrar.registerPW(null);
        }
    }

    public static void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        Context context = AndroidPlatformModule.getApplicationContext();
        RequestPermissionHelper.requestPermissionsForClass(NotificationPermissionActivity.class,
                context, new String[] {"android.permission.POST_NOTIFICATIONS"});
    }

    public void registerSMSNumber(String phoneNumber) {
        DeviceRegistrar.registerWithServer(phoneNumber, null, DeviceRegistrar.PLATFORM_SMS, result -> {
            if (result.isSuccess()) {
                PWLog.info(TAG, "Registered phone number: " + phoneNumber);
            } else {
                String errorDescription = result.getException() == null ? "" : result.getException().getMessage();
                if (TextUtils.isEmpty(errorDescription)) {
                    errorDescription = "Pushwoosh registration error";
                }

                PWLog.error(TAG, "Phone number registration error: " + errorDescription);
            }
        });
    }

    public void registerWhatsappNumber(String phoneNumber) {
        DeviceRegistrar.registerWithServer(phoneNumber, null, DeviceRegistrar.PLATFORM_WHATSAPP, result -> {
            if (result.isSuccess()) {
                PWLog.info(TAG, "Registered phone number for Whatsapp: " + phoneNumber);
            } else {
                String errorDescription = result.getException() == null ? "" : result.getException().getMessage();
                if (TextUtils.isEmpty(errorDescription)) {
                    errorDescription = "Pushwoosh registration error";
                }

                PWLog.error(TAG, "Whatsapp registration error: " + errorDescription);
            }
        });
    }

    public void registerForPushNotifications(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback, boolean shouldRequestPermission, TagsBundle tags) {
        PWLog.noise(TAG, "registerForPushNotifications()");

        EventBus.subscribe(NotificationPermissionEvent.class, new EventListener<NotificationPermissionEvent>() {
            @Override
            public void onReceive(NotificationPermissionEvent event) {
                boolean notificationsAllowed = true;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationsAllowed = event.getGrantedPermissions().contains("android.permission.POST_NOTIFICATIONS");
                }
                registerForPushesInternal(callback, notificationsAllowed, tags);
            }
        });

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                registerForPushesInternal(callback, true, tags);
            } else if (ContextCompat.checkSelfPermission(AndroidPlatformModule.getApplicationContext(),
                    "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                // check if user has manually denied notification permission, if not - request permission
                if (!RepositoryModule.getRegistrationPreferences().hasUserDeniedNotificationPermission().get() && shouldRequestPermission) {
                    requestNotificationPermission();
                // permission denied - register for pushes silently with notificationsAllowed == false
                } else {
                    registerForPushesInternal(callback, false, tags);
                }
            // permission already granted - register silently with notificationsAllowed == true and
            // set hasUserDeniedNotificationPermission to false in case permission was granted from app settings
            } else {
                RepositoryModule.getRegistrationPreferences().hasUserDeniedNotificationPermission().set(false);
                registerForPushesInternal(callback, true, tags);
            }
        } catch (Exception e) {
            PWLog.exception(e);
            EventBus.sendEvent(new RegistrationErrorEvent(e.getMessage()));
        }
    }

    public void registerExistingToken(
            String token,
            Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback
    ) {
        PWLog.noise(TAG, "RegisterExistingToken");
        if (TextUtils.isEmpty(token)) {
            PWLog.error(TAG, "Token is empty, ignoring method call");
            return;
        }
        RegistrationCallbackHolder.setCallback(callback, false);
        Data inputData = new Data.Builder()
                .putString(ExistingTokenRegistrarWorker.TOKEN, token)
                .build();
        OneTimeWorkRequest existingWorkerRequest = new OneTimeWorkRequest.Builder(ExistingTokenRegistrarWorker.class)
                .setInputData(inputData)
                .setConstraints(PushwooshWorkManagerHelper.getNetworkAvailableConstraints())
                .build();
        PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                existingWorkerRequest,
                ExistingTokenRegistrarWorker.TAG,
                ExistingWorkPolicy.REPLACE
        );
    }

    private void registerForPushesInternal(Callback callback, boolean notificationsAllowed, TagsBundle tags) {
        PWLog.noise(TAG, "registerForPushesInternal()");

        try {
            boolean communicationEnable = registrationPrefs.communicationEnable().get();
            if (!communicationEnable) {
                PWLog.info(TAG, "Communication with Pushwoosh is disabled");
                return;
            }
            registrationPrefs.isRegisteredForPush().set(true);
            RegistrationCallbackHolder.setCallback(callback, true);

            pushRegistrar.checkDevice(registrationPrefs.applicationId().get());

            final String pushToken = registrationPrefs.pushToken().get();

            long regDate = registrationPrefs.lastPushRegistration().get();
            long currentTime = System.currentTimeMillis();

            if (TextUtils.isEmpty(pushToken) || (currentTime - regDate) > EXPIRATION_TIME) {
                pushRegistrar.registerPW(tags);
            } else {
                EventBus.sendEvent(new RegistrationSuccessEvent(new RegisterForPushNotificationsResultData(pushToken, notificationsAllowed)));
            }
        } catch (Exception e) {
            PWLog.exception(e);
            EventBus.sendEvent(new RegistrationErrorEvent(e.getMessage()));
        }
    }

    public void unregisterForPushNotifications(Callback<String, UnregisterForPushNotificationException> callback) {
        UnregistrationCallbackHolder.setCallback(callback);
        registrationPrefs.isRegisteredForPush().set(false);
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

    public LocalNotificationRequest scheduleLocalNotification(LocalNotification notification) {
        int requestId = LocalNotificationReceiver.scheduleNotification(notification.getExtras(), notification.getDelay());
        return new LocalNotificationRequest(requestId);
    }

    public void rescheduleLocalNotifications() {
        if (pushesRescheduled.get()) {
            PWLog.warn(TAG, "Local pushes already rescheduled");
            return;
        }
        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(RescheduleNotificationsWorker.class)
                        .build();

        PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(request,
                RescheduleNotificationsWorker.TAG,
                ExistingWorkPolicy.KEEP);

        pushesRescheduled.set(true);
    }

    public void onExistingTokenReceived(String pushToken, String tagsJson) {
        onRegisteredForRemoteNotifications(pushToken, tagsJson, true);
    }

    public void onRemoteTokenReceived(String pushToken, String tagsJson) {
        onRegisteredForRemoteNotifications(pushToken, tagsJson, false);
    }

    private void onRegisteredForRemoteNotifications(String pushToken, String tagsJson, boolean shouldRetryRegistration) {
        PWLog.noise("PushwooshNotificationManager", String.format("onRegisteredForRemoteNotifications: %s", pushToken));
        //todo: probably we should move this into `if (result.isSuccess) { ... }`
        registrationPrefs.pushToken().set(pushToken);
        if (DeviceSpecificProvider.getInstance() != null) {
            if (shouldRetryRegistration) {
                DeviceRegistrar.registerWithServerWithRetries(
                        pushToken,tagsJson,DeviceSpecificProvider.getInstance().deviceType(), provideServerRegistrationCallback(pushToken));
            } else {
                DeviceRegistrar.registerWithServer(
                        pushToken,tagsJson,DeviceSpecificProvider.getInstance().deviceType(), provideServerRegistrationCallback(pushToken));
            }
        }
    }

    private Callback<Void, NetworkException> provideServerRegistrationCallback(String pushToken) {
        PWLog.noise(TAG, "provideServerRegistrationCallback");
        return new Callback<Void, NetworkException>() {
            @Override
            public void process(@NonNull Result<Void, NetworkException> result) {
                if (result.isSuccess()) {
                    registrationPrefs.registeredOnServer().set(true);
                    registrationPrefs.lastPushRegistration().set(new Date().getTime());
                    EventBus.sendEvent(new RegistrationSuccessEvent(
                            new RegisterForPushNotificationsResultData(
                                    pushToken,
                                    areNotificationsEnabled()
                            )
                    ));

                    PWLog.info(TAG, String.format("successfully registered for push notifications: %s", pushToken));
                } else {
                    EventBus.sendEvent(new RegistrationErrorEvent("can't register device"));

                    PWLog.error(TAG, "can't register device", result.getException());
                }
            }
        };
    }

    public void onFailedToRegisterForRemoteNotifications(String error) {
        EventBus.sendEvent(new RegistrationErrorEvent(error));
    }

    public void onUnregisteredFromRemoteNotifications(String pushToken) {
        registrationPrefs.clearSenderIdInfo();
        DeviceRegistrar.unregisterWithServer(pushToken);
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

    private static class ClearRequestStorageTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            RepositoryModule.getRequestStorage().clear();
            return null;
        }
    }
}
