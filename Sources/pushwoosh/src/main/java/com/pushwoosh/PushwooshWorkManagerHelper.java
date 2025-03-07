package com.pushwoosh;

import android.content.Context;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

import java.lang.reflect.Method;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public final class PushwooshWorkManagerHelper {
    public static void enqueueOneTimeUniqueWork(OneTimeWorkRequest request, String uniqueWorkName, ExistingWorkPolicy policy) {
        try {
            getWorkManager().enqueueUniqueWork(uniqueWorkName, policy, request);
        } catch (Exception e) {
            PWLog.error("Failed to enqueue work.");
            e.printStackTrace();
        }
    }

    public static void enqueuePeriodicUniqueWork(PeriodicWorkRequest request, String uniqueWorkName, ExistingPeriodicWorkPolicy policy) {
        try {
            getWorkManager().enqueueUniquePeriodicWork(uniqueWorkName, policy, request);
        } catch (Exception e) {
            PWLog.error("Failed to enqueue periodic work.");
            e.printStackTrace();
        }
    }

    public static void cancelPeriodicUniqueWork(String uniqueWorkName) {
        try {
            getWorkManager().cancelUniqueWork(uniqueWorkName);
        } catch (Exception e) {
            PWLog.error("Failed to cancel unique periodic work.");
            e.printStackTrace();
        }
    }

    private static WorkManager getWorkManager() throws Exception {
        try {
            Method getInstanceMethod = WorkManager.class.getMethod("getInstance", Context.class);
            return (WorkManager) getInstanceMethod.invoke(null, AndroidPlatformModule.getApplicationContext());
        } catch (NoSuchMethodException | NullPointerException e) {
            if (e instanceof NullPointerException) {
                PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
            }
            return WorkManager.getInstance();
        }
    }

    public static Constraints getNetworkAvailableConstraints() {
        return new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
    }
}
