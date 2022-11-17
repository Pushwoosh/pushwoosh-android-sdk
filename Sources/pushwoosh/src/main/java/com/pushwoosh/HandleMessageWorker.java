package com.pushwoosh;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.repository.RepositoryModule;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class HandleMessageWorker extends Worker {
    public static final String TAG = "HandleMessageWorker";
    public static final String DATA_PUSH_BUNDLE_ID = "data_push_bundle_id";

    public HandleMessageWorker(@NonNull Context context,
                               @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        long id = getInputData().getLong(DATA_PUSH_BUNDLE_ID, -1);
        if (id == -1) {
            return onFailure();
        }

        Bundle pushBundle;
        try {
            pushBundle = RepositoryModule.getPushBundleStorage().getPushBundle(id);
        } catch (Exception e) {
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
