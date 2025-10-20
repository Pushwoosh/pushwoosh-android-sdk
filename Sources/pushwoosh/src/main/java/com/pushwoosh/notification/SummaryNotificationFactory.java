package com.pushwoosh.notification;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.core.app.NotificationCompat;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.notification.builder.SummaryNotificationBuilder;
import com.pushwoosh.notification.channel.NotificationChannelManager;
import com.pushwoosh.repository.RepositoryModule;

import static com.pushwoosh.repository.NotificationPrefs.DEFAULT_GROUP_CHANNEL_NAME;

/**
 * Base class for customizing grouped notification summary appearance on Android 7.0 (Nougat) and higher.
 * <p>
 * When multiple push notifications are displayed with the same group ID, Android automatically
 * collapses them into a single stacked notification. The summary notification appears at the top
 * of the stack and provides an overview of all grouped notifications. This class allows you to
 * customize the appearance of that summary notification.
 * <p>
 * <b>Group Notifications Feature:</b>
 * <ul>
 * <li>Only works on Android 7.0 (API 24) and higher</li>
 * <li>Requires enabling multi-notification mode in AndroidManifest.xml</li>
 * <li>Groups notifications by the "pw_group" attribute in push payload</li>
 * <li>Summary shows the total count and custom message</li>
 * <li>User can expand to see individual notifications</li>
 * </ul>
 * <p>
 * <b>Registration:</b> Custom summary factory must be registered in AndroidManifest.xml:
 * <pre>
 * {@code
 *   <application>
 *       <!-- Enable multi-notification mode -->
 *       <meta-data
 *           android:name="com.pushwoosh.multi_notification_mode"
 *           android:value="true" />
 *
 *       <!-- Register custom summary factory -->
 *       <meta-data
 *           android:name="com.pushwoosh.summary_notification_factory"
 *           android:value=".MySummaryNotificationFactory" />
 *   </application>
 * }
 * </pre>
 * <p>
 * <b>Quick Start - Basic customization:</b>
 * <pre>
 * {@code
 *   public class MySummaryNotificationFactory extends SummaryNotificationFactory {
 *       @Override
 *       public String summaryNotificationMessage(int notificationsAmount) {
 *           // Customize summary text
 *           return notificationsAmount + " new messages";
 *       }
 *
 *       @Override
 *       public int summaryNotificationIconResId() {
 *           // Use custom icon for summary
 *           return R.drawable.ic_notification_stack;
 *       }
 *
 *       @Override
 *       public int summaryNotificationColor() {
 *           // Use brand color
 *           return 0xFF6200EE;
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * <b>Example - E-commerce app with order grouping:</b>
 * <pre>
 * {@code
 *   public class OrderSummaryFactory extends SummaryNotificationFactory {
 *       @Override
 *       public String summaryNotificationMessage(int notificationsAmount) {
 *           // Customize based on notification count
 *           if (notificationsAmount == 1) {
 *               return "1 order update";
 *           } else {
 *               return notificationsAmount + " order updates";
 *           }
 *       }
 *
 *       @Override
 *       public int summaryNotificationIconResId() {
 *           return R.drawable.ic_shopping_bag;
 *       }
 *
 *       @Override
 *       public int summaryNotificationColor() {
 *           return getApplicationContext()
 *               .getResources()
 *               .getColor(R.color.brand_primary);
 *       }
 *
 *       @Override
 *       public boolean autoCancelSummaryNotification() {
 *           // Dismiss summary when user taps it
 *           return true;
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * <b>Example - Conditional summary display:</b>
 * <pre>
 * {@code
 *   public class SmartSummaryFactory extends SummaryNotificationFactory {
 *       @Override
 *       public boolean shouldGenerateSummaryNotification() {
 *           // Check user preferences
 *           SharedPreferences prefs = getApplicationContext()
 *               .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
 *           return prefs.getBoolean("enable_grouped_notifications", true);
 *       }
 *
 *       @Override
 *       public String summaryNotificationMessage(int notificationsAmount) {
 *           return notificationsAmount + " notifications";
 *       }
 *
 *       @Override
 *       public int summaryNotificationIconResId() {
 *           return -1; // Use default
 *       }
 *
 *       @Override
 *       public int summaryNotificationColor() {
 *           return -1; // Use default
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>Your factory class MUST be public and have a public no-argument constructor</li>
 * <li>Multi-notification mode must be enabled for grouping to work</li>
 * <li>Summary notifications are only created on Android 7.0+</li>
 * <li>Group ID is specified in push payload with "pw_group" attribute</li>
 * </ul>
 *
 * @see PushwooshSummaryNotificationFactory
 * @see #summaryNotificationMessage(int)
 * @see #summaryNotificationIconResId()
 * @see #summaryNotificationColor()
 */
