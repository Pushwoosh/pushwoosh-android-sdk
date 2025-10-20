package com.pushwoosh.notification;

import androidx.annotation.ColorInt;

/**
 * Default implementation of {@link SummaryNotificationFactory} provided by the Pushwoosh SDK.
 * <p>
 * This class provides minimal default behavior for grouped notification summaries. It uses the
 * same icon and color as individual notifications and provides no custom summary text. This
 * factory is used automatically when multi-notification mode is enabled but no custom summary
 * factory is registered.
 * <p>
 * <b>Default Behavior:</b>
 * <ul>
 * <li>No summary message text (empty string)</li>
 * <li>Uses the same small icon as individual notifications</li>
 * <li>Uses the same accent color as individual notifications</li>
 * <li>Summary notification does not auto-dismiss when tapped</li>
 * </ul>
 * <p>
 * <b>When to Use:</b> This default factory is suitable if you just want basic notification
 * grouping without custom summary appearance. For better user experience, consider creating your
 * own factory that extends {@link SummaryNotificationFactory} directly and provides meaningful
 * summary text and custom styling.
 * <p>
 * <b>Example - Extend for customization:</b>
 * <pre>
 * {@code
 *   // Instead of using PushwooshSummaryNotificationFactory directly,
 *   // extend SummaryNotificationFactory for full control:
 *
 *   public class MySummaryFactory extends SummaryNotificationFactory {
 *       @Override
 *       public String summaryNotificationMessage(int notificationsAmount) {
 *           return notificationsAmount + " new messages";
 *       }
 *
 *       @Override
 *       public int summaryNotificationIconResId() {
 *           return R.drawable.ic_notification_stack;
 *       }
 *
 *       @Override
 *       public int summaryNotificationColor() {
 *           return 0xFF6200EE;
 *       }
 *   }
 *
 *   // Register in AndroidManifest.xml:
 *   <meta-data
 *       android:name="com.pushwoosh.summary_notification_factory"
 *       android:value=".MySummaryFactory" />
 * }
 * </pre>
 * <p>
 * <b>Note:</b> You typically don't need to extend this class. Instead, extend
 * {@link SummaryNotificationFactory} directly for better control and clearer intent.
 *
 * @see SummaryNotificationFactory
 * @see #summaryNotificationMessage(int)
 * @see #summaryNotificationIconResId()
 * @see #summaryNotificationColor()
 */
public class PushwooshSummaryNotificationFactory extends SummaryNotificationFactory {

    @Override
    public String summaryNotificationMessage(int notificationsAmount) {
        return "";
    }

    @Override
    public int summaryNotificationIconResId() {
        return -1; // default small icon
    }

    @Override
    @ColorInt
    public int summaryNotificationColor() {
        return -1; // default color
    }
}
