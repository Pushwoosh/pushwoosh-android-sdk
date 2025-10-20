package com.pushwoosh.notification;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.handlers.message.system.MessageSystemHandleChainProvider;
import com.pushwoosh.notification.handlers.message.user.MessageHandleChainProvider;
import com.pushwoosh.notification.handlers.notification.NotificationOpenHandlerChainProvider;
import com.pushwoosh.notification.handlers.notification.PushStatNotificationOpenHandler;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import java.util.List;

/**
 * Allows customization of push notification behavior before display and when opened.
 * <p>
 * NotificationServiceExtension provides hook methods to intercept and modify push notification
 * processing at key moments: when notifications are received, opened, canceled, or when notification
 * groups are opened. This is the primary extension point for customizing how your app handles
 * push notifications.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li><b>Foreground Handling</b> - Prevent notification display when app is in foreground</li>
 * <li><b>Custom Launch Behavior</b> - Control which activity opens when notification is tapped</li>
 * <li><b>Deep Link Control</b> - Override default URL/deep link handling</li>
 * <li><b>Lifecycle Callbacks</b> - Track notification open, cancel, and group open events</li>
 * <li><b>Context Access</b> - Access application context for custom logic</li>
 * </ul>
 * <p>
 * <b>Setup:</b>
 * <p>
 * 1. Create a public class extending NotificationServiceExtension with a public no-args constructor:
 * <pre>
 * {@code
 * public class MyNotificationService extends NotificationServiceExtension {
 *     @Override
 *     protected boolean onMessageReceived(PushMessage message) {
 *         // Return true to suppress notification when app is in foreground
 *         if (isAppOnForeground()) {
 *             // Handle notification in-app (show dialog, update UI, etc.)
 *             showInAppAlert(message.getMessage());
 *             return true;
 *         }
 *         return false;
 *     }
 *
 *     @Override
 *     protected void onMessageOpened(PushMessage message) {
 *         // Track notification open event
 *         String campaignId = message.getCustomData().getString("campaign_id");
 *         Analytics.track("notification_opened", campaignId);
 *     }
 * }
 * }
 * </pre>
 * <p>
 * 2. Register your extension in AndroidManifest.xml:
 * <pre>
 * {@code
 * <application>
 *     <meta-data
 *         android:name="com.pushwoosh.notification_service_extension"
 *         android:value="com.your.package.MyNotificationService" />
 * </application>
 * }
 * </pre>
 * <p>
 * <b>Common Use Cases:</b>
 * <br><br>
 * <b>Example 1: Suppress notifications when app is in foreground</b>
 * <pre>
 * {@code
 * @Override
 * protected boolean onMessageReceived(PushMessage message) {
 *     if (isAppOnForeground()) {
 *         // Show in-app notification instead
 *         Context context = getApplicationContext();
 *         Toast.makeText(context,
 *             message.getMessage(),
 *             Toast.LENGTH_LONG).show();
 *         return true; // Notification won't be displayed
 *     }
 *     return false; // Show notification normally
 * }
 * }
 * </pre>
 * <p>
 * <b>Example 2: Launch specific activity based on notification data</b>
 * <pre>
 * {@code
 * @Override
 * protected void startActivityForPushMessage(PushMessage message) {
 *     Context context = getApplicationContext();
 *     String screenType = message.getCustomData().getString("screen");
 *
 *     Intent intent;
 *     if ("product".equals(screenType)) {
 *         // Open product details
 *         intent = new Intent(context, ProductActivity.class);
 *         String productId = message.getCustomData().getString("product_id");
 *         intent.putExtra("productId", productId);
 *     } else if ("cart".equals(screenType)) {
 *         // Open shopping cart
 *         intent = new Intent(context, CartActivity.class);
 *     } else {
 *         // Default behavior
 *         super.startActivityForPushMessage(message);
 *         return;
 *     }
 *
 *     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
 *                     Intent.FLAG_ACTIVITY_CLEAR_TOP);
 *     context.startActivity(intent);
 * }
 * }
 * </pre>
 * <p>
 * <b>Example 3: Handle notification cancel events</b>
 * <pre>
 * {@code
 * @Override
 * protected void onMessageCanceled(PushMessage message) {
 *     // User dismissed notification - track in analytics
 *     String campaignId = message.getCustomData().getString("campaign_id");
 *     Analytics.track("notification_dismissed", campaignId);
 *
 *     // Update notification count
 *     SharedPreferences prefs = getApplicationContext()
 *         .getSharedPreferences("stats", Context.MODE_PRIVATE);
 *     int dismissCount = prefs.getInt("dismiss_count", 0);
 *     prefs.edit().putInt("dismiss_count", dismissCount + 1).apply();
 * }
 * }
 * </pre>
 * <p>
 * <b>Example 4: Override URL handling</b>
 * <pre>
 * {@code
 * @Override
 * protected boolean preHandleNotificationsWithUrl() {
 *     // Return false to disable automatic URL/deep link handling
 *     // You must handle URLs manually in startActivityForPushMessage()
 *     return false;
 * }
 *
 * @Override
 * protected void startActivityForPushMessage(PushMessage message) {
 *     // Custom URL handling logic
 *     String url = message.getLink();
 *     if (url != null && url.startsWith("myapp://")) {
 *         // Handle custom deep link
 *         handleDeepLink(url);
 *     } else {
 *         super.startActivityForPushMessage(message);
 *     }
 * }
 * }
 * </pre>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>Your extension class must be <b>public</b> and have a <b>public no-argument constructor</b></li>
 * <li>Application will crash on startup if these requirements are not met</li>
 * <li>{@link #onMessageReceived(PushMessage)} runs on a background thread - use {@link #getApplicationContext()} safely</li>
 * <li>{@link #startActivityForPushMessage(PushMessage)} runs on the main thread</li>
 * <li>Returning {@code true} from {@link #onMessageReceived(PushMessage)} suppresses notification display</li>
 * <li>Only one NotificationServiceExtension can be registered per application</li>
 * </ul>
 *
 * @see PushMessage
 * @see NotificationFactory
 * @see PushwooshNotificationSettings
 */