public abstract class SummaryNotificationFactory {
    public static String NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID = "pushwoosh_need_to_add_new_notification_channel_id";
    @Nullable
    private final Context applicationContext;
    private NotificationChannelManager notificationChannelManager;

    SummaryNotificationFactory() {
        applicationContext = AndroidPlatformModule.getApplicationContext();
        notificationChannelManager = new NotificationChannelManager(applicationContext);
    }

    /**
     * Returns the message text displayed in the summary notification.
     * <p>
     * This text appears in the collapsed summary notification that represents multiple grouped
     * notifications. It should provide a clear indication of how many notifications are in the
     * group and what they're about.
     * <p>
     * <b>Best Practices:</b>
     * <ul>
     * <li>Include the notification count for clarity</li>
     * <li>Use plural forms appropriately (1 message vs 2 messages)</li>
     * <li>Keep the text concise (under 40 characters is ideal)</li>
     * <li>Localize the text for international audiences</li>
     * </ul>
     * <br>
     * Example - Basic count message:
     * <pre>
     * {@code
     *   @Override
     *   public String summaryNotificationMessage(int notificationsAmount) {
     *       return notificationsAmount + " new messages";
     *   }
     * }
     * </pre>
     * <br>
     * Example - Proper pluralization:
     * <pre>
     * {@code
     *   @Override
     *   public String summaryNotificationMessage(int notificationsAmount) {
     *       if (notificationsAmount == 1) {
     *           return "1 new message";
     *       } else {
     *           return notificationsAmount + " new messages";
     *       }
     *   }
     * }
     * </pre>
     * <br>
     * Example - Localized with resources:
     * <pre>
     * {@code
     *   @Override
     *   public String summaryNotificationMessage(int notificationsAmount) {
     *       Resources res = getApplicationContext().getResources();
     *       return res.getQuantityString(
     *           R.plurals.notification_summary,
     *           notificationsAmount,
     *           notificationsAmount
     *       );
     *   }
     *
     *   // In res/values/strings.xml:
     *   // <plurals name="notification_summary">
     *   //     <item quantity="one">%d new message</item>
     *   //     <item quantity="other">%d new messages</item>
     *   // </plurals>
     * }
     * </pre>
     *
     * @param notificationsAmount Number of notifications in the group
     * @return Summary message text to display. Empty string will hide the summary text.
     */
    public abstract String summaryNotificationMessage(int notificationsAmount);

    /**
     * Returns the small icon resource ID for the summary notification.
     * <p>
     * The small icon appears in the status bar and in the notification itself. It should be a
     * simple, recognizable symbol that represents your app or the grouped notifications.
     * <p>
     * <b>Icon Requirements:</b>
     * <ul>
     * <li>Must be a drawable resource from your app</li>
     * <li>Should be a white icon on transparent background</li>
     * <li>Recommended size: 24x24 dp</li>
     * <li>Best as vector drawable for all screen densities</li>
     * </ul>
     * <br>
     * Example - Use custom stack icon:
     * <pre>
     * {@code
     *   @Override
     *   public int summaryNotificationIconResId() {
     *       return R.drawable.ic_notification_stack;
     *   }
     * }
     * </pre>
     * <br>
     * Example - Use default app icon:
     * <pre>
     * {@code
     *   @Override
     *   public int summaryNotificationIconResId() {
     *       return -1; // Uses same icon as individual notifications
     *   }
     * }
     * </pre>
     *
     * @return Drawable resource ID for the summary notification icon, or -1 to use the default
     *         notification icon configured for the app
     */
    public abstract int summaryNotificationIconResId();

    /**
     * Returns the accent color for the summary notification icon.
     * <p>
     * On Android 5.0 (Lollipop) and higher, the notification icon is displayed in a single color
     * on the notification shade. This method specifies that color, typically matching your app's
     * brand color or theme.
     * <p>
     * The color is applied as a tint to the small icon and can help users visually identify
     * notifications from your app.
     * <br><br>
     * Example - Use brand color:
     * <pre>
     * {@code
     *   @Override
     *   public int summaryNotificationColor() {
     *       return 0xFF6200EE; // Purple brand color
     *   }
     * }
     * </pre>
     * <br>
     * Example - Load from resources:
     * <pre>
     * {@code
     *   @Override
     *   public int summaryNotificationColor() {
     *       return getApplicationContext()
     *           .getResources()
     *           .getColor(R.color.notification_accent);
     *   }
     * }
     * </pre>
     * <br>
     * Example - Use default color:
     * <pre>
     * {@code
     *   @Override
     *   public int summaryNotificationColor() {
     *       return -1; // Uses same color as individual notifications
     *   }
     * }
     * </pre>
     *
     * @return Icon accent color in ARGB format (e.g., 0xFFRRGGBB), or -1 to use the default
     *         notification color configured for the app
     */
    @ColorInt
    public abstract int summaryNotificationColor();

