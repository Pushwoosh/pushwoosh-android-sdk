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
 * Main entry point for the Pushwoosh SDK.
 * <p>
 * Pushwoosh is a customer engagement platform that helps you turn user data into high-converting
 * omnichannel campaigns. Build personalized customer journeys using advanced segmentation, behavior
 * tracking, and automated messaging across push notifications, email, SMS, and WhatsApp.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Push Notifications - Send rich push notifications with images, actions, and deep links</li>
 * <li>User Segmentation - Tag users and create targeted campaigns based on behavior and preferences</li>
 * <li>Cross-Device Tracking - Track users across multiple devices using User ID</li>
 * <li>Local Notifications - Schedule notifications to be shown at specific times</li>
 * <li>In-App Messages - Display rich in-app content to engaged users</li>
 * <li>Analytics - Track push delivery, opens, and user engagement</li>
 * <li>Multichannel Campaigns - Send messages via push, email, SMS, and WhatsApp</li>
 * </ul>
 * <p>
 * <b>Quick Start:</b>
 * <pre>
 * {@code
 *   // 1. Get Pushwoosh instance (available after SDK initialization)
 *   Pushwoosh pushwoosh = Pushwoosh.getInstance();
 *
 *   // 2. Register for push notifications
 *   pushwoosh.registerForPushNotifications((result) -> {
 *       if (result.isSuccess()) {
 *           Log.d("App", "Push registered: " + result.getData().getToken());
 *       } else {
 *           Log.e("App", "Registration failed: " + result.getException());
 *       }
 *   });
 *
 *   // 3. Set user tags for targeting
 *   TagsBundle tags = new TagsBundle.Builder()
 *       .putString("Name", "John Doe")
 *       .putInt("Age", 25)
 *       .putString("Subscription", "premium")
 *       .build();
 *   pushwoosh.setTags(tags);
 *
 *   // 4. Set user ID for cross-device tracking
 *   pushwoosh.setUserId("user_12345");
 * }
 * </pre>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>Always call {@link #getInstance()} to access the SDK instance</li>
 * <li>On Android 13+, notification permission is requested automatically during {@link #registerForPushNotifications(Callback)}</li>
 * <li>Set user tags to enable targeted campaigns and personalization</li>
 * <li>Use {@link #setUserId(String)} to track users across multiple devices</li>
 * <li>Handle push notifications using {@link #getLaunchNotification()} for deep linking</li>
 * </ul>
 *
 * @see #registerForPushNotifications()
 * @see #setTags(TagsBundle)
 * @see #setUserId(String)
 * @see #scheduleLocalNotification(LocalNotification)
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
     * Returns the shared instance of Pushwoosh SDK.
     * <p>
     * This is the main entry point for all Pushwoosh SDK operations. The instance is created
     * automatically when the SDK is initialized and remains available throughout the application lifecycle.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   Pushwoosh pushwoosh = Pushwoosh.getInstance();
     *   pushwoosh.registerForPushNotifications();
     * }
     * </pre>
     *
     * @return Pushwoosh shared instance
     */
    @NonNull
    public static Pushwoosh getInstance() {
        return INSTANCE;
    }

    /**
     * Returns the current Pushwoosh application code.
     * <p>
     * This method retrieves the application code that was set either via {@link #setAppId(String)}
     * or through the "com.pushwoosh.appid" metadata in AndroidManifest.xml. The application code
     * uniquely identifies your app in the Pushwoosh system and can be found in the Pushwoosh Control Panel.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Get current application code for logging or debugging
     *   String appCode = Pushwoosh.getInstance().getApplicationCode();
     *   Log.d("App", "Current Pushwoosh App Code: " + appCode);
     *
     *   // Verify app code matches expected value
     *   if (!"XXXXX-XXXXX".equals(appCode)) {
     *       Log.w("App", "Warning: Unexpected app code configured");
     *   }
     * }
     * </pre>
     *
     * @return Current Pushwoosh application code, or empty string if not set or SDK not initialized
     * @see #setAppId(String)
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
     * Associates current application with the given Pushwoosh application code.
     * <p>
     * This method provides a runtime alternative to defining "com.pushwoosh.appid" metadata in AndroidManifest.xml.
     * The application code can be found in your Pushwoosh Control Panel.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   Pushwoosh.getInstance().setAppId("XXXXX-XXXXX");
     * }
     * </pre>
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
     * Sets the FCM/GCM sender ID for push notifications.
     * <p>
     * This method provides a runtime alternative to defining "com.pushwoosh.senderid" metadata in AndroidManifest.xml.
     * The sender ID can be found in your Firebase Console project settings.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   Pushwoosh.getInstance().setSenderId("123456789012");
     * }
     * </pre>
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
     * Returns the current GCM/FCM sender ID.
     * <p>
     * This method retrieves the sender ID that was set either via {@link #setSenderId(String)}
     * or through the "com.pushwoosh.senderid" metadata in AndroidManifest.xml.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   String senderId = Pushwoosh.getInstance().getSenderId();
     *   Log.d("Pushwoosh", "Sender ID: " + senderId);
     * }
     * </pre>
     *
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
     * Returns the Pushwoosh Hardware ID (HWID) associated with the current device.
     * <p>
     * HWID is a unique identifier generated by Pushwoosh SDK for each device installation.
     * It remains constant across app reinstalls and is used to identify the device in the Pushwoosh system.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   String hwid = Pushwoosh.getInstance().getHwid();
     *   Log.d("Pushwoosh", "Device HWID: " + hwid);
     * }
     * </pre>
     *
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
     * Returns the current push notification token.
     * <p>
     * This method returns the FCM/GCM token obtained after successful registration.
     * The token may be null if the device hasn't been registered yet or if registration is in progress.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   String pushToken = Pushwoosh.getInstance().getPushToken();
     *   if (pushToken != null && !pushToken.isEmpty()) {
     *       Log.d("Pushwoosh", "Push token: " + pushToken);
     *   } else {
     *       Log.d("Pushwoosh", "Device not registered yet");
     *   }
     * }
     * </pre>
     *
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
     * Sets a custom application language for push notification localization.
     * <p>
     * By default, the SDK uses the device language. This method allows you to override the language
     * for targeting localized push notifications. Set to null to revert to device language.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Set custom language
     *   Pushwoosh.getInstance().setLanguage("es");
     *
     *   // Revert to device language
     *   Pushwoosh.getInstance().setLanguage(null);
     * }
     * </pre>
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

    /**
     * Returns the current language code used for push notification localization.
     * <p>
     * This method returns either the custom language set via {@link #setLanguage(String)}
     * or the device language if no custom language was set.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   String language = Pushwoosh.getInstance().getLanguage();
     *   Log.d("Pushwoosh", "Current language: " + language);
     * }
     * </pre>
     *
     * @return Current language code in ISO-639-1 format
     */
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

    /**
     * Requests notification permission from the user.
     * <p>
     * On Android 13 (API level 33) and above, this method prompts the user to grant notification permission.
     * On earlier Android versions, this method has no effect as notification permission is granted by default.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Request permission before registering for push notifications
     *   Pushwoosh.getInstance().requestNotificationPermission();
     * }
     * </pre>
     */
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
     * Registers the device for push notifications without a callback.
     * <p>
     * This is a convenience method that calls {@link #registerForPushNotifications(Callback)} with a null callback.
     * Use this method when you don't need to handle registration results.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Simple registration without callback
     *   Pushwoosh.getInstance().registerForPushNotifications();
     * }
     * </pre>
     *
     * @see #registerForPushNotifications(Callback)
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
     * Registers the device for push notifications with a callback.
     * <p>
     * This method initiates the registration process with FCM/GCM and registers the device
     * with Pushwoosh. The callback provides information about the registration result including
     * the push token and notification permission status.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Register for push notifications in Application onCreate or MainActivity
     *   @Override
     *   protected void onCreate(Bundle savedInstanceState) {
     *       super.onCreate(savedInstanceState);
     *       setContentView(R.layout.activity_main);
     *
     *       // Register for push notifications (permission requested automatically on Android 13+)
     *       Pushwoosh.getInstance().registerForPushNotifications((result) -> {
     *           if (result.isSuccess()) {
     *               String pushToken = result.getData().getToken();
     *               boolean notificationsEnabled = result.getData().isEnabled();
     *               Log.d("App", "Push registration successful. Token: " + pushToken);
     *
     *               // Optionally store registration status
     *               SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
     *               prefs.edit().putBoolean("push_registered", true).apply();
     *           } else {
     *               Exception exception = result.getException();
     *               Log.e("App", "Push registration failed: " + exception.getMessage());
     *               // Show user-friendly error message
     *               Toast.makeText(this, "Unable to enable notifications", Toast.LENGTH_SHORT).show();
     *           }
     *       });
     *   }
     * }
     * </pre>
     *
     * @param callback push registration callback
     */
    public void registerForPushNotifications(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        registerForPushNotificationsInternal(callback, true, null);
    }

    /**
     * Registers the device for push notifications and sets tags in a single request.
     * <p>
     * This method combines registration and tag setting operations, which is more efficient
     * than calling {@link #registerForPushNotifications(Callback)} and {@link #setTags(TagsBundle)} separately.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Register with user profile data from onboarding flow
     *   private void registerWithUserProfile(User user) {
     *       TagsBundle userTags = new TagsBundle.Builder()
     *           .putString("Name", user.getName())
     *           .putInt("Age", user.getAge())
     *           .putString("Subscription", user.getSubscriptionType()) // "free", "premium"
     *           .putString("Language", user.getPreferredLanguage()) // "en", "es", "fr"
     *           .putBoolean("Marketing_Consent", user.hasMarketingConsent())
     *           .build();
     *
     *       Pushwoosh.getInstance().registerForPushNotificationsWithTags((result) -> {
     *           if (result.isSuccess()) {
     *               Log.d("App", "User registered with profile data");
     *               navigateToHomeScreen();
     *           } else {
     *               Log.e("App", "Registration failed: " + result.getException().getMessage());
     *               showRetryDialog();
     *           }
     *       }, userTags);
     *   }
     * }
     * </pre>
     *
     * @param callback push registration callback
     * @param tags     tags to be set when registering for pushes
     */
    public void registerForPushNotificationsWithTags(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback, TagsBundle tags) {
        registerForPushNotificationsInternal(callback, true, tags);
    }

    /**
     * Registers the device for push notifications without requesting notification permission.
     * <p>
     * This method is useful when you want to handle notification permission request yourself
     * or when you've already requested the permission separately. On Android 13+ (API 33+),
     * if notification permission is not granted, the registration will still succeed but
     * notifications won't be displayed to the user.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Custom permission flow - register after user grants permission
     *   private void onNotificationPermissionGranted() {
     *       // Permission already handled by custom flow
     *       Pushwoosh.getInstance().registerForPushNotificationsWithoutPermission((result) -> {
     *           if (result.isSuccess()) {
     *               String token = result.getData().getToken();
     *               Log.d("App", "Registered with token: " + token);
     *               updateSubscriptionStatus(true);
     *           } else {
     *               Log.e("App", "Registration failed: " + result.getException().getMessage());
     *           }
     *       });
     *   }
     *
     *   // Silent registration for background services
     *   private void registerSilently() {
     *       // Register without showing permission dialog
     *       Pushwoosh.getInstance().registerForPushNotificationsWithoutPermission((result) -> {
     *           if (result.isSuccess()) {
     *               // Device registered, can receive data pushes
     *               Log.d("App", "Background registration complete");
     *           }
     *       });
     *   }
     * }
     * </pre>
     *
     * @param callback push registration callback
     * @see #registerForPushNotifications(Callback)
     * @see #requestNotificationPermission()
     */
    public void registerForPushNotificationsWithoutPermission(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback) {
        registerForPushNotificationsInternal(callback, false, null);
    }

    /**
     * Registers the device for push notifications with tags without requesting notification permission.
     * <p>
     * This method combines registration and tag setting while skipping the notification permission request.
     * Useful when you want to handle the permission request yourself or have already requested it separately.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Register with user data after custom permission flow
     *   private void completeRegistrationWithUserData(User user, boolean hasPermission) {
     *       TagsBundle userTags = new TagsBundle.Builder()
     *           .putString("Name", user.getName())
     *           .putString("Account_Type", user.getAccountType())
     *           .putBoolean("Notification_Permission", hasPermission)
     *           .putDate("Registration_Date", new Date())
     *           .build();
     *
     *       // Register without triggering permission dialog (already handled)
     *       Pushwoosh.getInstance().registerForPushNotificationsWithTagsWithoutPermission((result) -> {
     *           if (result.isSuccess()) {
     *               Log.d("App", "User registered with profile data");
     *               if (hasPermission) {
     *                   showWelcomeNotification();
     *               }
     *               navigateToHome();
     *           } else {
     *               Log.e("App", "Registration failed: " + result.getException().getMessage());
     *               showRetryOption();
     *           }
     *       }, userTags);
     *   }
     * }
     * </pre>
     *
     * @param callback    push registration callback
     * @param tagsBundle  tags to be set when registering for pushes
     * @see #registerForPushNotificationsWithTags(Callback, TagsBundle)
     * @see #requestNotificationPermission()
     */
    public void registerForPushNotificationsWithTagsWithoutPermission(Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> callback, TagsBundle tagsBundle) {
        registerForPushNotificationsInternal(callback, false, tagsBundle);
    }

    /**
     * Registers the device using an existing FCM/GCM token.
     * <p>
     * This method is useful when you already have a push token obtained from Firebase
     * and want to register it with Pushwoosh without going through the full registration flow.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Assuming you obtained the token from FirebaseMessaging
     *   FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
     *       if (task.isSuccessful()) {
     *           String token = task.getResult();
     *           Pushwoosh.getInstance().registerExistingToken(token, (result) -> {
     *               if (result.isSuccess()) {
     *                   Log.d("Pushwoosh", "Token registered successfully");
     *               } else {
     *                   Log.e("Pushwoosh", "Failed: " + result.getException().getMessage());
     *               }
     *           });
     *       }
     *   });
     * }
     * </pre>
     *
     * @param token    FCM/GCM push token
     * @param callback registration callback
     */
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

    /**
     * Adds an alternative Pushwoosh application code for device registration.
     * <p>
     * This method allows registering the device with multiple Pushwoosh applications simultaneously.
     * This is useful for white-label apps, multi-brand applications, or when you need to send pushes
     * from different Pushwoosh applications to the same device.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Primary app code is set in AndroidManifest.xml or via setAppId()
     *   Pushwoosh.getInstance().setAppId("XXXXX-XXXXX");
     *
     *   // Add alternative app codes for white-label brands
     *   Pushwoosh.getInstance().addAlternativeAppCode("BRAND1-APPID");
     *   Pushwoosh.getInstance().addAlternativeAppCode("BRAND2-APPID");
     *
     *   // Device will now receive pushes from all three applications
     *   Pushwoosh.getInstance().registerForPushNotifications();
     * }
     * </pre>
     *
     * @param appCode Alternative Pushwoosh application code to add
     * @see #setAppId(String)
     * @see #resetAlternativeAppCodes()
     */
    public void addAlternativeAppCode(String appCode) {
        RepositoryModule.getRegistrationPreferences().registerAlternativeAppCode(appCode);
        PWLog.info("Added "+ appCode + " as an alternative app code for registration");
    }

    /**
     * Removes all alternative application codes previously added via {@link #addAlternativeAppCode(String)}.
     * <p>
     * After calling this method, the device will only be registered with the primary application code
     * set via {@link #setAppId(String)} or AndroidManifest.xml.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Clear all alternative app codes
     *   Pushwoosh.getInstance().resetAlternativeAppCodes();
     *
     *   // Re-register to update on server
     *   Pushwoosh.getInstance().registerForPushNotifications();
     * }
     * </pre>
     *
     * @see #addAlternativeAppCode(String)
     * @see #setAppId(String)
     */
    public void resetAlternativeAppCodes() {
        RepositoryModule.getRegistrationPreferences().resetAlternativeAppCodes();
    }

    /**
     * Registers a WhatsApp number for the current user.
     * <p>
     * This method associates a WhatsApp number with the device, allowing you to send
     * WhatsApp messages through Pushwoosh multichannel campaigns.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Register WhatsApp number with country code
     *   Pushwoosh.getInstance().registerWhatsappNumber("+1234567890");
     * }
     * </pre>
     *
     * @param number WhatsApp phone number with country code (e.g., "+1234567890")
     */
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

    /**
     * Registers an SMS number for the current user.
     * <p>
     * This method associates an SMS phone number with the device, allowing you to send
     * SMS messages through Pushwoosh multichannel campaigns.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Register SMS number with country code
     *   Pushwoosh.getInstance().registerSMSNumber("+1234567890");
     * }
     * </pre>
     *
     * @param number SMS phone number with country code (e.g., "+1234567890")
     */
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

    /**
     * Controls whether push notifications should be displayed when the app is in foreground.
     * <p>
     * By default, push notifications are shown even when the app is in foreground. Set this to false
     * if you want to suppress notification display when the app is active and handle them programmatically instead.
     * This is useful when you want to show in-app UI instead of system notifications while the app is open.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Suppress notifications when app is in foreground
     *   public class MyApplication extends Application {
     *       @Override
     *       public void onCreate() {
     *           super.onCreate();
     *
     *           // Don't show system notifications when app is active
     *           Pushwoosh.getInstance().setShowPushnotificationAlert(false);
     *       }
     *   }
     *
     *   // Handle foreground pushes programmatically using NotificationServiceExtension
     *   public class MyNotificationService extends NotificationServiceExtension {
     *       @Override
     *       public boolean onMessageReceived(PushMessage message) {
     *           if (isAppInForeground()) {
     *               // Show custom in-app UI instead of notification
     *               showInAppMessage(message.getMessage());
     *               return true; // Prevent default notification
     *           }
     *           return false; // Show default notification when app is background
     *       }
     *   }
     *
     *   // Re-enable notifications based on user preference
     *   private void updateNotificationSettings(boolean showNotifications) {
     *       Pushwoosh.getInstance().setShowPushnotificationAlert(showNotifications);
     *       Log.d("App", "Foreground notifications " + (showNotifications ? "enabled" : "disabled"));
     *   }
     * }
     * </pre>
     *
     * @param showAlert true to show notifications when app is in foreground (default), false to suppress them
     */
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
     * Unregisters the device from push notifications without a callback.
     * <p>
     * This is a convenience method that calls {@link #unregisterForPushNotifications(Callback)}
     * with a null callback. Use this when you don't need to handle the unregistration result.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Simple unregistration without callback
     *   Pushwoosh.getInstance().unregisterForPushNotifications();
     * }
     * </pre>
     *
     * @see #unregisterForPushNotifications(Callback)
     */
    public void unregisterForPushNotifications() {
        unregisterForPushNotifications(null);
    }

    /**
     * Unregisters the device from push notifications with a callback.
     * <p>
     * This method unregisters the device from Pushwoosh, stopping all push notifications.
     * The device will need to call {@link #registerForPushNotifications()} again to receive pushes.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   Pushwoosh.getInstance().unregisterForPushNotifications((result) -> {
     *       if (result.isSuccess()) {
     *           Log.d("Pushwoosh", "Successfully unregistered");
     *       } else {
     *           Exception exception = result.getException();
     *           Log.e("Pushwoosh", "Failed to unregister: " + exception.getMessage());
     *       }
     *   });
     * }
     * </pre>
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
     *   // Update user profile after login
     *   private void updateUserProfile(User user) {
     *       TagsBundle profileTags = new TagsBundle.Builder()
     *           .putString("Name", user.getFullName())
     *           .putInt("Age", user.getAge())
     *           .putString("Gender", user.getGender()) // "male", "female", "other"
     *           .putString("City", user.getCity())
     *           .putString("Subscription_Tier", user.getSubscriptionTier()) // "free", "basic", "premium"
     *           .putBoolean("Email_Verified", user.isEmailVerified())
     *           .build();
     *
     *       Pushwoosh.getInstance().setTags(profileTags);
     *   }
     *
     *   // Update user preferences
     *   private void saveUserPreferences(UserPreferences prefs) {
     *       TagsBundle preferencesTags = new TagsBundle.Builder()
     *           .putString("Favorite_Category", prefs.getFavoriteCategory()) // "electronics", "fashion", "sports"
     *           .putString("Language_Preference", prefs.getLanguage()) // "en", "es", "fr"
     *           .putBoolean("Push_Notifications_Enabled", prefs.isPushEnabled())
     *           .putBoolean("Email_Notifications_Enabled", prefs.isEmailEnabled())
     *           .putStringList("Interests", prefs.getInterests()) // ["tech", "gaming", "travel"]
     *           .build();
     *
     *       Pushwoosh.getInstance().setTags(preferencesTags);
     *   }
     *
     *   // Track user activity
     *   private void trackUserActivity(UserActivity activity) {
     *       TagsBundle activityTags = new TagsBundle.Builder()
     *           .putDate("Last_Login", new Date())
     *           .putInt("Total_Purchases", activity.getTotalPurchases())
     *           .putDouble("Total_Spent", activity.getTotalSpent())
     *           .putDate("Last_Purchase_Date", activity.getLastPurchaseDate())
     *           .putString("Last_Viewed_Product", activity.getLastViewedProductId())
     *           .build();
     *
     *       Pushwoosh.getInstance().setTags(activityTags);
     *   }
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
     *   // Update user subscription status with callback
     *   private void upgradeToPremium(User user) {
     *       TagsBundle subscriptionTags = new TagsBundle.Builder()
     *           .putString("Subscription_Tier", "premium")
     *           .putDate("Premium_Since", new Date())
     *           .putBoolean("Is_Premium", true)
     *           .putInt("Premium_Credits", 1000)
     *           .build();
     *
     *       Pushwoosh.getInstance().setTags(subscriptionTags, (result) -> {
     *           if (result.isSuccess()) {
     *               Log.d("App", "Premium subscription tags updated successfully");
     *               // Show success message to user
     *               Toast.makeText(this, "Welcome to Premium!", Toast.LENGTH_SHORT).show();
     *               // Navigate to premium features
     *               startActivity(new Intent(this, PremiumFeaturesActivity.class));
     *           } else {
     *               Log.e("App", "Failed to update subscription tags: " + result.getException().getMessage());
     *               // Tags will be retried on next app launch automatically
     *               Toast.makeText(this, "Subscription activated. Some features may take a moment to sync.", Toast.LENGTH_LONG).show();
     *           }
     *       });
     *   }
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
     *   // Retrieve and use user profile tags to personalize UI
     *   private void loadUserProfile() {
     *       Pushwoosh.getInstance().getTags((result) -> {
     *           if (result.isSuccess()) {
     *               TagsBundle tags = result.getData();
     *
     *               // Read user profile data
     *               String userName = tags.getString("Name", "Guest");
     *               int userAge = tags.getInt("Age", 0);
     *               String subscriptionTier = tags.getString("Subscription_Tier", "free");
     *               boolean isPremium = "premium".equals(subscriptionTier);
     *
     *               // Update UI based on tags
     *               updateWelcomeMessage("Welcome back, " + userName + "!");
     *               if (isPremium) {
     *                   showPremiumFeatures();
     *               } else {
     *                   showUpgradePrompt();
     *               }
     *
     *               // Check user preferences
     *               List<String> interests = tags.getStringList("Interests");
     *               if (interests != null && !interests.isEmpty()) {
     *                   showPersonalizedContent(interests);
     *               }
     *
     *               Log.d("App", "User profile loaded: " + userName + ", tier: " + subscriptionTier);
     *           } else {
     *               Log.e("App", "Failed to retrieve tags: " + result.getException().getMessage());
     *               // Show default UI
     *               showDefaultContent();
     *           }
     *       });
     *   }
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
     * Sends in-app purchase statistics to Pushwoosh.
     * <p>
     * Purchase information is automatically stored in the following default tags:
     * <ul>
     * <li>"In-app Product" - product SKU</li>
     * <li>"In-app Purchase" - purchase amount</li>
     * <li>"Last In-app Purchase date" - purchase timestamp</li>
     * </ul>
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Track in-app purchase
     *   Pushwoosh.getInstance().sendInappPurchase(
     *       "premium_subscription",
     *       new BigDecimal("9.99"),
     *       "USD"
     *   );
     * }
     * </pre>
     *
     * @param sku      purchased product ID
     * @param price    price of the product
     * @param currency currency of the price (ex: "USD")
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
     * Returns the push notification that launched the application.
     * <p>
     * This method returns the push message data if the app was started by tapping a push notification.
     * Returns null if the app was launched normally or if {@link #clearLaunchNotification()} was called.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   @Override
     *   protected void onCreate(Bundle savedInstanceState) {
     *       super.onCreate(savedInstanceState);
     *       setContentView(R.layout.activity_main);
     *
     *       // Check if app was launched from a push notification
     *       PushMessage launchNotification = Pushwoosh.getInstance().getLaunchNotification();
     *       if (launchNotification != null) {
     *           // Extract custom data for deep linking
     *           String screen = launchNotification.getCustomData().getString("screen");
     *           String productId = launchNotification.getCustomData().getString("product_id");
     *           String articleId = launchNotification.getCustomData().getString("article_id");
     *
     *           Log.d("App", "Launched from push: " + launchNotification.getMessage());
     *
     *           // Navigate to specific screen based on push data
     *           if ("product_details".equals(screen) && productId != null) {
     *               // Open product details screen
     *               Intent intent = new Intent(this, ProductDetailsActivity.class);
     *               intent.putExtra("product_id", productId);
     *               startActivity(intent);
     *           } else if ("article".equals(screen) && articleId != null) {
     *               // Open article screen
     *               Intent intent = new Intent(this, ArticleActivity.class);
     *               intent.putExtra("article_id", articleId);
     *               startActivity(intent);
     *           } else if ("promotions".equals(screen)) {
     *               // Open promotions screen
     *               startActivity(new Intent(this, PromotionsActivity.class));
     *           }
     *
     *           // Clear to prevent reprocessing
     *           Pushwoosh.getInstance().clearLaunchNotification();
     *       }
     *   }
     * }
     * </pre>
     *
     * @return Launch notification data or null
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
     * Clears the launch notification data.
     * <p>
     * After calling this method, {@link #getLaunchNotification()} will return null until
     * the app is launched from another push notification. This is useful to prevent processing
     * the same launch notification multiple times.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   PushMessage launchNotification = Pushwoosh.getInstance().getLaunchNotification();
     *   if (launchNotification != null) {
     *       // Process the launch notification
     *       handlePushMessage(launchNotification);
     *       // Clear it to prevent reprocessing
     *       Pushwoosh.getInstance().clearLaunchNotification();
     *   }
     * }
     * </pre>
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
     *   // Schedule a reminder notification
     *   private void scheduleWorkoutReminder() {
     *       LocalNotification workoutReminder = new LocalNotification.Builder()
     *           .setMessage("Time for your daily workout!")
     *           .setTitle("Fitness Reminder")
     *           .setDelay(3600) // 1 hour from now (in seconds)
     *           .setCustomData(new Bundle()) // Optional custom data
     *           .build();
     *
     *       LocalNotificationRequest request = Pushwoosh.getInstance().scheduleLocalNotification(workoutReminder);
     *       Log.d("App", "Workout reminder scheduled with ID: " + request.getRequestId());
     *   }
     *
     *   // Schedule promotional notification
     *   private void scheduleFlashSaleNotification() {
     *       Bundle customData = new Bundle();
     *       customData.putString("screen", "flash_sale");
     *       customData.putString("sale_id", "flash_2024_01");
     *
     *       LocalNotification saleNotification = new LocalNotification.Builder()
     *           .setTitle("Flash Sale Alert!")
     *           .setMessage("50% off on selected items. Don't miss out!")
     *           .setDelay(86400) // 24 hours from now
     *           .setCustomData(customData)
     *           .build();
     *
     *       LocalNotificationRequest request = Pushwoosh.getInstance().scheduleLocalNotification(saleNotification);
     *       // Store request ID to cancel later if needed
     *       saveNotificationRequestId(request.getRequestId());
     *   }
     *
     *   // Schedule cart abandonment reminder
     *   private void scheduleCartReminder(int itemCount, double cartTotal) {
     *       LocalNotification cartReminder = new LocalNotification.Builder()
     *           .setTitle("Your cart is waiting")
     *           .setMessage("You have " + itemCount + " items worth $" + cartTotal + " in your cart")
     *           .setDelay(7200) // 2 hours from now
     *           .build();
     *
     *       Pushwoosh.getInstance().scheduleLocalNotification(cartReminder);
     *   }
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
     * Returns the push notification history.
     * <p>
     * This method retrieves a list of recently received push notifications, including both remote
     * and local notifications. The history is limited to {@link #PUSH_HISTORY_CAPACITY} most recent pushes.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   List<PushMessage> history = Pushwoosh.getInstance().getPushHistory();
     *   for (PushMessage message : history) {
     *       Log.d("Pushwoosh", "Message: " + message.getMessage());
     *       Log.d("Pushwoosh", "Received at: " + message.getTimestamp());
     *   }
     * }
     * </pre>
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
     * Clears the push notification history.
     * <p>
     * This method removes all stored push notifications from the history. Usually called
     * after processing the history retrieved via {@link #getPushHistory()}.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Get and process push history
     *   List<PushMessage> history = Pushwoosh.getInstance().getPushHistory();
     *   processHistory(history);
     *
     *   // Clear history after processing
     *   Pushwoosh.getInstance().clearPushHistory();
     * }
     * </pre>
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
     * Sends an application open event to Pushwoosh.
     * <p>
     * This method is usually called automatically by the SDK when the application launches.
     * However, in some custom integration scenarios, you may need to call it manually to ensure
     * proper tracking of app opens and session analytics.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Manually send app open event (usually not needed)
     *   Pushwoosh.getInstance().sendAppOpen();
     * }
     * </pre>
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
     * Sets the user identifier without a callback.
     * <p>
     * This is a convenience method that calls {@link #setUserId(String, Callback)} with a null callback.
     * The user identifier can be a Facebook ID, username, email, or any unique user ID that allows
     * data and events to be matched across multiple devices.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Set user ID after successful login
     *   private void onLoginSuccess(User user) {
     *       // Associate device with user account for cross-device tracking
     *       Pushwoosh.getInstance().setUserId(user.getUserId());
     *
     *       // Also update user profile tags
     *       TagsBundle userProfile = new TagsBundle.Builder()
     *           .putString("Name", user.getName())
     *           .putString("Email", user.getEmail())
     *           .putDate("Last_Login", new Date())
     *           .build();
     *       Pushwoosh.getInstance().setTags(userProfile);
     *
     *       Log.d("App", "User logged in: " + user.getUserId());
     *   }
     * }
     * </pre>
     *
     * @param userId user identifier
     * @see #setUserId(String, Callback)
     */
    public void setUserId(@NonNull String userId) {
        setUserId(userId, null);
    }

    /**
     * Sets the user identifier with a callback.
     * <p>
     * This method associates a user identifier with the current device. The user ID can be
     * a Facebook ID, username, email, or any unique identifier. This enables cross-device
     * tracking and allows you to target users across all their devices.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Set user ID with callback after authentication
     *   private void authenticateUser(String username, String password) {
     *       authService.login(username, password, new AuthCallback() {
     *           @Override
     *           public void onSuccess(User user) {
     *               // Set user ID in Pushwoosh with callback
     *               Pushwoosh.getInstance().setUserId(user.getUserId(), (result) -> {
     *                   if (result.isSuccess()) {
     *                       Log.d("App", "User ID set successfully for cross-device tracking");
     *
     *                       // Update additional user data
     *                       TagsBundle userTags = new TagsBundle.Builder()
     *                           .putString("Username", user.getUsername())
     *                           .putString("Account_Type", user.getAccountType())
     *                           .putDate("Last_Login", new Date())
     *                           .build();
     *                       Pushwoosh.getInstance().setTags(userTags);
     *
     *                       // Navigate to home screen
     *                       startActivity(new Intent(LoginActivity.this, HomeActivity.class));
     *                       finish();
     *                   } else {
     *                       Log.e("App", "Failed to set user ID: " + result.getException().getMessage());
     *                       // Continue anyway as this is not critical for login
     *                       startActivity(new Intent(LoginActivity.this, HomeActivity.class));
     *                       finish();
     *                   }
     *               });
     *           }
     *
     *           @Override
     *           public void onError(String error) {
     *               Toast.makeText(LoginActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
     *           }
     *       });
     *   }
     * }
     * </pre>
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
     * Sets the user identifier and registers associated email addresses without a callback.
     * <p>
     * This is a convenience method that calls {@link #setUser(String, List, Callback)} with a null callback.
     * This method combines setting a user ID with registering email addresses in a single operation.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Set user with multiple email addresses after registration
     *   private void completeUserRegistration(User user) {
     *       List<String> userEmails = new ArrayList<>();
     *       userEmails.add(user.getPrimaryEmail());
     *
     *       // Add work email if provided
     *       if (user.getWorkEmail() != null) {
     *           userEmails.add(user.getWorkEmail());
     *       }
     *
     *       // Set user ID and associate all email addresses
     *       Pushwoosh.getInstance().setUser(user.getUserId(), userEmails);
     *
     *       Log.d("App", "User registered with " + userEmails.size() + " email(s)");
     *   }
     * }
     * </pre>
     *
     * @param userId user identifier
     * @param emails user's emails array list
     * @see #setUser(String, List, Callback)
     */
    public void setUser(@NonNull String userId, @NonNull List<String> emails) {
        setUser(userId, emails, null);
    }

    /**
     * Sets the user identifier and registers associated email addresses with a callback.
     * <p>
     * This method sets a user identifier and associates one or more email addresses with the user.
     * The user ID can be a Facebook ID, username, or any unique identifier. This enables cross-device
     * tracking and email-based targeting for multichannel campaigns.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Set user with email addresses and handle result
     *   private void linkUserAccount(User user) {
     *       List<String> userEmails = new ArrayList<>();
     *       userEmails.add(user.getPrimaryEmail());
     *
     *       // Add secondary emails if available
     *       if (user.hasSecondaryEmails()) {
     *           userEmails.addAll(user.getSecondaryEmails());
     *       }
     *
     *       Pushwoosh.getInstance().setUser(user.getUserId(), userEmails, (result) -> {
     *           if (result.isSuccess()) {
     *               Log.d("App", "User account linked successfully with " + userEmails.size() + " email(s)");
     *
     *               // Update user profile with additional data
     *               TagsBundle profileTags = new TagsBundle.Builder()
     *                   .putString("Name", user.getName())
     *                   .putString("Account_Status", "verified")
     *                   .putDate("Account_Created", user.getCreatedAt())
     *                   .build();
     *               Pushwoosh.getInstance().setTags(profileTags);
     *
     *               // Show confirmation to user
     *               Toast.makeText(this, "Account setup complete!", Toast.LENGTH_SHORT).show();
     *           } else {
     *               Log.e("App", "Failed to link account: " + result.getException().getMessage());
     *               // Show error but allow user to continue
     *               Toast.makeText(this, "Account created. Email sync pending.", Toast.LENGTH_SHORT).show();
     *           }
     *       });
     *   }
     * }
     * </pre>
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
     * Registers a list of email addresses for the current user without a callback.
     * <p>
     * This is a convenience method that calls {@link #setEmail(List, Callback)} with a null callback.
     * Email addresses are used for email-based targeting in multichannel campaigns.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   List<String> emails = new ArrayList<>();
     *   emails.add("user@example.com");
     *   emails.add("user.work@company.com");
     *
     *   Pushwoosh.getInstance().setEmail(emails);
     * }
     * </pre>
     *
     * @param emails user's emails array list
     * @see #setEmail(List, Callback)
     */
    public void setEmail(@NonNull List<String> emails) {
        setEmail(emails, null);
    }

    /**
     * Registers a list of email addresses for the current user with a callback.
     * <p>
     * This method associates one or more email addresses with the current user/device,
     * enabling email-based targeting and multichannel campaigns through Pushwoosh.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   List<String> emails = new ArrayList<>();
     *   emails.add("user@example.com");
     *   emails.add("user.work@company.com");
     *
     *   Pushwoosh.getInstance().setEmail(emails, (result) -> {
     *       if (result.isSuccess()) {
     *           Log.d("Pushwoosh", "Emails registered successfully");
     *       } else {
     *           Exception exception = result.getException();
     *           Log.e("Pushwoosh", "Failed to register emails: " + exception.getMessage());
     *       }
     *   });
     * }
     * </pre>
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
     * Registers a single email address for the current user without a callback.
     * <p>
     * This is a convenience method that calls {@link #setEmail(String, Callback)} with a null callback.
     * The email address is used for email-based targeting in multichannel campaigns.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   Pushwoosh.getInstance().setEmail("user@example.com");
     * }
     * </pre>
     *
     * @param email user's email string
     * @see #setEmail(String, Callback)
     */
    public void setEmail(@NonNull String email) {
        setEmail(email, null);
    }

    /**
     * Registers a single email address for the current user with a callback.
     * <p>
     * This method associates an email address with the current user/device, enabling
     * email-based targeting and multichannel campaigns through Pushwoosh.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Register user email after profile update
     *   private void updateUserEmail(String newEmail) {
     *       // Validate email first
     *       if (!isValidEmail(newEmail)) {
     *           Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
     *           return;
     *       }
     *
     *       Pushwoosh.getInstance().setEmail(newEmail, (result) -> {
     *           if (result.isSuccess()) {
     *               Log.d("App", "Email registered for marketing campaigns: " + newEmail);
     *
     *               // Update local user profile
     *               userProfile.setEmail(newEmail);
     *               saveUserProfile(userProfile);
     *
     *               // Also update in tags
     *               TagsBundle emailTag = new TagsBundle.Builder()
     *                   .putString("Email", newEmail)
     *                   .putBoolean("Email_Verified", false)
     *                   .build();
     *               Pushwoosh.getInstance().setTags(emailTag);
     *
     *               // Show success and send verification
     *               Toast.makeText(this, "Email updated successfully", Toast.LENGTH_SHORT).show();
     *               sendEmailVerification(newEmail);
     *           } else {
     *               Log.e("App", "Failed to register email: " + result.getException().getMessage());
     *               Toast.makeText(this, "Failed to update email. Please try again.", Toast.LENGTH_SHORT).show();
     *           }
     *       });
     *   }
     * }
     * </pre>
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
     * Merges or removes event statistics for a user identifier.
     * <p>
     * This method either moves all event statistics from oldUserId to newUserId (if doMerge is true)
     * or removes all events associated with oldUserId (if doMerge is false). This is useful when
     * migrating user accounts or cleaning up data.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Merge user data when user logs in with different account
     *   Pushwoosh.getInstance().mergeUserId(
     *       "temp_user_123",
     *       "permanent_user_456",
     *       true, // merge events
     *       (result) -> {
     *           if (result.isSuccess()) {
     *               Log.d("Pushwoosh", "User data merged successfully");
     *           } else {
     *               Log.e("Pushwoosh", "Merge failed: " + result.getException().getMessage());
     *           }
     *       }
     *   );
     *
     *   // Remove old user data without merging
     *   Pushwoosh.getInstance().mergeUserId(
     *       "old_user_123",
     *       "new_user_456",
     *       false, // remove old events
     *       null
     *   );
     * }
     * </pre>
     *
     * @param oldUserId source user identifier
     * @param newUserId destination user identifier
     * @param doMerge   true to merge events from oldUserId to newUserId, false to remove events for oldUserId
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
     * Returns the current user identifier.
     * <p>
     * This method retrieves the user ID that was previously set using {@link #setUserId(String)}.
     * Returns null if no user ID has been set.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   String userId = Pushwoosh.getInstance().getUserId();
     *   if (userId != null) {
     *       Log.d("Pushwoosh", "Current user: " + userId);
     *   } else {
     *       Log.d("Pushwoosh", "No user ID set");
     *   }
     * }
     * </pre>
     *
     * @return current user id or null if not set
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
     * Enables Huawei Push Kit for push notifications on Huawei devices.
     * <p>
     * This method is specifically designed for plugin-based applications (Cordova, React Native, etc.)
     * to enable Huawei Push Kit support on Huawei devices without Google Mobile Services.
     * This method has no effect when called in native Android applications.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Enable Huawei Push in plugin-based applications
     *   Pushwoosh.getInstance().enableHuaweiPushNotifications();
     *   Pushwoosh.getInstance().registerForPushNotifications();
     * }
     * </pre>
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
     * Starts communication with the Pushwoosh server.
     * <p>
     * This method enables communication with Pushwoosh servers, allowing the SDK to send
     * and receive data. Use this in conjunction with {@link #stopServerCommunication()}
     * to implement GDPR compliance or user privacy preferences.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // User accepts privacy policy
     *   if (userAcceptsPrivacyPolicy()) {
     *       Pushwoosh.getInstance().startServerCommunication();
     *       Pushwoosh.getInstance().registerForPushNotifications();
     *   }
     * }
     * </pre>
     *
     * @see #stopServerCommunication()
     * @see #isServerCommunicationAllowed()
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
     * Stops communication with the Pushwoosh server.
     * <p>
     * This method disables all communication with Pushwoosh servers. The SDK will not send
     * any data or register for push notifications until {@link #startServerCommunication()}
     * is called. Use this to implement GDPR compliance or user privacy preferences.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // User opts out of notifications/tracking
     *   if (userOptsOut()) {
     *       Pushwoosh.getInstance().stopServerCommunication();
     *   }
     *
     *   // GDPR compliance: stop communication until user consents
     *   if (!userHasGivenConsent()) {
     *       Pushwoosh.getInstance().stopServerCommunication();
     *   }
     * }
     * </pre>
     *
     * @see #startServerCommunication()
     * @see #isServerCommunicationAllowed()
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
     * Checks if communication with Pushwoosh server is currently allowed.
     * <p>
     * This method returns the current state of server communication. It returns false
     * if {@link #stopServerCommunication()} was called and true after {@link #startServerCommunication()}.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   if (Pushwoosh.getInstance().isServerCommunicationAllowed()) {
     *       Log.d("Pushwoosh", "Server communication is enabled");
     *       Pushwoosh.getInstance().registerForPushNotifications();
     *   } else {
     *       Log.d("Pushwoosh", "Server communication is disabled");
     *   }
     * }
     * </pre>
     *
     * @return true if communication with Pushwoosh server is allowed, false otherwise
     * @see #startServerCommunication()
     * @see #stopServerCommunication()
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

    /**
     * Sets the list of allowed external hosts for secure push content.
     * <p>
     * This method configures which external hosts are allowed to serve content referenced
     * in push notifications (such as images). This is a security feature to prevent
     * unauthorized content from being loaded. Only hosts in this list will be allowed.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   ArrayList<String> allowedHosts = new ArrayList<>();
     *   allowedHosts.add("cdn.mycompany.com");
     *   allowedHosts.add("images.example.com");
     *
     *   Pushwoosh.getInstance().setAllowedExternalHosts(allowedHosts);
     * }
     * </pre>
     *
     * @param allowedExternalHosts list of allowed external host names
     */
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

    /**
     * Sets the API access token for Pushwoosh REST API calls.
     * <p>
     * This method configures the API token used for authenticating REST API requests to Pushwoosh.
     * The API token can be found in your Pushwoosh Control Panel under API Access settings.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     *   // Set API token for server-side operations
     *   Pushwoosh.getInstance().setApiToken("YOUR_API_TOKEN");
     * }
     * </pre>
     *
     * @param token Pushwoosh API access token
     */
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
