package com.pushwoosh.notification.handlers.message.system;

import android.app.Notification;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.pushwoosh.exception.NotificationIdNotFoundException;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.notification.SummaryNotificationFactory;
import com.pushwoosh.notification.SummaryNotificationUtils;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.StatusBarNotificationStorage;

import java.util.List;

public class CancelNotificationHandler implements MessageSystemHandler{
    String TAG = CancelNotificationHandler.class.getSimpleName();

    public CancelNotificationHandler() {/*do nothing*/}

    @Override
    public boolean preHandleMessage(Bundle pushBundle) {
        String cancelId = pushBundle.getString("CancelID");
        String groupId = PushBundleDataProvider.getGroupId(pushBundle);
        if (cancelId != null) {
            StatusBarNotificationStorage storage = RepositoryModule.getStatusBarNotificationStorage();
            int notificationId;
            try {
                notificationId = storage.remove(Long.parseLong(cancelId));
                try {
                    AndroidPlatformModule.getManagerProvider().getNotificationManager().cancel(notificationId);
                    updateSummaryNotification(groupId);
                    return true;
                } catch (Exception e) {
                    PWLog.error(TAG, "Failed to cancel notification with ID: " + cancelId + "." + e.getMessage());
                    return false;
                }
            } catch (NotificationIdNotFoundException notificationIdNotFoundException) {
                // Notification not found, resuming chain;
                return false;
            }
        }
        return false;
    }

    private void updateSummaryNotification(String groupId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            List<StatusBarNotification> activeNotifications = NotificationBuilderManager.getActiveNotificationsForGroup(groupId);
            if (activeNotifications.isEmpty()) {
                NotificationBuilderManager.cancelGroupSummary(groupId);
                return;
            }
            Notification summaryNotification =
                    SummaryNotificationUtils.getSummaryNotification(activeNotifications.size(),
                            SummaryNotificationFactory.NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID, groupId);
            if (summaryNotification != null) {
                SummaryNotificationUtils.fireSummaryNotification(summaryNotification);
            }
        }
    }
}
