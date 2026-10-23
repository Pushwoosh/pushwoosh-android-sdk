package com.pushwoosh.internal.registrar;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.work.WorkerParameters;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.work.BasePushwooshWorker;
import com.pushwoosh.repository.RepositoryModule;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Delivers the device's saved push token to the server in a retrying WorkManager job, scheduled
 * by {@link com.pushwoosh.repository.DeviceRegistrar#scheduleRegistration(String)}.
 */
public class RegisterDeviceWorker extends BasePushwooshWorker {
    public static final String TAG = "RegisterDeviceWorker";
    public static final String DATA_TAGS = "tags";
    public static final long DEFAULT_SDK_READY_TIMEOUT_MILLIS = 30_000L;
    private static final long SDK_READY_POLL_MILLIS = 100L;

    @VisibleForTesting
    public static long sdkReadyTimeoutMillis = DEFAULT_SDK_READY_TIMEOUT_MILLIS;

    public RegisterDeviceWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull @Override
    protected String getLogTag() {
        return TAG;
    }

    @NonNull @Override
    public Result doWork() {
        SdkStateProvider state = SdkStateProvider.getInstance();
        if (state.getCurrentState() == SdkStateProvider.SdkState.ERROR) {
            PWLog.debug(TAG, "SDK initialization failed, retrying the delivery later");
            return Result.retry();
        }
        boolean ready = awaitSdkReady(state);
        if (isStopped()) {
            PWLog.debug(TAG, "Worker stopped while waiting for the SDK, dropping the delivery");
            return Result.failure();
        }
        if (!ready) {
            PWLog.debug(TAG, "SDK is not ready yet, retrying the delivery later");
            return Result.retry();
        }

        String token = RepositoryModule.getRegistrationPreferences().pushToken().get();
        if (TextUtils.isEmpty(token)) {
            PWLog.debug(TAG, "No push token saved, nothing to deliver");
            return Result.success();
        }

        com.pushwoosh.function.Result<Void, NetworkException> result = PushwooshPlatform.getInstance()
                .deviceRegistrar()
                .registerSavedToken(getInputData().getString(DATA_TAGS));
        if (result.isSuccess()) {
            PWLog.debug(TAG, "Token delivered on attempt " + getRunAttemptCount());
            return Result.success();
        }

        NetworkException e = result.getException();
        boolean transientFailure = e instanceof ConnectionException && ((ConnectionException) e).isTransient();
        PWLog.debug(
                TAG,
                "Token delivery failed on attempt " + getRunAttemptCount()
                        + (transientFailure ? ", retrying later" : ", giving up"));
        return transientFailure ? Result.retry() : Result.failure();
    }

    private boolean awaitSdkReady(SdkStateProvider state) {
        CountDownLatch ready = new CountDownLatch(1);
        state.executeOrQueue(ready::countDown);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(sdkReadyTimeoutMillis);
        try {
            // polled: setError() drops the queued task, so the latch never fires on ERROR
            while (!ready.await(SDK_READY_POLL_MILLIS, TimeUnit.MILLISECONDS)) {
                if (isStopped()
                        || state.getCurrentState() == SdkStateProvider.SdkState.ERROR
                        || System.nanoTime() >= deadline) {
                    return false;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return state.isReady();
    }
}
