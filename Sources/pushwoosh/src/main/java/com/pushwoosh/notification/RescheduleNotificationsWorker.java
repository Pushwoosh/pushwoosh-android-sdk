package com.pushwoosh.notification;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.TimeProvider;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.RepositoryModule;

public class RescheduleNotificationsWorker extends Worker {
    public static final String TAG = "RescheduleNotificationsWorker";

    public RescheduleNotificationsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    private static long getCurrentTime() {
        TimeProvider timeProvider = AndroidPlatformModule.getInstance().getTimeProvider();
        if (timeProvider != null) {
            return timeProvider.getCurrentTime();
        } else {
            return System.currentTimeMillis();
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        long currentTime = getCurrentTime();
        LocalNotificationStorage storage = RepositoryModule.getLocalNotificationStorage();
        if (storage == null) {
            PWLog.error(TAG, "LocalNotificationStorage is null, can't reschedule notifications");
            return Result.failure();
        }
        storage.enumerateDbLocalNotificationList(dbLocalNotification -> {
            Bundle bundle = dbLocalNotification.getBundle();
            PWLog.debug(TAG, "Rescheduling local push: " + bundle.toString());
            LocalNotificationReceiver.rescheduleNotification(dbLocalNotification, currentTime);
        });
        return Result.success();
    }
}
