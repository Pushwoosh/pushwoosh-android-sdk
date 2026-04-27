package com.pushwoosh.internal.work;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.work.WorkInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pushwoosh.internal.utils.PWLog;

/**
 * Base class for all Pushwoosh SDK WorkManager workers.
 * <p>
 * Adds diagnostic logging of stop reasons ({@link ListenableWorker#getStopReason()} on API 31+),
 * so that saas/support can distinguish between quota exhaustion, connectivity loss,
 * timeouts, and other WorkManager-driven cancellations when investigating prod issues.
 */
public abstract class BasePushwooshWorker extends Worker {

    public BasePushwooshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /**
     * Log tag used by {@link #onStopped()}. Each subclass must return its own static literal
     * so logs survive R8 obfuscation regardless of consumer-rules configuration.
     */
    @NonNull protected abstract String getLogTag();

    @Override
    public void onStopped() {
        super.onStopped();
        String tag = getLogTag();
        int attempt = getRunAttemptCount();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PWLog.warn(tag, "Worker stopped, reason=" + stopReasonToString(getStopReason()) + ", attempt=" + attempt);
        } else {
            PWLog.warn(tag, "Worker stopped, attempt=" + attempt);
        }
    }

    @VisibleForTesting
    static String stopReasonToString(int reason) {
        switch (reason) {
            case WorkInfo.STOP_REASON_NOT_STOPPED:
                return "NOT_STOPPED";
            case WorkInfo.STOP_REASON_UNKNOWN:
                return "UNKNOWN";
            case WorkInfo.STOP_REASON_CANCELLED_BY_APP:
                return "CANCELLED_BY_APP";
            case WorkInfo.STOP_REASON_PREEMPT:
                return "PREEMPT";
            case WorkInfo.STOP_REASON_TIMEOUT:
                return "TIMEOUT";
            case WorkInfo.STOP_REASON_DEVICE_STATE:
                return "DEVICE_STATE";
            case WorkInfo.STOP_REASON_CONSTRAINT_BATTERY_NOT_LOW:
                return "CONSTRAINT_BATTERY_NOT_LOW";
            case WorkInfo.STOP_REASON_CONSTRAINT_CHARGING:
                return "CONSTRAINT_CHARGING";
            case WorkInfo.STOP_REASON_CONSTRAINT_CONNECTIVITY:
                return "CONSTRAINT_CONNECTIVITY";
            case WorkInfo.STOP_REASON_CONSTRAINT_DEVICE_IDLE:
                return "CONSTRAINT_DEVICE_IDLE";
            case WorkInfo.STOP_REASON_CONSTRAINT_STORAGE_NOT_LOW:
                return "CONSTRAINT_STORAGE_NOT_LOW";
            case WorkInfo.STOP_REASON_QUOTA:
                return "QUOTA";
            case WorkInfo.STOP_REASON_BACKGROUND_RESTRICTION:
                return "BACKGROUND_RESTRICTION";
            case WorkInfo.STOP_REASON_APP_STANDBY:
                return "APP_STANDBY";
            case WorkInfo.STOP_REASON_USER:
                return "USER";
            case WorkInfo.STOP_REASON_SYSTEM_PROCESSING:
                return "SYSTEM_PROCESSING";
            case WorkInfo.STOP_REASON_ESTIMATED_APP_LAUNCH_TIME_CHANGED:
                return "ESTIMATED_APP_LAUNCH_TIME_CHANGED";
            default:
                return "CODE_" + reason;
        }
    }
}