public class NotificationServiceExtension {
    private static final String TAG = "NotificationService";

    private NotificationOpenHandler notificationOpenHandler;
    private PushMessageHandler pushMessageHandler;
    private PushMessageFactory pushMessageFactory;
    private PushwooshNotificationManager pushNotificationManager;
    private Config config;
    @Nullable
    private Context applicationContext;
    private final PushStatNotificationOpenHandler pushStatNotificationOpenHandler;

    public NotificationServiceExtension() {
        pushMessageFactory = PushwooshPlatform.getInstance().getPushMessageFactory();
        applicationContext = AndroidPlatformModule.getApplicationContext();
        pushNotificationManager = PushwooshPlatform.getInstance().notificationManager();
        notificationOpenHandler = new NotificationOpenHandler(NotificationOpenHandlerChainProvider.getNotificationOpenHandlerChain());
        pushMessageHandler = new PushMessageHandler(MessageSystemHandleChainProvider.getMessageSystemChain(), MessageHandleChainProvider.getHandleProcessor());
        config = PushwooshPlatform.getInstance().getConfig();
        pushStatNotificationOpenHandler = new PushStatNotificationOpenHandler();
    }

    /**
     * Internal method that handles incoming push notification messages.
     * <p>
     * This is a final method called by the SDK when a push notification arrives. It processes
     * the notification bundle and delegates to {@link #onMessageReceived(PushMessage)} for
     * custom handling. You should override {@link #onMessageReceived(PushMessage)} instead
     * of calling this method directly.
     * <p>
     * The method runs on a background worker thread and performs the following:
     * <ul>
     * <li>Validates the notification payload</li>
     * <li>Creates a {@link PushMessage} from the bundle</li>
     * <li>Calls {@link #onMessageReceived(PushMessage)} for custom processing</li>
     * <li>Handles notification display based on your return value</li>
     * </ul>
     *
     * @param pushBundle push notification payload as Bundle containing all notification data
     *
     * @see #onMessageReceived(PushMessage)
     */
    @WorkerThread

