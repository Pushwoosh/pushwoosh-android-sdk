package com.pushwoosh.internal.registrar;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.utils.PWLog;

public class ExistingTokenRegistrarWorker extends Worker {
    public static final String TAG = "ExistingTokenRegistrarWorker";
    public static final String TOKEN = "token";

    public ExistingTokenRegistrarWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        PWLog.noise(TAG, "ExistingTokenRegistrarWorker doWork");
        try {
            String token = getInputData().getString(TOKEN);
            if (TextUtils.isEmpty(token)) {
                PWLog.error(TAG, "Cannot register for pushes with null token");
                return Result.failure();
            }
            SdkStateProvider.getInstance().executeOrQueue(()->{
                PushwooshPlatform.getInstance().notificationManager().onExistingTokenReceived(token, null);
            });
        } catch (Exception e) {
            PWLog.error(TAG, "Failed to register existing token", e);
            return Result.failure();
        }
        // result is ignored, so we can immediately return success
        return Result.success();
    }
}
