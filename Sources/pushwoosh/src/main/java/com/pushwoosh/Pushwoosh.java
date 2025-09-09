package com.pushwoosh;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.MergeUserException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.exception.SetEmailException;
import com.pushwoosh.exception.SetUserException;
import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.internal.PushRegistrarHelper;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.LocalNotification;
import com.pushwoosh.notification.LocalNotificationRequest;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Pushwoosh class is used to manage push registration, application tags and local notifications.<br>
 * By default Pushwoosh SDK automatically adds following permissions: <br>
 * ${applicationId}.permission.C2D_MESSAGE <br>
 * ${applicationId}.permission.RECEIVE_ADM_MESSAGE <br>
 * <a href="https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_NETWORK_STATE">android.permission.ACCESS_NETWORK_STATE</a> <br>
 * <a href="https://developer.android.com/reference/android/Manifest.permission.html#INTERNET">android.permission.INTERNET</a> <br>
 * <a href="https://developer.android.com/reference/android/Manifest.permission.html#WAKE_LOCK">android.permission.WAKE_LOCK</a> <br>
 * <br><br>
 * <a href="https://developer.android.com/reference/android/Manifest.permission.html#RECEIVE_BOOT_COMPLETED">android.permission.RECEIVE_BOOT_COMPLETED</a>
 * should be added manually to take advantage of local notification rescheduling after device restart.
 */
public class Pushwoosh {
    /**
     * Intent extra key for push notification payload. Is added to intent that starts Activity when push notification is clicked.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   @Override
     *   public void onCreate(Bundle savedInstanceState) {
     *       super.onCreate(savedInstanceState);
     *
     *       if (getIntent().hasExtra(Pushwoosh.PUSH_RECEIVE_EVENT)) {
     *           // Activity was started in response to push notification
     *           showMessage("Push message is " + getIntent().getExtras().getString(Pushwoosh.PUSH_RECEIVE_EVENT));
     *       }
     *   }
     * }
     * </pre>
     */
    public static final String PUSH_RECEIVE_EVENT = "PUSH_RECEIVE_EVENT";

    /**
     * Maximum number of notifications returned by {@link Pushwoosh#getPushHistory()}
     */
    public static final int PUSH_HISTORY_CAPACITY = 16;

    private static final Pushwoosh INSTANCE = new Pushwoosh();

    private final PushwooshNotificationManager notificationManager;
    private final PushwooshRepository pushwooshRepository;
    private final InAppRepository inAppRepository;
    private final PushRegistrarHelper pushRegistrarHelper;
    private final RegistrationPrefs registrationPrefs;
    private final ServerCommunicationManager serverCommunicationManager;
    private Subscription<RegistrationSuccessEvent> subscriberRegister;
    private volatile boolean isInitialized = false;

    private Pushwoosh() {
        PushwooshPlatform pushwooshPlatform = PushwooshPlatform.getInstance();
        if (pushwooshPlatform == null) {
            notificationManager = null;
            pushwooshRepository = null;
            inAppRepository = null;
            pushRegistrarHelper = null;
            registrationPrefs = null;
            serverCommunicationManager = null;
        } else {
            notificationManager = pushwooshPlatform.notificationManager();
            pushwooshRepository = pushwooshPlatform.pushwooshRepository();
            inAppRepository = InAppModule.getInAppRepository();
            pushRegistrarHelper = pushwooshPlatform.getPushRegistrarHelper();
            registrationPrefs = pushwooshPlatform.getRegistrationPrefs();
            serverCommunicationManager = pushwooshPlatform.getServerCommunicationManager();
            isInitialized = true;
        }
    }

    /**
     * Checks if public api is properly initialized
     *
     * @return true if public api is ready to use
     */
    private boolean ensureInitialized() {
        if (!isInitialized) {
            PWLog.error("Pushwoosh", "SDK is not initialized.");
            return false;
        }
        return true;
    }