    public final void handleMessage(Bundle pushBundle) {
        handleMessageInternal(pushBundle);
    }

    void handleMessageInternal(Bundle pushBundle) {
        if (pushBundle == null) {
            PWLog.info("handle null message");
            return;
        }

        PWLog.debug(TAG, "handleMessage: " + pushBundle.toString());

        if (pushMessageHandler.preHandleMessage(pushBundle)) {
            return;
        }

        PushMessage message = pushMessageFactory.createPushMessage(pushBundle);


        boolean isHandled = onMessageReceived(message);

        boolean isNeedSendPushStat = isHandled && config.getSendPushStatIfShowForegroundDisabled();
        if (isNeedSendPushStat) {
            PWLog.debug(TAG, String.format("pushStatNotificationOpenHandler.postHandleNotification: %s", pushBundle));
            pushStatNotificationOpenHandler.postHandleNotification(pushBundle);
        }

        pushMessageHandler.handlePushMessage(message, isHandled);
    }

    /**
     * Internal method that handles notification open events.
     * <p>
     * This is a final method called by the SDK when a user taps on a notification. It processes
     * the notification open event and delegates to {@link #onMessageOpened(PushMessage)} and
     * {@link #startActivityForPushMessage(PushMessage)} for custom handling.
     * <p>
     * You should override {@link #onMessageOpened(PushMessage)} or
     * {@link #startActivityForPushMessage(PushMessage)} instead of calling this method directly.
     * <p>
     * The method performs the following:
     * <ul>
     * <li>Processes URLs and deep links if {@link #preHandleNotificationsWithUrl()} returns true</li>
     * <li>Sets the notification as launch notification in the SDK</li>
     * <li>Calls {@link #startActivityForPushMessage(PushMessage)} to launch the appropriate activity</li>
     * <li>Sends push statistics to Pushwoosh</li>
     * <li>Calls {@link #onMessageOpened(PushMessage)} callback</li>
     * </ul>
     *
     * @param pushBundle push notification payload as Bundle containing all notification data
     *
     * @see #onMessageOpened(PushMessage)
     * @see #startActivityForPushMessage(PushMessage)
     * @see #preHandleNotificationsWithUrl()
     */
    public final void handleNotification(Bundle pushBundle) {
        if (pushBundle == null) {
            PWLog.info("open null notification");
            return;
        }
        PushMessage message = new PushMessage(pushBundle);

        try {
            if (preHandleNotificationsWithUrl()) {
                if (notificationOpenHandler.preHandleNotification(pushBundle)) {
                    return;
                }
            }

            pushNotificationManager.setLaunchNotification(message);

            startActivityForPushMessage(message);
        } finally {
            notificationOpenHandler.postHandleNotification(pushBundle);
            onMessageOpened(message);
        }

    }

    /**
     * Internal method that handles notification group open events.
     * <p>
     * This is a final method called by the SDK when a user taps on a grouped notification
     * (notification stack). On Android, multiple notifications from your app can be grouped
     * together in a single expandable notification.
     * <p>
     * By default, this method delegates to {@link #onMessagesGroupOpened(List)}, which opens
     * the most recent notification. You can override {@link #onMessagesGroupOpened(List)} to
     * customize group open behavior.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * protected void onMessagesGroupOpened(List<PushMessage> messages) {
     *     // Show summary screen with all messages
     *     Intent intent = new Intent(getApplicationContext(), NotificationListActivity.class);
     *     intent.putExtra("message_count", messages.size());
     *     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     *     getApplicationContext().startActivity(intent);
     * }
     * }
     * </pre>
     *
     * @param messages list of push messages in the group that was opened
     *
     * @see #onMessagesGroupOpened(List)
     */
    public final void handleNotificationGroup(List<PushMessage> messages) {
        onMessagesGroupOpened(messages);
    }

