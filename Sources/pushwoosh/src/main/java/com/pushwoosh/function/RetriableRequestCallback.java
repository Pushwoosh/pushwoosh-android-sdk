package com.pushwoosh.function;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.utils.PWLog;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RetriableRequestCallback<Response> implements Callback<Response, NetworkException> {
    private final Callback<Response, NetworkException> callback;
    private final PushRequest<Response> request;
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static final int[] RETRY_DELAYS_SECONDS = { 1, 5, 10 };

    public RetriableRequestCallback(Callback<Response,NetworkException> callback, PushRequest<Response> request) {
        this.callback = callback;
        this.request = request;
    }
    @Override
    public void process(@NonNull Result<Response,NetworkException> result) {
        if (result.isSuccess() || !(result.getException() instanceof ConnectionException) ||
            !needToRetry((ConnectionException) result.getException())) {
            safeProcessCallback(callback, result);
            return;
        }

        retryRequest(0, result);
    }

    private void retryRequest(final int attempt, final Result<Response, NetworkException> lastResult) {
        try {
            if (attempt >= RETRY_DELAYS_SECONDS.length) {
                safeProcessCallback(callback, lastResult);
                return;
            }

            long delay = RETRY_DELAYS_SECONDS[attempt];
            PWLog.debug("Scheduling retry attempt " + (attempt + 1) + " with a delay of " + delay + " seconds");

            executor.schedule(() -> {
                RequestManager requestManager = NetworkModule.getRequestManager();
                if (requestManager == null) {
                    NetworkException exception = new NetworkException("Failed to retry request " +
                            request.getMethod() + ": RequestManager is null");
                    safeProcessCallback(callback, Result.fromException(exception));
                    return;
                }

                Result<Response, NetworkException> result = requestManager.sendRequestSync(request);

                if (result.isSuccess()) {
                    safeProcessCallback(callback, result);
                } else {
                    retryRequest(attempt + 1, result);
                }
            }, delay, TimeUnit.SECONDS);
        } catch (Exception e) {
            PWLog.error("Failed to retry request " + request.getMethod() + ": " + e.getMessage());
            callback.process(Result.fromException( new NetworkException(e.getMessage())));
        }
    }


    boolean needToRetry(ConnectionException exception) {
        int pushwooshStatus = exception.getPushwooshStatusCode();
        int networkStatus = exception.getStatusCode();
        // statuses are 0 by default and changed after processing request. If they are both still 0
        // then request failed due to connection errors
        boolean hasRequestFailed = pushwooshStatus == 0 && networkStatus == 0;

        return hasRequestFailed || isRetriableStatus(networkStatus);
    }

    private static boolean isRetriableStatus(int code) {
        switch (code) {
            case 408: // Request Timeout
            case 429: // Too Many Requests
            case 500: // Internal Server Error
            case 502: // Bad Gateway
            case 503: // Service Unavailable
            case 504: // Gateway Timeout
                return true;
            default:
                return false;
        }
    }

    private void safeProcessCallback(
            Callback<Response, NetworkException> callback, Result<Response, NetworkException> result) {
        if (callback != null) {
            callback.process(result);
        }
    }
}
