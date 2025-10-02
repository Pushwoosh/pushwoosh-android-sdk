package com.pushwoosh;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

import java.lang.reflect.Method;

public final class PushwooshWorkManagerHelper {
    public static void enqueueOneTimeUniqueWork(OneTimeWorkRequest request, String uniqueWorkName, ExistingWorkPolicy policy) {
        try {
            getWorkManager().enqueueUniqueWork(uniqueWorkName, policy, request);
        } catch (Exception e) {
            PWLog.error("Failed to enqueue work.", e);
        }
    }

    public static void enqueuePeriodicUniqueWork(PeriodicWorkRequest request, String uniqueWorkName, ExistingPeriodicWorkPolicy policy) {
        try {
            getWorkManager().enqueueUniquePeriodicWork(uniqueWorkName, policy, request);
        } catch (Exception e) {
            PWLog.error("Failed to enqueue periodic work.", e);
        }
    }

    public static void cancelPeriodicUniqueWork(String uniqueWorkName) {
        try {
            getWorkManager().cancelUniqueWork(uniqueWorkName);
        } catch (Exception e) {
            PWLog.error("Failed to cancel unique periodic work.", e);
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

    /**
     * Get optimized constraints for statistics delivery.
     * These constraints allow execution in low battery, Doze Mode, etc. 
     * to ensure statistics are delivered reliably.
     */
    public static Constraints getStatisticsConstraints() {
        return new Constraints.Builder()
                // Require network connection
                .setRequiredNetworkType(NetworkType.CONNECTED)
                
                // Allow execution with low battery - statistics are critical
                .setRequiresBatteryNotLow(false)
                
                // Allow execution in Doze Mode
                .setRequiresDeviceIdle(false)
                
                // Allow execution with low storage
                .setRequiresStorageNotLow(false)
                
                // Allow execution without charging
                .setRequiresCharging(false)
                
                .build();
    }
}