    /**
     * Internal method that handles notification dismissal events.
     * <p>
     * This is a final method called by the SDK when a user swipes away or dismisses a notification
     * from the notification center. It delegates to {@link #onMessageCanceled(PushMessage)} for
     * custom handling.
     * <p>
     * You should override {@link #onMessageCanceled(PushMessage)} instead of calling this method
     * directly.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * protected void onMessageCanceled(PushMessage message) {
     *     // Track dismissal in analytics
     *     String messageId = message.getCustomData().getString("message_id");
     *     Analytics.track("notification_dismissed", messageId);
     *
     *     // Update unread count
     *     decrementUnreadNotifications();
     * }
     * }
     * </pre>
     *
     * @param pushBundle push notification payload as Bundle containing all notification data
     *
     * @see #onMessageCanceled(PushMessage)
     */
    public final void handleNotificationCanceled(Bundle pushBundle) {
        if (pushBundle == null) {
            PWLog.info("cancel null notification");
            return;
        }
        PushMessage message = new PushMessage(pushBundle);
        onMessageCanceled(message);
    }

    /**
     * Callback invoked when a user taps on a notification.
     * <p>
     * Override this method to handle notification open events, such as tracking analytics,
     * updating application state, or triggering custom logic. This callback is called after
     * the activity is launched but provides an opportunity to perform additional processing.
     * <p>
     * This method is called on the main thread after {@link #startActivityForPushMessage(PushMessage)}.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * protected void onMessageOpened(PushMessage message) {
     *     // Track notification open in analytics
     *     String campaignCode = message.getCustomData().getString("campaign_code");
     *     Analytics.track("push_opened", campaignCode);
     *
     *     // Update user engagement score
     *     SharedPreferences prefs = getApplicationContext()
     *         .getSharedPreferences("user_stats", Context.MODE_PRIVATE);
     *     int openCount = prefs.getInt("push_open_count", 0);
     *     prefs.edit().putInt("push_open_count", openCount + 1).apply();
     *
     *     // Log for debugging
     *     Log.d("PushNotifications", "User opened: " + message.getMessage());
     * }
     * }
     * </pre>
     *
     * @param message the push message that was opened, containing notification data and custom payload
     *
     * @see #startActivityForPushMessage(PushMessage)
     * @see PushMessage
     */

    @SuppressWarnings({"WeakerAccess", "unused"})
    protected void onMessageOpened(final PushMessage message) {

    }

    /**
     * Callback invoked when a user dismisses or swipes away a notification.
     * <p>
     * Override this method to handle notification dismissal events. This is useful for tracking
     * user engagement, cleaning up notification state, or updating notification counters. The
     * callback is triggered when the user explicitly dismisses a notification from the notification
     * center (not when it expires or is cleared programmatically).
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * protected void onMessageCanceled(PushMessage message) {
     *     // Track dismissal in analytics
     *     String campaignId = message.getCustomData().getString("campaign_id");
     *     Analytics.track("notification_dismissed", campaignId);
     *
     *     // Update badge count
     *     SharedPreferences prefs = getApplicationContext()
     *         .getSharedPreferences("notifications", Context.MODE_PRIVATE);
     *     int badgeCount = prefs.getInt("badge_count", 0);
     *     if (badgeCount > 0) {
     *         prefs.edit().putInt("badge_count", badgeCount - 1).apply();
     *     }
     *
     *     // Clean up related local data
     *     String notificationId = message.getCustomData().getString("notification_id");
     *     deleteNotificationData(notificationId);
     * }
     * }
     * </pre>
     *
     * @param message the push message that was dismissed by the user
     *
     * @see PushMessage
     */
    protected void onMessageCanceled(final PushMessage message) {

    }


