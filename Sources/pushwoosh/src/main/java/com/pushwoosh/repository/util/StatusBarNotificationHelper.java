package com.pushwoosh.repository.util;

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
import java.util.Collections;
import java.util.List;

public class StatusBarNotificationHelper {
    private static final String TAG = "StatusBarNotificationStorageHelper";

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static List<Pair<Long,Integer>> getActiveNotificationsIds() {
        List<Pair<Long, Integer>> idsPairs = new ArrayList<>();
        NotificationManager nm = AndroidPlatformModule.getManagerProvider().getNotificationManager();
        if  (nm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                StatusBarNotification[] notifications = nm.getActiveNotifications();

                for (StatusBarNotification notification : notifications) {
                    String notificationString = notification.getNotification().toString();
                    Bundle pushBundle = JsonUtils.jsonStringToBundle(notificationString, true);
                    PushMessage message = new PushMessage(pushBundle);
                    if (message.getPushwooshNotificationId() != -1) {
                        idsPairs.add(Pair.create(message.getPushwooshNotificationId(), notification.getId()));
                    }
                }
                return idsPairs;
            } catch (Exception e) {
                PWLog.error(TAG,"Failed to get list of active notifications");
                return Collections.emptyList();
            }
        } else return Collections.emptyList();
    }
}
