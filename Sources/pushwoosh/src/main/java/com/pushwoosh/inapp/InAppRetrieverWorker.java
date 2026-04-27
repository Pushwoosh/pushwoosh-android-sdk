package com.pushwoosh.inapp;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.pushwoosh.inapp.network.InAppRepository;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.work.BasePushwooshWorker;

public class InAppRetrieverWorker extends BasePushwooshWorker {
    public static final String TAG = "InAppRetrieverWorker";

    public InAppRetrieverWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override
    protected String getLogTag() {
        return TAG;
    }

    @NonNull @Override
    public Result doWork() {
        try {
            doLoadInApps();
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to load in-apps", e);
        }
        return Result.success();
    }

    private static void doLoadInApps() {
        InAppRepository inAppRepository = InAppModule.getInAppRepository();
        if (inAppRepository == null) {
            return;
        }
        inAppRepository.loadInApps();
    }
}