    /**
     * Callback invoked when a user taps on a grouped notification (notification stack).
     * <p>
     * On Android, when multiple notifications from your app are displayed, they can be automatically
     * grouped into a single expandable notification. This method is called when the user taps on
     * the grouped notification or its summary.
     * <p>
     * By default, this method opens the most recent notification by calling
     * {@link #handleNotification(Bundle)} with the last message in the list. Override this method
     * to implement custom behavior, such as showing a list of all messages or opening a summary screen.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * protected void onMessagesGroupOpened(List<PushMessage> messages) {
     *     Context context = getApplicationContext();
     *
     *     // Create intent to summary screen
     *     Intent intent = new Intent(context, NotificationSummaryActivity.class);
     *
     *     // Pass all message IDs
     *     ArrayList<String> messageIds = new ArrayList<>();
     *     for (PushMessage msg : messages) {
     *         String id = msg.getCustomData().getString("message_id");
     *         messageIds.add(id);
     *     }
     *     intent.putStringArrayListExtra("message_ids", messageIds);
     *     intent.putExtra("count", messages.size());
     *
     *     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
     *                     Intent.FLAG_ACTIVITY_CLEAR_TOP);
     *     context.startActivity(intent);
     *
     *     // Track group open
     *     Analytics.track("notification_group_opened", messages.size());
     * }
     * }
     * </pre>
     *
     * @param messages list of push messages in the group that was opened, ordered from oldest to newest
     *
     * @see #onMessageOpened(PushMessage)
     * @see PushMessage
     */
    protected void onMessagesGroupOpened(final List<PushMessage> messages) {
        handleNotification(messages.get(messages.size() - 1).toBundle());
    }

    /**
     * Callback invoked when a push notification is received, before it is displayed.
     * <p>
     * This is the primary method for customizing notification display behavior. Override this method
     * to suppress notification display when the app is in the foreground, filter unwanted notifications,
     * or handle notifications with custom UI.
     * <p>
     * <b>Important:</b> This method runs on a <b>background worker thread</b>, so it's safe to perform
     * I/O operations, but you must use {@link android.os.Handler} or {@code runOnUiThread()} for UI updates.
     * <p>
     * <b>Return Value:</b>
     * <ul>
     * <li><b>true</b> - Notification will NOT be displayed in the notification center. Use this when
     * handling the notification with custom in-app UI or when suppressing foreground notifications.</li>
     * <li><b>false</b> - Notification will be displayed normally in the notification center.</li>
     * </ul>
     * <p>
     * By default, this method returns {@code true} if the app is in the foreground and
     * "Show foreground alert" is enabled in Pushwoosh settings, otherwise returns {@code false}.
     * <br><br>
     * <b>Example 1: Suppress notifications when app is in foreground</b>
     * <pre>
     * {@code
     * @Override
     * protected boolean onMessageReceived(PushMessage message) {
     *     if (isAppOnForeground()) {
     *         // Show custom in-app notification
     *         Context context = getApplicationContext();
     *         Handler mainHandler = new Handler(context.getMainLooper());
     *         mainHandler.post(() -> {
     *             Toast.makeText(context,
     *                 message.getMessage(),
     *                 Toast.LENGTH_LONG).show();
     *         });
     *         return true; // Don't show system notification
     *     }
     *     return false; // Show notification normally
     * }
     * }
     * </pre>
     * <p>
     * <b>Example 2: Filter notifications by priority</b>
     * <pre>
     * {@code
     * @Override
     * protected boolean onMessageReceived(PushMessage message) {
     *     String priority = message.getCustomData().getString("priority");
     *
     *     // Block low-priority notifications when user is busy
     *     if ("low".equals(priority) && isUserBusy()) {
     *         Log.d("Notifications", "Suppressed low priority notification");
     *         return true; // Suppress notification
     *     }
     *
     *     // Show high-priority with custom sound
     *     if ("high".equals(priority)) {
     *         playCustomSound();
     *     }
     *
     *     return false; // Show notification
     * }
     * }
     * </pre>
     * <p>
     * <b>Example 3: Handle notifications with custom in-app UI</b>
     * <pre>
     * {@code
     * @Override
     * protected boolean onMessageReceived(PushMessage message) {
     *     if (isAppOnForeground()) {
     *         Context context = getApplicationContext();
     *         Handler mainHandler = new Handler(context.getMainLooper());
     *
     *         mainHandler.post(() -> {
     *             // Show custom dialog or banner
     *             Intent intent = new Intent(context, InAppNotificationActivity.class);
     *             intent.putExtra("message", message.getMessage());
     *             intent.putExtra("title", message.getHeader());
     *             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     *             context.startActivity(intent);
     *         });
     *
     *         return true; // Notification handled in-app
     *     }
     *     return false; // Show system notification
     * }
     * }
     * </pre>
     *
     * @param data the push message data containing notification content and custom payload
     * @return {@code true} to suppress notification display, {@code false} to show notification normally
     *
     * @see #isAppOnForeground()
     * @see #getApplicationContext()
     * @see PushMessage
     */
    @WorkerThread
    protected boolean onMessageReceived(PushMessage data) {
        if (RepositoryModule.getNotificationPreferences() != null) {
            return (RepositoryModule.getNotificationPreferences().showPushnotificationAlert().get() && isAppOnForeground());
        } else {
            return false;
        }
    }

