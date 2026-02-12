package com.pushwoosh.notification;

import android.app.Notification;
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
        long rowId = intent.getLongExtra(EXTRA_NOTIFICATION_ROW_ID, 0);
        if (rowId > 0) {
            int summaryNotificationId = intent.getIntExtra(EXTRA_GROUP_ID, 0);
            removeGroupPushBundle(rowId);
            if (summaryNotificationId > 0) {
                updateSummaryNotification(summaryNotificationId);
            }
        }
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

        long rowId = intent.getLongExtra(EXTRA_NOTIFICATION_ROW_ID, 0);
        if (rowId != 0) {
            removeGroupPushBundle(rowId);
        }

        int summaryNotificationId = intent.getIntExtra(EXTRA_GROUP_ID, 0);
        if (summaryNotificationId > 0) {
            updateSummaryNotification(summaryNotificationId);
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
                    PWLog.warn("NotificationIntentHelper", "Group ID not found for summary notification: " + summaryNotificationId);
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

    private static void removeGroupPushBundle(long rowId) {
        BackgroundExecutor.execute(() -> {
            Context context = AndroidPlatformModule.getApplicationContext();
            if (context == null) {
                PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
                return;
            }
            RepositoryModule.getPushBundleStorage().removeGroupPushBundle(rowId);
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

    private static void updateSummaryNotification(int notificationId) {
        BackgroundExecutor.execute(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                String groupId = getSummaryNotificationGroupForId(notificationId);
                if (groupId == null) {
                    return;
                }

                List<StatusBarNotification> activeNotifications =
                        NotificationBuilderManager.getActiveNotificationsForGroup(groupId);
                if (activeNotifications == null) {
                    return;
                }
                /*if group summary had 2 notifications, and one was closed, it will still exist,
                but the last notification will be shown separately, so we need to cancel summary here */
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
        });
    }

    private static String getSummaryNotificationGroupForId(int notificationId) {
        try {
            return RepositoryModule.getSummaryNotificationStorage().getGroup(notificationId);
        } catch (NotificationIdNotFoundException e) {
            return null;
        }
    }

}
