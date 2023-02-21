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
     * Override this method to set your custom message of the group summary notification.
     *
     * @param notificationsAmount - number of the notifications in the group summary
     *
     * @return Group summary notification message. By default returns "{@param notificationsAmount} new messages".
     */

    public abstract String summaryNotificationMessage(int notificationsAmount);

    /**
     * Override this method to set your drawable as an icon of the group summary notification.
     *
     * @return Drawable resource id which will appear as an icon of the group summary notification.
     * By default returns -1 which is used to set the same icon as a common notification.
     */
    public abstract int summaryNotificationIconResId();

    /**
     * Override this method to set the icon color.
     *
     * @return The accent color to use.
     * By default returns -1 which is used to set the same color as a common notification.
     */
    @ColorInt
    public abstract int summaryNotificationColor();

    /**
     * Custom notification groups are not currently supported.
     * We strongly discourage you from using this override unless it is absolutely necessary for your use-case.
     *
     * @return The summary notification group key.
     * By default returns the "group_undefined" value.
     */
    public abstract String summaryNotificationGroup();

    /**
     * Override this method to set whether the summary notification will be dismissed after the user opens it.
     *
     * @return The flag indicating whether the group summary notification would be cancelled automatically. By default returns false.
     */
    public boolean autoCancelSummaryNotification() {
        return false;
    }

    public boolean shouldGenerateSummaryNotification() { return true; }

    @RequiresApi(Build.VERSION_CODES.N)
    public final Notification onGenerateSummaryNotification(int notificationsAmount, String notificationChannelId) {
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
                .setGroup(summaryNotificationGroup())
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