    /**
     * Callback invoked to launch an activity when a notification is opened.
     * <p>
     * Override this method to customize which activity opens when the user taps on a notification.
     * This is the primary method for implementing custom deep linking, routing users to specific
     * screens based on notification data, or adding custom launch logic.
     * <p>
     * <b>Default Behavior:</b><br>
     * By default, this method attempts to start an activity in the following order:
     * <ol>
     * <li>Activity with intent filter action: {@code {applicationId}.MESSAGE}</li>
     * <li>Default launcher activity (if no MESSAGE activity found)</li>
     * </ol>
     * <p>
     * <b>Important:</b> This method runs on the <b>main thread</b>. It's safe to update UI but avoid
     * long-running operations.
     * <br><br>
     * <b>Example 1: Route to different activities based on notification data</b>
     * <pre>
     * {@code
     * @Override
     * protected void startActivityForPushMessage(PushMessage message) {
     *     Context context = getApplicationContext();
     *     String screenType = message.getCustomData().getString("screen");
     *
     *     Intent intent;
     *     if ("product".equals(screenType)) {
     *         // Open product details
     *         intent = new Intent(context, ProductActivity.class);
     *         String productId = message.getCustomData().getString("product_id");
     *         intent.putExtra("productId", productId);
     *
     *     } else if ("order".equals(screenType)) {
     *         // Open order details
     *         intent = new Intent(context, OrderActivity.class);
     *         String orderId = message.getCustomData().getString("order_id");
     *         intent.putExtra("orderId", orderId);
     *
     *     } else if ("cart".equals(screenType)) {
     *         // Open shopping cart
     *         intent = new Intent(context, CartActivity.class);
     *
     *     } else {
     *         // Default behavior for unknown screen types
     *         super.startActivityForPushMessage(message);
     *         return;
     *     }
     *
     *     // Required flags for starting activity from non-activity context
     *     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
     *                     Intent.FLAG_ACTIVITY_CLEAR_TOP |
     *                     Intent.FLAG_ACTIVITY_SINGLE_TOP);
     *
     *     // Pass full push message data
     *     intent.putExtra("pushMessage", message.toJson().toString());
     *
     *     context.startActivity(intent);
     * }
     * }
     * </pre>
     * <p>
     * <b>Example 2: Handle custom deep links</b>
     * <pre>
     * {@code
     * @Override
     * protected void startActivityForPushMessage(PushMessage message) {
     *     String deepLink = message.getCustomData().getString("deep_link");
     *
     *     if (deepLink != null && deepLink.startsWith("myapp://")) {
     *         // Parse and handle custom deep link
     *         Uri uri = Uri.parse(deepLink);
     *         String path = uri.getPath(); // e.g., "/product/123"
     *
     *         // Route based on path
     *         if (path.startsWith("/product/")) {
     *             String productId = path.substring("/product/".length());
     *             openProductScreen(productId);
     *         } else if (path.equals("/cart")) {
     *             openCartScreen();
     *         } else {
     *             // Unknown deep link - use default
     *             super.startActivityForPushMessage(message);
     *         }
     *     } else {
     *         // No custom deep link - use default
     *         super.startActivityForPushMessage(message);
     *     }
     * }
     * }
     * </pre>
     * <p>
     * <b>Example 3: Add authentication check before opening activity</b>
     * <pre>
     * {@code
     * @Override
     * protected void startActivityForPushMessage(PushMessage message) {
     *     Context context = getApplicationContext();
     *     SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
     *     boolean isLoggedIn = prefs.getBoolean("is_logged_in", false);
     *
     *     String screenType = message.getCustomData().getString("screen");
     *     boolean requiresAuth = message.getCustomData().getBoolean("requires_auth", false);
     *
     *     if (requiresAuth && !isLoggedIn) {
     *         // Redirect to login screen
     *         Intent intent = new Intent(context, LoginActivity.class);
     *         intent.putExtra("redirect_screen", screenType);
     *         intent.putExtra("pushMessage", message.toJson().toString());
     *         intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     *         context.startActivity(intent);
     *     } else {
     *         // User is authenticated or auth not required
     *         super.startActivityForPushMessage(message);
     *     }
     * }
     * }
     * </pre>
     *
     * @param message the push message containing notification data and custom payload used for routing
     *
     * @see PushMessage
     * @see #onMessageOpened(PushMessage)
     */
    @MainThread
    protected void startActivityForPushMessage(PushMessage message) {
        notificationOpenHandler.startPushLauncherActivity(message);
    }

