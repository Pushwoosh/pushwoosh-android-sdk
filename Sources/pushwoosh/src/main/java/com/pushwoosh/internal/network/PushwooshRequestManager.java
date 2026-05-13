//
// RequestManager.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.pushwoosh.internal.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link com.pushwoosh.internal.network.RequestManager}
 */
class PushwooshRequestManager implements RequestManager {
    private static final String TAG = "RequestManager";
    private static final String DEVICE_REMOVED_MSG =
            "Device data was removed from Pushwoosh and all interactions were stopped";
    private static final String COMMUNICATION_STOPPED_MSG =
            "Server communication stopped. Call Pushwoosh.startServerCommunication() to resume";

    private final RegistrationPrefs registrationPrefs;
    private final ServerCommunicationManager serverCommunicationManager;

    @Nullable private volatile String baseRequestUrl;

    @Nullable private volatile String reverseProxyUrl;

    private volatile Map<String, String> customHeaders = new HashMap<>();
    private final boolean reverseProxyRequired;

    private final HttpTransport httpTransport = new HttpTransport();

    PushwooshRequestManager(
            RegistrationPrefs registrationPrefs,
            ServerCommunicationManager serverCommunicationManager,
            boolean reverseProxyRequired) {
        this.registrationPrefs = registrationPrefs;
        this.serverCommunicationManager = serverCommunicationManager;
        this.reverseProxyRequired = reverseProxyRequired;
    }

    private boolean isRemoveAllDataDevice() {
        boolean removeAllDeviceData = registrationPrefs.removeAllDeviceData().get();

        if (removeAllDeviceData) {
            PWLog.noise(TAG, "remove all data device is true, it is block request to server");
        }
        return removeAllDeviceData;
    }

    private <Response> void safeProcessCallback(
            Callback<Response, NetworkException> callback, Result<Response, NetworkException> result) {
        if (callback == null) return;
        try {
            callback.process(result);
        } catch (Exception e) {
            PWLog.error(TAG, "Error processing callback", e);
        }
    }

    @Override
    public <Response> void sendRequest(final PushRequest<Response> request) {
        sendRequest(request, null);
    }

    public <Response> void sendRequest(
            PushRequest<Response> request, @Nullable Callback<Response, NetworkException> callback) {
        sendRequest(request, null, callback);
    }

    @Override
    public <Response> void sendRequest(
            final PushRequest<Response> request,
            final String baseUrl,
            final Callback<Response, NetworkException> callback) {
        BackgroundExecutor.network(() -> {
            Result<Response, NetworkException> result = sendRequestSync(request, baseUrl);
            if (callback != null) {
                BackgroundExecutor.main(() -> safeProcessCallback(callback, result));
            }
        });
    }

    @NonNull public <Response> Result<Response, NetworkException> sendRequestSync(PushRequest<Response> request) {
        return sendRequestSync(request, baseRequestUrl);
    }

    @Override
    public boolean updateBaseUrl(final String baseUrl) {
        String normalized = registrationPrefs.updateBaseUrl(baseUrl);
        if (normalized != null) {
            baseRequestUrl = normalized;
            return true;
        }
        return false;
    }

    @Override
    public void setReverseProxyUrl(String url, Map<String, String> headers) {
        reverseProxyUrl = url;
        customHeaders = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }

    @NonNull private <Response> Result<Response, NetworkException> sendRequestSync(
            PushRequest<Response> request, String baseUrl) {
        if (baseUrl == null) {
            baseUrl = baseRequestUrl;
        }
        if (reverseProxyRequired && reverseProxyUrl == null) {
            PWLog.error(TAG, "Reverse proxy is required but not configured. Request blocked.");
            return Result.fromException(new NetworkException("Reverse proxy is required but not configured"));
        }
        Endpoint endpoint = resolveEndpoint(baseUrl);
        if (endpoint == null) {
            PWLog.warn(TAG, "Base URL is not configured yet. Request blocked: " + request.getMethod());
            return Result.fromException(new NetworkException("Base URL is not configured"));
        }
        if (isRemoveAllDataDevice()) {
            return Result.fromException(new NetworkException(DEVICE_REMOVED_MSG));
        }
        if (serverCommunicationManager != null && !serverCommunicationManager.isServerCommunicationAllowed()) {
            return Result.fromException(new NetworkException(COMMUNICATION_STOPPED_MSG));
        }

        Exception exception;
        int statusCode = 0, pushwooshStatusCode = 0;
        try {
            JSONObject data = request.getParams();
            JSONObject payload = request.shouldWrapRequest() ? new JSONObject().put("request", data) : data;
            HttpResponse httpResponse = httpTransport.makeRequest(
                    endpoint.url, payload, request.getMethod(), endpoint.headers, getApiToken());

            statusCode = httpResponse.statusCode;
            JSONObject envelope = new JSONObject();
            if (isErrorResponseCode(statusCode)) {
                try {
                    envelope.put("status_code", statusCode);
                    envelope.put("status_message", httpResponse.statusMessage);
                } catch (JSONException e) {
                    PWLog.error(TAG, e.getMessage());
                }
                pushwooshStatusCode = statusCode;
            }
            if (!httpResponse.body.isEmpty()) {
                try {
                    envelope = new JSONObject(httpResponse.body);
                    pushwooshStatusCode = envelope.getInt("status_code");
                } catch (Exception e) {
                    PWLog.error(TAG, "Failed to parse response envelope", e);
                }
            }

            if (200 == statusCode && 200 == pushwooshStatusCode) {
                // honor base url change
                if (envelope.has("base_url") && endpoint.rotatable) {
                    String newBaseUrl = envelope.optString("base_url");
                    updateBaseUrl(newBaseUrl);
                }

                JSONObject responseData = envelope.optJSONObject("response");
                if (responseData == null) {
                    responseData = new JSONObject();
                }

                return Result.fromData(request.parseResponse(responseData));
            } else {
                exception = new NetworkException(envelope.toString());
            }
        } catch (Exception ex) {
            exception = ex;
        }
        PWLog.error(TAG, exception.getClass().getCanonicalName());
        if (exception instanceof ConnectionException) {
            PWLog.error(TAG, "ERROR: " + "connection error.");
        } else {
            PWLog.error(TAG, "ERROR: " + exception.getMessage(), exception);
        }

        return Result.fromException(new ConnectionException(exception.getMessage(), statusCode, pushwooshStatusCode));
    }

    private String getApiToken() {
        return "Token " + registrationPrefs.apiToken().get();
    }

    private static boolean isErrorResponseCode(int code) {
        return code >= 400 && code < 600;
    }

    @Nullable private Endpoint resolveEndpoint(@Nullable String callerBaseUrl) {
        String proxy = reverseProxyUrl;
        Map<String, String> headers = customHeaders;
        if (proxy != null) {
            return new Endpoint(proxy, headers, false);
        }
        if (TextUtils.isEmpty(callerBaseUrl)) {
            return null;
        }
        boolean rotatable = Objects.equals(callerBaseUrl, baseRequestUrl);
        return new Endpoint(callerBaseUrl, Collections.emptyMap(), rotatable);
    }

    private static final class Endpoint {
        final String url;
        final Map<String, String> headers;
        final boolean rotatable;

        Endpoint(String url, Map<String, String> headers, boolean rotatable) {
            this.url = url;
            this.headers = headers;
            this.rotatable = rotatable;
        }
    }
}
