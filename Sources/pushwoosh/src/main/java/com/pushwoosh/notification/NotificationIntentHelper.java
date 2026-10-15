package com.pushwoosh.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.exception.NotificationIdNotFoundException;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.repository.PushBundleStorage;
import com.pushwoosh.repository.RepositoryModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationIntentHelper {

    public static final String EXTRA_NOTIFICATION_BUNDLE = "pushBundle";
    public static final String EXTRA_GROUP_ID = "group_id";
    public static final String EXTRA_NOTIFICATION_ROW_ID = "row_id";
    public static final String EXTRA_IS_DELETE_INTENT = "is_delete_intent";
    public static final String EXTRA_IS_SUMMARY_NOTIFICATION = "is_summary_notification";
    public static final String EXTRA_PUSHWOOSH_NOTIFICATION_ID = "pushwoosh_notification_id";
    public static final int SUMMARY_GROUP_ID = 20191017;

    public static void processIntent(Context context, Intent intent, Runnable onComplete) {
        if (intent == null) {
            onComplete.run();
            return;
        }
        if (intent.getBooleanExtra(EXTRA_IS_DELETE_INTENT, false)) {
            PWLog.debug("NotificationIntentHelper", "processIntent: delete intent");
            handleDeleteIntent(intent);
            onComplete.run();
        } else if (ActionButtonIntents.getTappedButtonId(intent) != null) {
            PWLog.debug("NotificationIntentHelper", "processIntent: action button click");
            handleActionButton(intent);
            onComplete.run();
        } else if (intent.getBooleanExtra(EXTRA_IS_SUMMARY_NOTIFICATION, false)) {
            PWLog.debug("NotificationIntentHelper", "processIntent: summary notification click");
            handleNotificationGroup(intent, onComplete);
        } else {
            PWLog.debug("NotificationIntentHelper", "processIntent: single notification click");
            handleNotification(intent);
            onComplete.run();
        }
    }

    private static void handleDeleteIntent(Intent intent) {
        try {
            Bundle pushBundle = intent.getBundleExtra(EXTRA_NOTIFICATION_BUNDLE);
            NotificationServiceExtension notificationServiceExtension =
                    PushwooshPlatform.getInstance().notificationService();
            notificationServiceExtension.handleNotificationCanceled(pushBundle);
        } catch (Exception e) {
            PWLog.exception(e);
        }
        if (intent.getBooleanExtra(EXTRA_IS_SUMMARY_NOTIFICATION, false)) {
            BackgroundExecutor.execute(
                    () -> RepositoryModule.getPushBundleStorage().removeGroupPushBundles());
            return;
        }
        finishGroupedNotification(
                intent.getLongExtra(EXTRA_NOTIFICATION_ROW_ID, 0), intent.getIntExtra(EXTRA_GROUP_ID, 0));
    }

    private static void handleNotification(Intent intent) {
        try {
            Bundle pushBundle = intent.getBundleExtra(EXTRA_NOTIFICATION_BUNDLE);
            NotificationServiceExtension notificationServiceExtension =
                    PushwooshPlatform.getInstance().notificationService();
            notificationServiceExtension.handleNotification(pushBundle);
        } catch (Exception e) {
            PWLog.exception(e);
        }

        finishNotificationIntent(intent);
    }

    private static void handleActionButton(Intent intent) {
        // An action button is not auto-cancel, unlike the notification body.
        cancelNotification(intent);

        try {
            Bundle pushBundle = intent.getBundleExtra(EXTRA_NOTIFICATION_BUNDLE);
            NotificationServiceExtension notificationServiceExtension =
                    PushwooshPlatform.getInstance().notificationService();
            notificationServiceExtension.handleNotificationButton(
                    pushBundle, ActionButtonIntents.getTappedButtonId(intent));
        } catch (Exception e) {
            PWLog.exception(e);
        }

        finishNotificationIntent(intent);
    }

    private static void finishNotificationIntent(Intent intent) {
        finishGroupedNotification(
                intent.getLongExtra(EXTRA_NOTIFICATION_ROW_ID, 0), intent.getIntExtra(EXTRA_GROUP_ID, 0));
    }

    private static void cancelNotification(Intent intent) {
        try {
            NotificationManager manager =
                    AndroidPlatformModule.getManagerProvider().getNotificationManager();
            if (manager == null) {
                return;
            }
            manager.cancel(
                    ActionButtonIntents.getNotificationTag(intent), ActionButtonIntents.getNotificationId(intent));
        } catch (Exception e) {
            PWLog.exception(e);
        }
    }

    private static void handleNotificationGroup(Intent intent, Runnable onComplete) {
        int summaryNotificationId = intent.getIntExtra(EXTRA_GROUP_ID, 0);
        if (summaryNotificationId == 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            onComplete.run();
            return;
        }

        BackgroundExecutor.execute(() -> {
            try {
                String groupId = getSummaryNotificationGroupForId(summaryNotificationId);
                if (groupId == null) {
                    PWLog.warn(
                            "NotificationIntentHelper",
                            "Group ID not found for summary notification: " + summaryNotificationId);
                    BackgroundExecutor.main(() -> {
                        onGetGroupPushMessagesFail(intent);
                        onComplete.run();
                    });
                    return;
                }

                List<PushMessage> messages = getGroupPushMessages(groupId);
                NotificationBuilderManager.cancelLastStatusBarNotificationForGroup(groupId);

                BackgroundExecutor.main(() -> {
                    if (!messages.isEmpty()) {
                        onGetGroupPushMessagesSuccess(messages);
                    } else {
                        onGetGroupPushMessagesFail(intent);
                    }
                    onComplete.run();
                });
            } catch (Throwable t) {
                PWLog.exception(t);
                BackgroundExecutor.main(() -> {
                    onGetGroupPushMessagesFail(intent);
                    onComplete.run();
                });
            }
        });
    }

    private static void onGetGroupPushMessagesSuccess(List<PushMessage> pushMessages) {
        try {
            NotificationServiceExtension notificationServiceExtension =
                    PushwooshPlatform.getInstance().notificationService();
            notificationServiceExtension.handleNotificationGroup(pushMessages);
        } catch (Exception e) {
            PWLog.exception(e);
        }
    }

    private static void onGetGroupPushMessagesFail(Intent intent) {
        handleNotification(intent);
    }

    // The row is the primary source of the group; a missing row (e.g. a failed insert) falls back to the summary id.
    private static void finishGroupedNotification(long rowId, int summaryNotificationId) {
        if (rowId <= 0 && summaryNotificationId <= 0) {
            return;
        }
        BackgroundExecutor.execute(() -> {
            String groupId = null;
            if (rowId > 0) {
                PushBundleStorage storage = RepositoryModule.getPushBundleStorage();
                groupId = storage.getGroupPushBundleGroupId(rowId);
                try {
                    storage.removeGroupPushBundle(rowId);
                } catch (Throwable t) {
                    // A failed delete must still leave the summary rebuild below reachable.
                    PWLog.error("NotificationIntentHelper", "Failed to remove group push bundle row " + rowId, t);
                }
            }
            if (groupId == null && summaryNotificationId > 0) {
                groupId = getSummaryNotificationGroupForId(summaryNotificationId);
            }
            if (groupId != null) {
                rebuildSummaryNotification(groupId);
            }
        });
    }

    private static List<PushMessage> getGroupPushMessages(String groupId) {
        List<Bundle> pushBundles = RepositoryModule.getPushBundleStorage().getGroupPushBundles();
        if (pushBundles == null || pushBundles.isEmpty()) {
            return Collections.emptyList();
        }
        List<PushMessage> messages = new ArrayList<>();
        for (Bundle pushBundle : pushBundles) {
            PushMessage pushMessage = new PushMessage(pushBundle);
            if (groupId.equals(pushMessage.getGroupId())) {
                messages.add(pushMessage);
            }
        }
        return messages;
    }

    private static void rebuildSummaryNotification(String groupId) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return;
        }
        // Building a summary mints its id when the group has none: a lone child must not spawn one.
        if (SummaryNotificationUtils.getNotificationIdForGroup(groupId) == -1) {
            return;
        }
        List<StatusBarNotification> activeNotifications =
                NotificationBuilderManager.getActiveNotificationsForGroup(groupId);
        if (activeNotifications == null) {
            return;
        }
        if (activeNotifications.isEmpty()) {
            NotificationBuilderManager.cancelGroupSummary(groupId);
            return;
        }
        Notification summaryNotification = SummaryNotificationUtils.getSummaryNotification(
                activeNotifications.size(),
                SummaryNotificationFactory.NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID,
                groupId);
        if (summaryNotification == null) {
            return;
        }
        SummaryNotificationUtils.fireSummaryNotification(summaryNotification);
    }

    private static String getSummaryNotificationGroupForId(int notificationId) {
        try {
            return RepositoryModule.getSummaryNotificationStorage().getGroup(notificationId);
        } catch (NotificationIdNotFoundException e) {
            return null;
        }
    }
}
