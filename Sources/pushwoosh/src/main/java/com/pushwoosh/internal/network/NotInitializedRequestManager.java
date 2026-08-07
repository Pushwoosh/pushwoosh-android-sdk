package com.pushwoosh.internal.network;

import androidx.annotation.NonNull;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.utils.PWLog;

import java.util.Map;

/**
 * Null Object handed out by {@link NetworkModule#getRequestManager()} before {@link NetworkModule#init}
 * has run — a broken integration (no FCM/HMS module, so the platform is never built) or a test that
 * reset the manager.
 * <p>
 * Every request is dropped with a terminal error: without a real manager there is no endpoint to reach
 * and no amount of retrying changes that. The error is deliberately a bare {@link NetworkException} and
 * never a {@link ConnectionException}, so the retry layers ({@code RetriableRequestCallback},
 * {@code PushStatisticsWorker}) treat it as final — the terminality the removed null-guards had.
 * <p>
 * Callbacks run synchronously on the calling thread, matching the removed guards, which handled the
 * error inline. This differs from {@link PushwooshRequestManager}, which hops to the main thread: a
 * caller that relies on that hop must not rely on it here. A throwing callback is caught and logged,
 * as it is there.
 */
class NotInitializedRequestManager implements RequestManager {
    private static final String TAG = "NotInitializedRequestManager";
    private static final String NOT_INITIALIZED_MSG = "SDK is not initialized";

    @Override
    public <Response> void sendRequest(PushRequest<Response> request) {
        sendRequest(request, null, null);
    }

    @Override
    public <Response> void sendRequest(PushRequest<Response> request, Callback<Response, NetworkException> callback) {
        sendRequest(request, null, callback);
    }

    @Override
    public <Response> void sendRequest(
            PushRequest<Response> request, String baseUrl, Callback<Response, NetworkException> callback) {
        Result<Response, NetworkException> result = sendRequestSync(request);
        if (callback == null) {
            return;
        }
        try {
            callback.process(result);
        } catch (Exception e) {
            PWLog.error(TAG, "Error processing callback", e);
        }
    }

    @Override
    @NonNull public <Response> Result<Response, NetworkException> sendRequestSync(PushRequest<Response> request) {
        PWLog.error(TAG, NOT_INITIALIZED_MSG + ", request dropped: " + request.getMethod());
        return Result.fromException(new NetworkException(NOT_INITIALIZED_MSG));
    }

    @Override
    public boolean updateBaseUrl(String baseUrl) {
        PWLog.warn(TAG, NOT_INITIALIZED_MSG + ", base url not updated");
        return false;
    }

    @Override
    public boolean setReverseProxyUrl(String url, Map<String, String> headers) {
        PWLog.warn(TAG, NOT_INITIALIZED_MSG + ", reverse proxy not set");
        return false;
    }
}