    /** @noinspection BooleanMethodIsAlwaysInverted*/
    private boolean ensureAllowedCommunication() {
        if (!isServerCommunicationAllowed()) {
            PWLog.error("Pushwoosh", "Communication with Pushwoosh is disabled. See startServerCommunication method.");
            return false;
        }
        return true;
    }

    private <T, E extends PushwooshException> void safeProcessCallback(Callback<T, E> callback, Result<T, E> result) {
        if (callback != null) {
            callback.process(result);
        }
    }

    /**
     * @return Pushwoosh shared instance
     */
    @NonNull
    public static Pushwoosh getInstance() {
        return INSTANCE;
    }

    /**
     * @return Current Pushwoosh application code
     */
    public String getApplicationCode() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getApplicationCode()");
        try {
            if (ensureInitialized()) {
                return registrationPrefs.applicationId().get();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get application code", e);
        }
        return "";
    }

    /**
     * @return Current Pushwoosh application code
     * @see #getApplicationCode()
     * @deprecated
     */
    @Deprecated
    public String getAppId() {
        return getApplicationCode();
    }

    /**
     * Associates current applicaton with given pushwoosh application code
     * (Alternative for "com.pushwoosh.appid" metadata in AndroidManifest.xml)
     *
     * @param appId Pushwoosh application code
     */
    public void setAppId(@NonNull String appId) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setAppId()");
        try {
            if (ensureInitialized()) {
                notificationManager.setAppId(appId);
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set application code", e);
        }
    }

    /**
     * Sets FCM/GCM sender Id
     * (Alternative for "com.pushwoosh.senderid" metadata in AndroidManifest.xml)
     *
     * @param senderId GCM/FCM sender id
     */
    public void setSenderId(@NonNull String senderId) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setSenderId()");
        try {
            if (ensureInitialized()) {
                notificationManager.setSenderId(senderId);
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set sender id", e);
        }
    }

    /**
     * @return Current GCM/FCM sender id
     */
    public String getSenderId() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getSenderId()");
        try {
            if (ensureInitialized()) {
                return registrationPrefs.projectId().get();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get sender id", e);
        }
        return "";
    }

    /**
     * @return Pushwoosh HWID associated with current device
     */
    @NonNull
    public String getHwid() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getHwid()");
        try {
            if (ensureInitialized()) {
                return pushwooshRepository.getHwid();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get hwid", e);
        }
        return "";
    }

    /**
     * @return Push notification token or null if device is not registered yet.
     */
    @Nullable
    public String getPushToken() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getPushToken()");
        try {
            if (ensureInitialized()) {
                return notificationManager.getPushToken();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get push token", e);
        }
        return "";
    }

    /**
     * Set custom application language.
     * Device language used by default.
     * Set to null if you want to use device language again.
     *
     * @param language lowercase two-letter code according to ISO-639-1 standard ("en", "de", "fr", etc.) or null (device language).
     */
    public void setLanguage(@Nullable String language) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setLanguage()");
        try {
            if (ensureInitialized()) {
                registrationPrefs.setLanguage(language);
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set language", e);
        }
    }

    public String getLanguage() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getLanguage()");
        try {
            if (ensureInitialized()) {
                return registrationPrefs.language().get();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get language", e);
        }
        return "";
    }

    public void requestNotificationPermission() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().requestNotificationPermission()");
        try {
            if (ensureInitialized()) {
                PushwooshNotificationManager.requestNotificationPermission();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't request notification permission", e);
        }
    }

    /**
     * @see <a href="#registerForPushNotifications(Callback)">registerForPushNotifications(Callback)</a>
     */
    public void registerForPushNotifications() {
        registerForPushNotifications(null);
    }

    /**
     * @see <a href="#registerForPushNotificationsWithTags(Callback, TagsBundle)">registerForPushNotifications(Callback, TagsBundle)</a>
     */
    public void registerForPushNotificationsWithTags(TagsBundle tags) {
        registerForPushNotificationsWithTags(null, tags);
    }

    /**
     * Registers device for push notifications
     *
     * @param callback push registration callback
     */
    public void registerForPushNotifications(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        registerForPushNotificationsInternal(callback, true, null);
    }