    /**
     * Controls whether the summary notification automatically dismisses when the user taps it.
     * <p>
     * By default, summary notifications remain visible after being tapped. Override this method
     * and return true to make the summary notification automatically dismiss, similar to how
     * individual notifications behave.
     * <p>
     * <b>Recommendation:</b> Return true if tapping the summary opens your app's main screen or
     * a list of all items. Return false if the summary remains relevant after interaction.
     * <br><br>
     * Example - Auto-dismiss summary:
     * <pre>
     * {@code
     *   @Override
     *   public boolean autoCancelSummaryNotification() {
     *       return true; // Summary dismisses when user taps it
     *   }
     * }
     * </pre>
     *
     * @return true to automatically dismiss the summary notification when tapped, false to keep
     *         it visible (default behavior)
     */
    public boolean autoCancelSummaryNotification() {
        return false;
    }

    /**
     * Controls whether summary notifications should be generated at all.
     * <p>
     * Override this method to conditionally enable or disable summary notification creation based
     * on app state, user preferences, or other criteria. When this returns false, individual
     * grouped notifications will still appear, but no summary notification will be shown.
     * <p>
     * This is useful for scenarios where you want to disable grouping temporarily without
     * changing the AndroidManifest configuration.
     * <br><br>
     * Example - Conditional summary based on preferences:
     * <pre>
     * {@code
     *   @Override
     *   public boolean shouldGenerateSummaryNotification() {
     *       SharedPreferences prefs = getApplicationContext()
     *           .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
     *       return prefs.getBoolean("enable_grouped_notifications", true);
     *   }
     * }
     * </pre>
     * <br>
     * Example - Disable summary during onboarding:
     * <pre>
     * {@code
     *   @Override
     *   public boolean shouldGenerateSummaryNotification() {
     *       // Don't show summary during first-time user experience
     *       SharedPreferences prefs = getApplicationContext()
     *           .getSharedPreferences("onboarding", Context.MODE_PRIVATE);
     *       return prefs.getBoolean("onboarding_completed", false);
     *   }
     * }
     * </pre>
     *
     * @return true to generate summary notifications (default), false to disable summary creation
     */
    public boolean shouldGenerateSummaryNotification() { return true; }

    @RequiresApi(Build.VERSION_CODES.N)
    public final Notification onGenerateSummaryNotification(int notificationsAmount, String notificationChannelId, String groupId) {
        if (!shouldGenerateSummaryNotification()) {
            return null;
        }
        Context appContext = getApplicationContext();
        if (appContext == null) {
            PWLog.error("onGenerateSummaryNotification " + AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
            return null;
        }

        int smallIcon = summaryNotificationIconResId();
        if (smallIcon == -1) {
            smallIcon = NotificationUtils.tryToGetIconFormStringOrGetFromApplication(null);
        }

        int color = summaryNotificationColor();
        if (color == -1) {
            color = RepositoryModule.getNotificationPreferences().iconBackgroundColor().get();
        }

        String channelId =
                TextUtils.equals(notificationChannelId, NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID)
                        ? addChannel() : notificationChannelId;

        SummaryNotificationBuilder notificationBuilder = NotificationBuilderManager
                .createSummaryNotificationBuilder(getApplicationContext(), channelId);
        notificationBuilder
                .setSmallIcon(smallIcon)
                .setColor(color)
                .setNumber(notificationsAmount)
                .setAutoCancel(autoCancelSummaryNotification())
                .setGroup(groupId)
                .setGroupSummary(true);

        String summaryText = summaryNotificationMessage(notificationsAmount);
        if (!TextUtils.isEmpty(summaryText)) {
            notificationBuilder.setStyle(new NotificationCompat.InboxStyle()
                    .setSummaryText(summaryText));
        }
        return notificationBuilder.build();
    }

    /**
     * @return Intent to start when user clicks on the summary notification
     */
    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static Intent getNotificationIntent() {
        Context context = AndroidPlatformModule.getApplicationContext();
        Intent notifyIntent = new Intent(context, NotificationOpenActivity.class);
        notifyIntent.setAction("summary-" + System.currentTimeMillis());
        return notifyIntent;
    }

    /**
     * @return Application context.
     */
    @Nullable
    protected final Context getApplicationContext() {
        return applicationContext;
    }

    /**
     * Create, if not exist, new default group notifications channel.
     *
     * @return channel id which connected with channel name. For Api less than 26 it doesn't create anything
     */
    private String addChannel() {
        return notificationChannelManager.addGroupNotificationsChannel(DEFAULT_GROUP_CHANNEL_NAME);
    }
}