    /**
     * Controls whether Pushwoosh should automatically handle notifications containing URLs or deep links.
     * <p>
     * By default, Pushwoosh automatically processes notifications that contain a URL or deep link in
     * the notification payload. If an activity can handle the URL/deep link, it will be started
     * automatically, and {@link #startActivityForPushMessage(PushMessage)} will not be called.
     * <p>
     * Override this method to return {@code false} if you want to handle all URLs and deep links
     * manually in {@link #startActivityForPushMessage(PushMessage)}.
     * <p>
     * <b>Default Behavior (returns true):</b><br>
     * Pushwoosh attempts to open URLs/deep links using Android's Intent system. If successful,
     * {@link #startActivityForPushMessage(PushMessage)} is skipped.
     * <p>
     * <b>Custom Behavior (returns false):</b><br>
     * All notifications are routed to {@link #startActivityForPushMessage(PushMessage)}, giving
     * you full control over URL/deep link handling.
     * <br><br>
     * <b>Example 1: Disable automatic URL handling</b>
     * <pre>
     * {@code
     * @Override
     * protected boolean preHandleNotificationsWithUrl() {
     *     // Disable automatic URL handling
     *     return false;
     * }
     *
     * @Override
     * protected void startActivityForPushMessage(PushMessage message) {
     *     // Now YOU control all URL/deep link handling
     *     String url = message.getLink();
     *
     *     if (url != null) {
     *         if (url.startsWith("myapp://")) {
     *             // Handle custom deep link
     *             handleDeepLink(url);
     *         } else if (url.startsWith("http")) {
     *             // Handle web URL with custom browser
     *             openInCustomBrowser(url);
     *         }
     *     } else {
     *         // No URL - use default launch
     *         super.startActivityForPushMessage(message);
     *     }
     * }
     * }
     * </pre>
     * <p>
     * <b>Example 2: Conditional URL handling</b>
     * <pre>
     * {@code
     * @Override
     * protected boolean preHandleNotificationsWithUrl() {
     *     // Let Pushwoosh handle external URLs only
     *     // Handle custom deep links manually
     *     SharedPreferences prefs = getApplicationContext()
     *         .getSharedPreferences("settings", Context.MODE_PRIVATE);
     *     return prefs.getBoolean("auto_open_links", true);
     * }
     * }
     * </pre>
     *
     * @return {@code true} to enable automatic URL/deep link handling (default),
     *         {@code false} to handle all URLs manually in {@link #startActivityForPushMessage(PushMessage)}
     *
     * @see #startActivityForPushMessage(PushMessage)
     * @see PushMessage#getLink()
     */
    protected boolean preHandleNotificationsWithUrl() {
        return true;
    }

