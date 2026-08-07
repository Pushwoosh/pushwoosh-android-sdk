package com.pushwoosh.function;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.utils.PWLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RetriableRequestCallback<Response> implements Callback<Response, NetworkException> {
    private final Callback<Response, NetworkException> callback;
    private final PushRequest<Response> request;
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final int[] RETRY_DELAYS_SECONDS = {1, 5, 10};

    public RetriableRequestCallback(Callback<Response, NetworkException> callback, PushRequest<Response> request) {
        this.callback = callback;
        this.request = request;
    }

    @Override
    public void process(@NonNull Result<Response, NetworkException> result) {
        if (!shouldRetry(result)) {
            safeProcessCallback(callback, result);
            return;
        }

        retryRequest(0, result);
    }

    private boolean shouldRetry(Result<Response, NetworkException> result) {
        return !result.isSuccess()
                && result.getException() instanceof ConnectionException
                && ((ConnectionException) result.getException()).isTransient();
    }

    private void retryRequest(final int attempt, final Result<Response, NetworkException> lastResult) {
        try {
            if (attempt >= RETRY_DELAYS_SECONDS.length) {
                safeProcessCallback(callback, lastResult);
                return;
            }

            long delay = RETRY_DELAYS_SECONDS[attempt];
            PWLog.debug("Scheduling retry attempt " + (attempt + 1) + " with a delay of " + delay + " seconds");

            executor.schedule(
                    () -> {
                        Result<Response, NetworkException> result =
                                NetworkModule.getRequestManager().sendRequestSync(request);

                        if (shouldRetry(result)) {
                            retryRequest(attempt + 1, result);
                        } else {
                            safeProcessCallback(callback, result);
                        }
                    },
                    delay,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            PWLog.error("Failed to retry request " + request.getMethod(), e);
            safeProcessCallback(callback, Result.fromException(new NetworkException(e.getMessage())));
        }
    }

    private void safeProcessCallback(
            Callback<Response, NetworkException> callback, Result<Response, NetworkException> result) {
        if (callback != null) {
            callback.process(result);
        }
    }
}
