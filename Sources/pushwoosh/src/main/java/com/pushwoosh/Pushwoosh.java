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
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.network.ServerCommunicationManager;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.LocalNotification;
import com.pushwoosh.notification.LocalNotificationRequest;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationManager;
import com.pushwoosh.notification.event.RegistrationSuccessEvent;
import com.pushwoosh.repository.PushwooshRepository;
import com.pushwoosh.repository.RegistrationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.tags.TagsBundle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final GDPRManager gdprManager;
    private final InAppRepository inAppRepository;
    private final PushRegistrarHelper pushRegistrarHelper;
    private final ServerCommunicationManager serverCommunicationManager;
    private Subscription<RegistrationSuccessEvent> subscriberRegister;
    private AtomicBoolean isSubscriptionSegmentsCasePresented;

    private Pushwoosh() {
        PushwooshPlatform pushwooshPlatform = PushwooshPlatform.getInstance();
        if (pushwooshPlatform == null) {
            PushwooshPlatform.notifyNotInitialized();
            notificationManager = null;
            pushwooshRepository = null;
            gdprManager = null;
            inAppRepository = null;
            pushRegistrarHelper = null;
            serverCommunicationManager = null;
        } else {
            notificationManager = pushwooshPlatform.notificationManager();
            pushwooshRepository = pushwooshPlatform.pushwooshRepository();
            gdprManager = pushwooshPlatform.getGdprManager();
            isSubscriptionSegmentsCasePresented = new AtomicBoolean();
            inAppRepository = InAppModule.getInAppRepository();
            pushRegistrarHelper = pushwooshPlatform.getPushRegistrarHelper();
            serverCommunicationManager = pushwooshPlatform.getServerCommunicationManager();
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
        if (pushwooshRepository != null)
            return RepositoryModule.getRegistrationPreferences().applicationId().get();
        return "";
    }

    /**
     * @return Current Pushwoosh application code
     * @deprecated
     * @see #getApplicationCode()
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
        if (notificationManager != null)
            notificationManager.setAppId(appId);
    }

    /**
     * Sets FCM/GCM sender Id
     * (Alternative for "com.pushwoosh.senderid" metadata in AndroidManifest.xml)
     *
     * @param senderId GCM/FCM sender id
     */
    public void setSenderId(@NonNull String senderId) {
        if (notificationManager != null)
            notificationManager.setSenderId(senderId);
    }

    /**
     * @return Current GCM/FCM sender id
     */
    public String getSenderId() {
        if (notificationManager != null)
            return RepositoryModule.getRegistrationPreferences().projectId().get();
        return "";
    }

    /**
     * @return Pushwoosh HWID associated with current device
     */
    @NonNull
    public String getHwid() {
        if (notificationManager != null)
            return pushwooshRepository.getHwid();
        return "";
    }

    /**
     * @return Push notification token or null if device is not registered yet.
     */
    @Nullable
    public String getPushToken() {
        if (notificationManager != null)
            return notificationManager.getPushToken();
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
        RegistrationPrefs preferences = RepositoryModule.getRegistrationPreferences();
        preferences.setLanguage(language);
    }

    public String getLanguage() {
        if (pushwooshRepository != null)
            return RepositoryModule.getRegistrationPreferences().language().get();
        return "";
    }

    public void requestNotificationPermission() {
        PushwooshNotificationManager.requestNotificationPermission();
    }

    /**
     * @see <a href="#registerForPushNotifications(Callback)">registerForPushNotifications(Callback)</a>
     */
    public void registerForPushNotifications() {
        registerForPushNotifications(null);
    }

    /**
     * Registers device for push notifications
     *
     * @param callback push registration callback
     */
    public void registerForPushNotifications(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        registerForPushNotificationsInternal(callback, true);
    }

    public void registerForPushNotificationsWithoutPermission(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        registerForPushNotificationsInternal(callback, false);
    }

    private void registerForPushNotificationsInternal(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback, boolean shouldRequestPermission) {
        if (RepositoryModule.getNotificationPreferences() != null &&
                !RepositoryModule.getNotificationPreferences().isServerCommunicationAllowed().get()) {
            String error = "Communication with Pushwoosh is disabled. You have to enable the server" +
                    " communication to register for push notifications. " +
                    "To enable the server communication use startServerCommunication method.";
            if (callback != null) {
                callback.process(Result.fromException(new RegisterForPushNotificationsException(error)));
            } else {
                PWLog.error(error);
            }
            return;
        }
        if (notificationManager != null) {
            notificationManager.registerForPushNotifications(callback, shouldRequestPermission);
        }
    }

    private void subscribeRegisterFromInApp(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        if (callback != null) {
            subscriberRegister = EventBus.subscribe(RegistrationSuccessEvent.class, event -> {
                unSubscribeRegisterEvent();
                callback.process(Result.fromData(event.getData()));
            });
        }
    }

    private void unSubscribeRegisterEvent() {
        if (subscriberRegister != null) {
            subscriberRegister.unsubscribe();
            subscriberRegister = null;
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
        if (RepositoryModule.getNotificationPreferences() != null &&
                !RepositoryModule.getNotificationPreferences().isServerCommunicationAllowed().get()) {
            String error = "Communication with Pushwoosh is disabled. You have to enable the server" +
                    " communication to unregister from push notifications. " +
                    "To enable the server communication use startServerCommunication method.";
            if (callback != null) {
                callback.process(Result.fromException(new UnregisterForPushNotificationException(error)));
            } else {
                PWLog.error(error);
            }
            return;
        }
        if (notificationManager != null)
            notificationManager.unregisterForPushNotifications(callback);
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
     * @param tags     {@link com.pushwoosh.tags.TagsBundle application tags bundle}
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
     * @param email user email
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
        if (pushwooshRepository != null)
            pushwooshRepository.sendTags(tags, callback);
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
     * @param email user email
     * @param callback sendEmailTags operation callback
     */

    public void setEmailTags(@NonNull TagsBundle emailTags, String email, Callback<Void, PushwooshException> callback) {
        if (pushwooshRepository != null)
            pushwooshRepository.sendEmailTags(emailTags, email, callback);
    }

    /**
     * @deprecated As of release 6.0, replaced by {@link #setTags(TagsBundle)} }
     * @see #setTags(TagsBundle)
     */
    @Deprecated
    public void sendTags(@NonNull TagsBundle tags) {
        setTags(tags);
    }

    /**
     * @param tags     {@link com.pushwoosh.tags.TagsBundle application tags bundle}
     * @param callback sendTags operation callback
     * @deprecated As of release 6.0, replaced by {@link #setTags(TagsBundle, Callback)} }
     * @see #setTags(TagsBundle, Callback)
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
     *            // tags sucessfully received
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
        if (pushwooshRepository != null)
            pushwooshRepository.getTags(callback);
    }

    /**
     * Sends In-App purchase statistics. Purchase information is stored in "In-app Product", "In-app Purchase" and "Last In-app Purchase date" default tags.
     *
     * @param sku      purchased product ID
     * @param price    price of the product
     * @param currency currency of the price (ex: “USD”)
     */
    public void sendInappPurchase(@NonNull String sku, @NonNull BigDecimal price, @NonNull String currency) {
        if (pushwooshRepository != null)
            pushwooshRepository.sendInappPurchase(sku, price, currency, new Date());
    }

    /**
     * @return Launch notification data or null.
     */
    @Nullable
    public PushMessage getLaunchNotification() {
        if (notificationManager != null)
            return notificationManager.getLaunchNotification();
        return null;
    }

    /**
     * reset {@link #getLaunchNotification()} to return null.
     */
    public void clearLaunchNotification() {
        if (notificationManager != null)
            notificationManager.clearLaunchNotification();
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
    @NonNull
    public LocalNotificationRequest scheduleLocalNotification(LocalNotification notification) {
        if (notificationManager != null)
            return notificationManager.scheduleLocalNotification(notification);
        return null;
    }

    /**
     * Gets push notification history. History contains both remote and local notifications.
     *
     * @return Push history as List of {@link com.pushwoosh.notification.PushMessage}. Maximum of {@link #PUSH_HISTORY_CAPACITY} pushes are returned
     */
    @NonNull
    public List<PushMessage> getPushHistory() {
        if (pushwooshRepository != null)
            return pushwooshRepository.getPushHistory();
        return new ArrayList<>();
    }

    /**
     * Clears push history. Usually called after {@link #getPushHistory()}.
     */
    public void clearPushHistory() {
        if (pushwooshRepository != null)
            RepositoryModule.getNotificationPreferences().pushHistory().clear();
    }

    /**
     * Informs the Pushwoosh about the app being launched. Usually called internally by SDK.
     */
    public void sendAppOpen() {
        if (pushwooshRepository != null) {
            pushwooshRepository.sendAppOpen();
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
     * @param userId user identifier
     * @param callback setUserId operation callback
     */
    public void setUserId(@NonNull String userId, Callback<Boolean, SetUserIdException> callback) {
        if (serverCommunicationManager == null)
        {
            if (callback != null) {
                callback.process(Result.fromException(new SetUserIdException("Pushwoosh platform is not initialized")));
            }
            return;
        }

        if (!serverCommunicationManager.isServerCommunicationAllowed()) {
            return;
        }
        String oldUserId = RepositoryModule.getRegistrationPreferences().userId().get();

        if (TextUtils.equals(userId, oldUserId)) {
            if (callback != null) {
                callback.process(Result.fromData(true));
            }
            return;
        }

        if (!TextUtils.isEmpty(userId)) {
            RepositoryModule.getRegistrationPreferences().userId().set(userId);
        }
        inAppRepository.setUserId(userId, callback);
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
     * @param userId user identifier
     * @param emails user's emails array list
     * @param callback setUser operation callback
     */

    public void setUser(@NonNull String userId, @NonNull List<String> emails, Callback<Boolean, SetUserException> callback) {
        if (!TextUtils.isEmpty(userId)) {
            RepositoryModule.getRegistrationPreferences().userId().set(userId);
        }

        inAppRepository.setUser(userId, emails, callback);
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
     * @param emails user's emails array list
     * @param callback setEmail operation callback
     */

    public void setEmail(@NonNull List<String> emails, Callback<Boolean, SetEmailException> callback) {
        if (emails.isEmpty()) {
            return;
        }

        inAppRepository.setEmail(emails, callback);
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
     * @param email user's email string
     * @param callback setEmail operation callback
     */

    public void setEmail(@NonNull String email, Callback<Boolean, SetEmailException> callback) {
        if (TextUtils.isEmpty(email)) {
            return;
        }
        ArrayList<String> list = new ArrayList<>();
        list.add(email);

        inAppRepository.setEmail(list, callback);
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
        inAppRepository.mergeUserId(oldUserId, newUserId, doMerge, callback);
    }

    /**
     * @return current user id
     * @see #setUserId(String)
     */
    @Nullable
    public String getUserId() {
        return RepositoryModule.getRegistrationPreferences().userId().get();
    }

    /**
     * Enables Huawei push messaging in plugin-based applications. This method gives
     * no effect if it is called in a native application.
     */
    public void enableHuaweiPushNotifications() {
        if (pushRegistrarHelper != null) {
            pushRegistrarHelper.enableHuaweiPushNotifications();
        }
    }

    public void enableXiaomiPushNotifications() {
        if (pushRegistrarHelper != null) {
            pushRegistrarHelper.enableXiaomiPushNotifications();
        }
    }

    /**
     * Starts communication with Pushwoosh server.
     */
    public void startServerCommunication() {
        if (serverCommunicationManager != null) {
            serverCommunicationManager.startServerCommunication();
        }
    }

    /**
     * Stops communication with Pushwoosh server.
     */
    public void stopServerCommunication() {
        if (serverCommunicationManager != null) {
            serverCommunicationManager.stopServerCommunication();
        }
    }

    /**
     * Sends push message delivery statistics to Pushwoosh server. This method
     * is called internally by Pushwoosh SDK.
     * CAUTION: Usage of this method without a clear purpose will break statistics in the Control panel
     * @param pushBundle Bundle of a received push notification
     */
    private void sendMessageDelivery(Bundle pushBundle) {
        if (pushBundle.containsKey("pw_msg")) {
            PushwooshMessagingServiceHelper.sendMessageDeliveryEvent(pushBundle);
        } else {
            PWLog.warn("/messageDeliveryEvent request was not sent, as the push was not received from Pushwoosh");
        }
    }

    /**
     * Sends push message opened statistics to Pushwoosh server. This method
     * is called internally by Pushwoosh SDK.
     * CAUTION: Usage of this method without a clear purpose will break statistics in the Control panel
     * @param pushBundle Bundle of a received push message
     */
    private void sendPushStat(Bundle pushBundle) {
        if (pushBundle.containsKey("pw_msg")) {
            PushwooshMessagingServiceHelper.sendPushStat(pushBundle);
        } else {
            PWLog.warn("/pushStat request was not sent, as the push was not received from Pushwoosh");
        }
    }
}
