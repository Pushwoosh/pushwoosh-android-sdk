package com.pushwoosh;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.pushwoosh.internal.network.CachedRequest;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.network.RequestStorage;
import com.pushwoosh.internal.work.BasePushwooshWorker;
import com.pushwoosh.repository.RepositoryModule;

public class SendCachedRequestWorker extends BasePushwooshWorker {
    public static final String TAG = "SendCachedRequestWorker";
    public static final String DATA_CACHED_REQUEST_ID = "data_cached_request_id";
    private static final int RETRY_COUNT = 3;

    public SendCachedRequestWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override
    protected String getLogTag() {
        return TAG;
    }

    @NonNull @Override
    public Result doWork() {
        long id = getInputData().getLong(DATA_CACHED_REQUEST_ID, -1);
        if (id == -1) {
            return onFail();
        }

        RequestStorage requestStorage = RepositoryModule.getRequestStorage();
        if (requestStorage == null) {
            return onFail();
        }
        CachedRequest cachedRequest = requestStorage.get(id);
        if (cachedRequest == null) {
            return onFail();
        }

        RequestManager requestManager = NetworkModule.getRequestManager();
        if (requestManager == null) {
            return onFail();
        }

        com.pushwoosh.function.Result<Void, NetworkException> result = requestManager.sendRequestSync(cachedRequest);
        if (!result.isSuccess() && result.getException() instanceof ConnectionException) {
            return onFail();
        }

        requestStorage.remove(cachedRequest.getKey());
        return Result.success();
    }

    private Result onFail() {
        if (getRunAttemptCount() >= RETRY_COUNT) {
            return Result.success(); // dropping the request not to fail SendCachedRequestWorkers sequence
        }
        return Result.retry();
    }
}
