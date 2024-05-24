package com.pushwoosh.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import com.pushwoosh.NotificationUpdateReceiver;
import com.pushwoosh.exception.GroupIdNotFoundException;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.notification.channel.NotificationChannelManager;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import static com.pushwoosh.repository.NotificationPrefs.DEFAULT_GROUP_CHANNEL_NAME;

public class SummaryNotificationUtils {
    private final static Object MUTEX = new Object();
    private final static NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
    @RequiresApi(api = Build.VERSION_CODES.N)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    public static Notification getSummaryNotification(int activeNotificationsCount, String notificationChannelId, String groupId) {
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
            return null;
        }
        Notification summaryNotification = provideSummaryNotificationFactory().onGenerateSummaryNotification(activeNotificationsCount, notificationChannelId, groupId);
        if (summaryNotification == null) {
            return null;
        }

        int messageId = getOrGenerateNotificationIdForGroup(groupId);
        Intent summaryContentIntent = SummaryNotificationFactory.getNotificationIntent();
        summaryContentIntent.putExtra(NotificationIntentHelper.EXTRA_GROUP_ID, messageId);
        summaryContentIntent.putExtra(NotificationIntentHelper.EXTRA_IS_SUMMARY_NOTIFICATION, true);
        summaryNotification.extras.putBoolean(NotificationIntentHelper.EXTRA_IS_SUMMARY_NOTIFICATION,true);
        summaryNotification.contentIntent = PendingIntent.getActivity(context, messageId, summaryContentIntent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_CANCEL_CURRENT));
        summaryNotification.deleteIntent = PendingIntent.getBroadcast(context, messageId, getSummaryDeleteIntent(), PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_CANCEL_CURRENT));
        return summaryNotification;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static void fireSummaryNotification(@Nullable Notification notification) {
        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
            return;
        }

        NotificationManager manager = AndroidPlatformModule.getManagerProvider().getNotificationManager();

        if (manager == null || notification == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = Notification.Builder.recoverBuilder(context, notification)
                    .setChannelId(new NotificationChannelManager(context).addGroupNotificationsChannel(DEFAULT_GROUP_CHANNEL_NAME))
                    .build();
        }

        manager.notify(getOrGenerateNotificationIdForGroup(notification.getGroup()), notification);
    }

    @NonNull
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    private static Intent getSummaryDeleteIntent() {
        Context applicationContext = AndroidPlatformModule.getApplicationContext();
        Intent deleteIntent = new Intent(applicationContext, NotificationUpdateReceiver.class);
        deleteIntent.putExtra(NotificationIntentHelper.EXTRA_IS_SUMMARY_NOTIFICATION, true);
        deleteIntent.putExtra(NotificationIntentHelper.EXTRA_IS_DELETE_INTENT, true);
        deleteIntent.setAction(Long.toString(System.currentTimeMillis()));
        return deleteIntent;
    }

    private static SummaryNotificationFactory provideSummaryNotificationFactory() {
        NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
        try {
            Class<?> clazz = notificationPrefs.summaryNotificationFactoryClass().get();
            if (clazz != null) {
                return (SummaryNotificationFactory) clazz.newInstance();
            }
        } catch (Exception e) {
            PWLog.exception(e);
        }

        return new PushwooshSummaryNotificationFactory();
    }

    private static int getOrGenerateNotificationIdForGroup(String groupId) {
        int messageId = getNotificationIdForGroup(groupId);
        if (messageId == -1) {
            synchronized (MUTEX) {
                messageId = notificationPrefs.messageId().get();

                if (notificationPrefs.multiMode().get()) {
                    messageId++;
                    notificationPrefs.messageId().set(messageId);
                    RepositoryModule.getSummaryNotificationStorage().put(groupId, messageId);
                }
            }
        }
        return messageId;
    }

    public static int getNotificationIdForGroup(String groupId) {
        try {
            return RepositoryModule.getSummaryNotificationStorage().getNotificationId(groupId);
        } catch (GroupIdNotFoundException e) {
            return -1;
        }
    }
}