    /**
     * Checks whether the application is currently running in the foreground.
     * <p>
     * Use this method to determine if your app is visible to the user when a push notification
     * arrives. This is commonly used in {@link #onMessageReceived(PushMessage)} to decide whether
     * to suppress the system notification and show custom in-app UI instead.
     * <p>
     * An app is considered "in foreground" when any of its activities is visible and has focus.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * protected boolean onMessageReceived(PushMessage message) {
     *     if (isAppOnForeground()) {
     *         // App is visible - show in-app notification
     *         showInAppAlert(message.getMessage());
     *         return true; // Suppress system notification
     *     }
     *     // App is in background - show normal notification
     *     return false;
     * }
     * }
     * </pre>
     *
     * @return {@code true} if the application is currently visible and has focus,
     *         {@code false} if the app is in the background or not running
     *
     * @see #onMessageReceived(PushMessage)
     */
    protected boolean isAppOnForeground() {
        return DeviceUtils.isAppOnForeground();
    }

    /**
     * Returns the application context.
     * <p>
     * Use this method to access Android's application context when implementing custom notification
     * handling logic. The application context is safe to use for accessing system services, resources,
     * SharedPreferences, and other application-level operations.
     * <p>
     * <b>Important:</b> This method may return {@code null} if called before the SDK is fully
     * initialized. Always check for null before using the returned context.
     * <br><br>
     * Example:
     * <pre>
     * {@code
     * @Override
     * protected boolean onMessageReceived(PushMessage message) {
     *     Context context = getApplicationContext();
     *     if (context != null && isAppOnForeground()) {
     *         // Use context to show Toast
     *         Handler mainHandler = new Handler(context.getMainLooper());
     *         mainHandler.post(() -> {
     *             Toast.makeText(context,
     *                 message.getMessage(),
     *                 Toast.LENGTH_LONG).show();
     *         });
     *         return true;
     *     }
     *     return false;
     * }
     *
     * @Override
     * protected void startActivityForPushMessage(PushMessage message) {
     *     Context context = getApplicationContext();
     *     if (context != null) {
     *         // Use context to access SharedPreferences
     *         SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
     *         boolean useCustomLauncher = prefs.getBoolean("custom_launcher", false);
     *
     *         if (useCustomLauncher) {
     *             // Launch custom activity
     *             Intent intent = new Intent(context, CustomLauncherActivity.class);
     *             intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
     *             context.startActivity(intent);
     *         } else {
     *             super.startActivityForPushMessage(message);
     *         }
     *     }
     * }
     * }
     * </pre>
     *
     * @return the application context, or {@code null} if not yet initialized
     *
     * @see #onMessageReceived(PushMessage)
     * @see #startActivityForPushMessage(PushMessage)
     */
    @Nullable
    protected final Context getApplicationContext() {
        return applicationContext;
    }
}