    /**
     * Registers device for push notifications
     *
     * @param callback push registration callback
     * @param tags     tags to be set when registering for pushes
     */
    public void registerForPushNotificationsWithTags(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback, TagsBundle tags) {
        registerForPushNotificationsInternal(callback, true, tags);
    }

    public void registerForPushNotificationsWithoutPermission(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        registerForPushNotificationsInternal(callback, false, null);
    }

    public void registerForPushNotificationsWithTagsWithoutPermission(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback, TagsBundle tagsBundle) {
        registerForPushNotificationsInternal(callback, false, tagsBundle);
    }

    public void registerExistingToken(@NonNull String token, @Nullable Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().registerExistingToken()");
        try {
            if (!ensureInitialized()) {
                return;
            }

            if (TextUtils.isEmpty(token)) {
                PWLog.warn("Pushwoosh", "token is empty");
                safeProcessCallback(callback, Result.fromException(new RegisterForPushNotificationsException("token is empty")));
                return;
            }

            if (token.equals(registrationPrefs.pushToken().get())) {
                RegisterForPushNotificationsResultData data =
                        new RegisterForPushNotificationsResultData(token, NotificationUtils.areNotificationsEnabled());
                safeProcessCallback(callback, Result.fromData(data));
                return;
            }

            SdkStateProvider.getInstance().executeOrQueue(() -> {
                notificationManager.registerExistingToken(token, callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't register device with existing token", e);
        }
    }

    public void addAlternativeAppCode(String appCode) {
        RepositoryModule.getRegistrationPreferences().registerAlternativeAppCode(appCode);
        PWLog.info("Added "+ appCode + " as an alternative app code for registration");
    }

    public void resetAlternativeAppCodes() {
        RepositoryModule.getRegistrationPreferences().resetAlternativeAppCodes();
    }

    public void registerWhatsappNumber(String number) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().registerWhatsappNumber()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            if (Objects.isNull(number) || TextUtils.isEmpty(number)) {
                PWLog.warn("Pushwoosh", "Whatsapp number is empty");
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                notificationManager.registerWhatsappNumber(number);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't register whatsapp number", e);
        }
    }

    public void registerSMSNumber(@NonNull String number) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().registerSMSNumber()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            if (Objects.isNull(number) || TextUtils.isEmpty(number)) {
                PWLog.warn("Pushwoosh", "SMS number is empty");
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                notificationManager.registerSMSNumber(number);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't register sms number", e);
        }
    }

    private void registerForPushNotificationsInternal(
            Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback,
            boolean shouldRequestPermission,
            TagsBundle tags) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().registerForPushNotifications()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new RegisterForPushNotificationsException("SDK is not initialized")));
                return;
            }
            if (!ensureAllowedCommunication()) {
                safeProcessCallback(callback, Result.fromException(new RegisterForPushNotificationsException("Communication with Pushwoosh is disabled")));
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                notificationManager.registerForPushNotifications(callback, shouldRequestPermission, tags);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't register for push notifications", e);
        }
    }

    public void setShowPushnotificationAlert(boolean showAlert) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setShowPushnotificationAlert()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            RepositoryModule.getNotificationPreferences().showPushnotificationAlert().set(showAlert);
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set showPushNotificationAlert", e);
        }
    }

    private void subscribeRegisterFromInApp(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().subscribeRegisterFromInApp()");
        if (callback == null) {
            return;
        }
        try {
            subscriberRegister = EventBus.subscribe(RegistrationSuccessEvent.class,event -> {
                unSubscribeRegisterEvent();
                callback.process(Result.fromData(event.getData()));
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't subscribe to registration success event", e);
        }
    }

    private void unSubscribeRegisterEvent() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().unSubscribeRegisterEvent()");
        if (subscriberRegister == null) {
            return;
        }
        try {
            subscriberRegister.unsubscribe();
            subscriberRegister = null;
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't unsubscribe from registration success event", e);
        }
    }

    /**
     * @see <a href="#unregisterForPushNotifications(Callback)">unregisterForPushNotifications(Callback)</a>
     */
    public void unregisterForPushNotifications() {
        unregisterForPushNotifications(null);
    }

    /**
     * Unregisters device from push notifications
     *
     * @param callback push unregister callback
     */
    public void unregisterForPushNotifications(Callback<String, UnregisterForPushNotificationException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().unregisterForPushNotifications()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new UnregisterForPushNotificationException("SDK is not initialized")));
                return;
            }

            if (!ensureAllowedCommunication()) {
                safeProcessCallback(callback, Result.fromException(new UnregisterForPushNotificationException("Communication with Pushwoosh is disabled")));
                return;
            }

            SdkStateProvider.getInstance().executeOrQueue(() -> {
                notificationManager.unregisterForPushNotifications(callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't unregister for push notifications", e);
        }
    }

    /**
     * Associates device with given tags. If setTags request fails tags will be resent on the next application launch.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   pushwoosh.setTags(Tags.intTag("intTag", 42));
     * }
     * </pre>
     *
     * @param tags {@link com.pushwoosh.tags.TagsBundle application tags bundle}
     */
    public void setTags(@NonNull TagsBundle tags) {
        setTags(tags, null);
    }

    /**
     * Associates device with given email tags. If setEmailTags request fails email tags will be resent on the next application launch.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   pushwoosh.setEmailTags(Tags.intTag("intTag", 42), "my@email.com");
     * }
     * </pre>
     *
     * @param emailTags {@link com.pushwoosh.tags.TagsBundle application tags bundle}
     * @param email     user email
     */

    public void setEmailTags(@NonNull TagsBundle emailTags, @NonNull String email) {
        setEmailTags(emailTags, email, null);
    }

    /**
     * Associates device with given tags. If setTags request fails tags will be resent on the next application launch.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   pushwoosh.setTags(Tags.intTag("intTag", 42), (result) -> {
     *       if (result.isSuccess()) {
     *           // tags sucessfully sent
     *       }
     *       else {
     *           // failed to send tags
     *       }
     *   });
     * }
     * </pre>
     *
     * @param tags     {@link com.pushwoosh.tags.TagsBundle application tags bundle}
     * @param callback sendTags operation callback
     */
    public void setTags(@NonNull TagsBundle tags, Callback<Void, PushwooshException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setTags()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new PushwooshException("SDK is not initialized")));
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                pushwooshRepository.sendTags(tags, callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set tags", e);
        }
    }

    /**
     * Associates device with given email tags. If setEmailTags request fails email tags will be resent on the next application launch.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   List<String> emails = new ArrayList<>();
     *   emails.add("my@email.com");
     *   pushwoosh.setEmailTags(Tags.intTag("intTag", 42), emails, (result) -> {
     *       if (result.isSuccess()) {
     *           // tags sucessfully sent
     *       }
     *       else {
     *           // failed to send tags
     *       }
     *   });
     * }
     * </pre>
     *
     * @param emailTags {@link com.pushwoosh.tags.TagsBundle application tags bundle}
     * @param email     user email
     * @param callback  sendEmailTags operation callback
     */

    public void setEmailTags(@NonNull TagsBundle emailTags, String email, Callback<Void, PushwooshException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setEmailTags()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new PushwooshException("SDK is not initialized")));
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                pushwooshRepository.sendEmailTags(emailTags, email, callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set email tags", e);
        }
    }

    /**
     * @see #setTags(TagsBundle)
     * @deprecated As of release 6.0, replaced by {@link #setTags(TagsBundle)} }
     */
    @Deprecated
    public void sendTags(@NonNull TagsBundle tags) {
        setTags(tags);
    }

    /**
     * @param tags     {@link com.pushwoosh.tags.TagsBundle application tags bundle}
     * @param callback sendTags operation callback
     * @see #setTags(TagsBundle, Callback)
     * @deprecated As of release 6.0, replaced by {@link #setTags(TagsBundle, Callback)} }
     */
    @Deprecated
    public void sendTags(@NonNull TagsBundle tags, Callback<Void, PushwooshException> callback) {
        setTags(tags, callback);
    }

    /**
     * Gets tags associated with current device
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   pushwoosh.getTags((result) -> {
     *       if (result.isSuccess()) {
     *            // tags successfully received
     *            int intTag = result.getInt("intTag");
     *       }
     *       else {
     *           // failed to receive tags
     *       }
     *   });
     * }
     * </pre>
     *
     * @param callback callback handler
     */
    public void getTags(@NonNull Callback<TagsBundle, GetTagsException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getTags()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new GetTagsException("SDK is not initialized")));
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                pushwooshRepository.getTags(callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get tags", e);
        }
    }

    /**
     * Sends In-App purchase statistics. Purchase information is stored in "In-app Product", "In-app Purchase" and "Last In-app Purchase date" default tags.
     *
     * @param sku      purchased product ID
     * @param price    price of the product
     * @param currency currency of the price (ex: “USD”)
     */
    public void sendInappPurchase(@NonNull String sku, @NonNull BigDecimal price, @NonNull String currency) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().sendInappPurchase()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                pushwooshRepository.sendInappPurchase(sku, price, currency, new Date());
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't send in-app purchase", e);
        }
    }

    /**
     * @return Launch notification data or null.
     */
    @Nullable
    public PushMessage getLaunchNotification() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getLaunchNotification()");
        try {
            if (ensureInitialized()) {
                return notificationManager.getLaunchNotification();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get launch notification", e);
        }
        return null;
    }

    /**
     * reset {@link #getLaunchNotification()} to return null.
     */
    public void clearLaunchNotification() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().clearLaunchNotification()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            notificationManager.clearLaunchNotification();
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't clear launch notification", e);
        }
    }

    /**
     * Schedules local notification.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   LocalNotification notification = new LocalNotification.Builder().setMessage("Local notification content")
     * 			  .setDelay(seconds)
     * 			  .build();
     *   LocalNotificationRequest request = Pushwoosh.getInstance().scheduleLocalNotification(notification);
     * }
     * </pre>
     *
     * @param notification {@link com.pushwoosh.notification.LocalNotification notification} to send
     * @return {@link com.pushwoosh.notification.LocalNotificationRequest local notification request}
     */
    public LocalNotificationRequest scheduleLocalNotification(LocalNotification notification) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().scheduleLocalNotification()");
        try {
            if (ensureInitialized()) {
                return notificationManager.scheduleLocalNotification(notification);
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't schedule local notification", e);
        }
        return null;
    }

    /**
     * Gets push notification history. History contains both remote and local notifications.
     *
     * @return Push history as List of {@link com.pushwoosh.notification.PushMessage}. Maximum of {@link #PUSH_HISTORY_CAPACITY} pushes are returned
     */
    @NonNull
    public List<PushMessage> getPushHistory() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getPushHistory()");
        try {
            if (ensureInitialized()) {
                return pushwooshRepository.getPushHistory();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get push history", e);
        }
        return new ArrayList<>();
    }

    /**
     * Clears push history. Usually called after {@link #getPushHistory()}.
     */
    public void clearPushHistory() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().clearPushHistory()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            RepositoryModule.getNotificationPreferences().pushHistory().clear();
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't clear push history", e);
        }
    }

    /**
     * Informs the Pushwoosh about the app being launched. Usually called internally by SDK.
     */
    public void sendAppOpen() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().sendAppOpen()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(pushwooshRepository::sendAppOpen);
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't send app open", e);
        }
    }

    /**
     * Set User identifier. This could be Facebook ID, username or email, or any other user ID.
     * This allows data and events to be matched across multiple user devices.
     *
     * @param userId user identifier
     */
    public void setUserId(@NonNull String userId) {
        setUserId(userId, null);
    }

    /**
     * Set User identifier. This could be Facebook ID, username or email, or any other user ID.
     * This allows data and events to be matched across multiple user devices.
     *
     * @param userId   user identifier
     * @param callback setUserId operation callback
     */
    public void setUserId(@NonNull String userId, Callback<Boolean, SetUserIdException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setUserId()");

        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new SetUserIdException("SDK is not initialized.")));
                return;
            }

            if (!ensureAllowedCommunication()) {
                safeProcessCallback(callback, Result.fromException(new SetUserIdException("Communication with Pushwoosh is disabled")));
                return;
            }

            String oldUserId = registrationPrefs.userId().get();
            if (TextUtils.equals(userId, oldUserId)) {
                safeProcessCallback(callback, Result.fromData(true));
                return;
            }

            if (TextUtils.isEmpty(userId)) {
                return;
            }
            registrationPrefs.userId().set(userId);
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                inAppRepository.setUserId(userId, callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set user id", e);
        }
    }

    /**
     * Set User identifier and register emails associated to the user. UserId could be Facebook ID
     * or any other user ID. This allows data and events to be matched across multiple user devices.
     *
     * @param userId user identifier
     * @param emails user's emails array list
     */

    public void setUser(@NonNull String userId, @NonNull List<String> emails) {
        setUser(userId, emails, null);
    }

    /**
     * Set User identifier and register emails associated to the user. UserId could be Facebook ID
     * or any other user ID. This allows data and events to be matched across multiple user devices.
     *
     * @param userId   user identifier
     * @param emails   user's emails array list
     * @param callback setUser operation callback
     */

    public void setUser(@NonNull String userId, @NonNull List<String> emails, Callback<Boolean, SetUserException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setUser()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new SetUserException("SDK is not initialized.")));
                return;
            }
            if (TextUtils.isEmpty(userId)) {
                return;
            }

            registrationPrefs.userId().set(userId);
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                inAppRepository.setUser(userId, emails, callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set user", e);
        }
    }

    /**
     * Register emails list associated to the current user.
     *
     * @param emails user's emails array list
     */

    public void setEmail(@NonNull List<String> emails) {
        setEmail(emails, null);
    }

    /**
     * Register emails list associated to the current user.
     *
     * @param emails   user's emails array list
     * @param callback setEmail operation callback
     */

    public void setEmail(@NonNull List<String> emails, Callback<Boolean, SetEmailException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setEmail()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new SetEmailException("SDK is not initialized.")));
                return;
            }
            if (emails.isEmpty()) {
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                inAppRepository.setEmail(emails, callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set email", e);
        }
    }

    /**
     * Register email associated to the current user. Email should be a string and could not be null or empty.
     *
     * @param email user's email string
     */

    public void setEmail(@NonNull String email) {
        setEmail(email, null);
    }

    /**
     * Register email associated to the current user. Email should be a string and could not be null or empty.
     *
     * @param email    user's email string
     * @param callback setEmail operation callback
     */

    public void setEmail(@NonNull String email, Callback<Boolean, SetEmailException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setEmail()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new SetEmailException("SDK is not initialized.")));
                return;
            }
            if (TextUtils.isEmpty(email)) {
                return;
            }
            ArrayList<String> list = new ArrayList<>();
            list.add(email);
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                inAppRepository.setEmail(list, callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set email", e);
        }
    }

    /**
     * Move all event statistics from oldUserId to newUserId if doMerge is true. If doMerge is false all events for oldUserId are removed.
     *
     * @param oldUserId source user identifier
     * @param newUserId destination user identifier
     * @param doMerge   merge/remove events for source user identifier
     * @param callback  method completion callback
     */
    public void mergeUserId(@NonNull String oldUserId, @NonNull String newUserId, boolean doMerge, @Nullable Callback<Void, MergeUserException> callback) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().mergeUserId()");
        try {
            if (!ensureInitialized()) {
                safeProcessCallback(callback, Result.fromException(new MergeUserException("SDK is not initialized.")));
                return;
            }
            SdkStateProvider.getInstance().executeOrQueue(() -> {
                inAppRepository.mergeUserId(oldUserId, newUserId, doMerge, callback);
            });
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't merge user id", e);
        }
    }

    /**
     * @return current user id
     * @see #setUserId(String)
     */
    @Nullable
    public String getUserId() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().getUserId()");
        try {
            if (ensureInitialized()) {
                return registrationPrefs.userId().get();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't get user id", e);
        }
        return null;
    }

    /**
     * Enables Huawei push messaging in plugin-based applications. This method gives
     * no effect if it is called in a native application.
     */
    public void enableHuaweiPushNotifications() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().enableHuaweiPushNotifications()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            pushRegistrarHelper.enableHuaweiPushNotifications();
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't enable huawei push notifications", e);
        }
    }

    /**
     * Starts communication with Pushwoosh server.
     */
    public void startServerCommunication() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().startServerCommunication()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            serverCommunicationManager.startServerCommunication();
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't start server communication", e);
        }
    }

    /**
     * Stops communication with Pushwoosh server.
     */
    public void stopServerCommunication() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().stopServerCommunication()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            serverCommunicationManager.stopServerCommunication();
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't stop server communication", e);
        }
    }

    /**
     * Check if communication with Pushwoosh server is allowed.
     *
     * @return true if communication with Pushwoosh server is allowed
     */
    public boolean isServerCommunicationAllowed() {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().isServerCommunicationAllowed()");
        try {
            if (ensureInitialized()) {
                return serverCommunicationManager.isServerCommunicationAllowed();
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't check server communication status", e);
        }
        return false;
    }

    public void setAllowedExternalHosts(ArrayList<String> allowedExternalHosts) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setAllowedExternalHosts()");
        try {
            if (!ensureInitialized()) {
                return;
            }

            if (allowedExternalHosts.isEmpty()) {
                return;
            }

            NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
            for (String s : allowedExternalHosts) {
                if (!TextUtils.isEmpty(s)) {
                    notificationPrefs.allowedExternalHosts().add(s);
                }
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set allowed external hosts", e);
        }
    }

    public void setApiToken(String token) {
        PWLog.noise("Pushwoosh", "Pushwoosh.getInstance().setApiToken()");
        try {
            if (!ensureInitialized()) {
                return;
            }
            registrationPrefs.setApiToken(token);
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't set api token", e);
        }
    }

    /**
     * Sends push message delivery statistics to Pushwoosh server. This method
     * is called internally by Pushwoosh SDK.
     * CAUTION: Usage of this method without a clear purpose will break statistics in the Control panel
     *
     * @param pushBundle Bundle of a received push notification
     */
    private void sendMessageDelivery(Bundle pushBundle) {
        PWLog.noise("Pushwoosh", "sendMessageDelivery()");
        try {
            if (!ensureInitialized()) {
                return;
            }

            if (pushBundle.containsKey("pw_msg")) {
                SdkStateProvider.getInstance().executeOrQueue(() -> {
                    PushwooshMessagingServiceHelper.sendMessageDeliveryEvent(pushBundle);
                });
            } else {
                PWLog.warn("/messageDeliveryEvent request was not sent, as the push was not received from Pushwoosh");
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't send message delivery", e);
        }
    }

    /**
     * Sends push message opened statistics to Pushwoosh server. This method
     * is called internally by Pushwoosh SDK.
     * CAUTION: Usage of this method without a clear purpose will break statistics in the Control panel
     *
     * @param pushBundle Bundle of a received push message
     */
    private void sendPushStat(Bundle pushBundle) {
        PWLog.noise("Pushwoosh", "sendPushStat()");
        try {
            if (!ensureInitialized()) {
                return;
            }

            if (pushBundle.containsKey("pw_msg")) {
                SdkStateProvider.getInstance().executeOrQueue(() -> {
                    PushwooshMessagingServiceHelper.sendPushStat(pushBundle);
                });
            } else {
                PWLog.warn("/pushStat request was not sent, as the push was not received from Pushwoosh");
            }
        } catch (Exception e) {
            PWLog.error("Pushwoosh", "can't send push stat", e);
        }
    }
}
