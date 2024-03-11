package com.pushwoosh.repository.util;

import static com.pushwoosh.notification.NotificationIntentHelper.EXTRA_IS_SUMMARY_NOTIFICATION;

import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Pair;

import androidx.annotation.RequiresApi;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StatusBarNotificationHelper {
    private static final String TAG = "StatusBarNotificationStorageHelper";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static List<Pair<Long,Integer>> getActiveNotificationsIds() {
        List<Pair<Long, Integer>> idsPairs = new ArrayList<>();
        StatusBarNotification[] notifications = getStatusBarNotifications();

        for (StatusBarNotification notification : notifications) {
            String notificationString = notification.getNotification().toString();
            Bundle pushBundle = JsonUtils.jsonStringToBundle(notificationString, true);
            PushMessage message = new PushMessage(pushBundle);
            if (message.getPushwooshNotificationId() != -1) {
                idsPairs.add(Pair.create(message.getPushwooshNotificationId(), notification.getId()));
            }
        }
        return idsPairs;
    }

    public static List<Pair<String, Integer>> getSummaryNotificationsIds() {
        List<Pair<String, Integer>> idsPairs = new ArrayList<>();
        StatusBarNotification[] notifications = getStatusBarNotifications();
        for (StatusBarNotification notification : notifications) {
            if (notification.getNotification().extras.getBoolean(EXTRA_IS_SUMMARY_NOTIFICATION) == true) {
                String notificationGroupId = notification.getNotification().getGroup();
                idsPairs.add(Pair.create(notificationGroupId, notification.getId()));
            }
        }
        return idsPairs;
    }

    private static StatusBarNotification[] getStatusBarNotifications() {
        NotificationManager nm = AndroidPlatformModule.getManagerProvider().getNotificationManager();
        StatusBarNotification[] notifications;
        if (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                notifications = nm.getActiveNotifications();
                return notifications;
            } catch (Exception e) {
                PWLog.error(TAG, "Failed to get list of active notifications");
                return new StatusBarNotification[]{};
            }
        } return new StatusBarNotification[]{};
    }
}
