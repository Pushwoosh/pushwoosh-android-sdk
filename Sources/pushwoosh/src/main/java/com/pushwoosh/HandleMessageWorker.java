package com.pushwoosh;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.work.BasePushwooshWorker;
import com.pushwoosh.repository.RepositoryModule;

public class HandleMessageWorker extends BasePushwooshWorker {
    public static final String TAG = "HandleMessageWorker";
    public static final String DATA_PUSH_BUNDLE_ID = "data_push_bundle_id";

    public HandleMessageWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override
    protected String getLogTag() {
        return TAG;
    }

    @NonNull @Override
    public Result doWork() {
        long id = getInputData().getLong(DATA_PUSH_BUNDLE_ID, -1);
        if (id == -1) {
            return onFailure();
        }

        Bundle pushBundle;
        try {
            pushBundle = RepositoryModule.getPushBundleStorage().getPushBundle(id);
        } catch (Throwable e) {
            return onFailure();
        }
        if (pushBundle == null) {
            return onFailure();
        }

        RepositoryModule.getPushBundleStorage().removePushBundle(id);
        NotificationRegistrarHelper.handleMessageBundle(pushBundle);
        return Result.success();
    }

    private Result onFailure() {
        return getRunAttemptCount() < 2 ? Result.retry() : Result.success();
    }
}
