//
// RequestManager.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.pushwoosh.internal.network;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RegistrationPrefs;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

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
    private volatile String baseRequestUrl;
    private volatile String reverseProxyUrl;
    private volatile Map<String, String> customHeaders = new HashMap<>();
    private final boolean reverseProxyRequired;

    PushwooshRequestManager(
            RegistrationPrefs registrationPrefs, ServerCommunicationManager serverCommunicationManager,
            boolean reverseProxyRequired) {
        this.registrationPrefs = registrationPrefs;
        this.serverCommunicationManager = serverCommunicationManager;
        this.reverseProxyRequired = reverseProxyRequired;

        baseRequestUrl = registrationPrefs.baseUrl().get();
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
            PWLog.error(TAG, "Error processing callback: " + e.getMessage());
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
    public void updateBaseUrl(final String baseUrl) {
        saveBaseUrl(baseUrl);
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
            NetworkResult result = makeRequest(baseUrl, data, request.getMethod());

            statusCode = result.getStatus();
            pushwooshStatusCode = result.getPushwooshStatus();
            if (NetworkResult.STATUS_OK == statusCode && NetworkResult.STATUS_OK == pushwooshStatusCode) {

                JSONObject response = result.getResponse();
                // honor base url change
                if (response.has("base_url") && baseUrl.equals(baseRequestUrl) && reverseProxyUrl == null) {
                    String newBaseUrl = response.optString("base_url");
                    saveBaseUrl(newBaseUrl);
                }

                JSONObject responseData = response.optJSONObject("response");
                if (responseData == null) {
                    responseData = new JSONObject();
                }

                return Result.fromData(request.parseResponse(responseData));
            } else {
                exception = new NetworkException(result.getResponse().toString());
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

    private void saveBaseUrl(String url) {
        PWLog.info(TAG, String.format("Set base url: %s", url));
        baseRequestUrl = url;
        registrationPrefs.baseUrl().set(url);
    }

    private String getApiToken() {
        return "Token " + registrationPrefs.apiToken().get();
    }

    private NetworkResult makeRequest(final String baseUrl, JSONObject data, String methodName) throws Exception {
        try {
            String effectiveUrl = reverseProxyUrl != null ? reverseProxyUrl : baseUrl;
            URL url = new URL(effectiveUrl + methodName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            if (reverseProxyUrl != null) {
                for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", getApiToken());
            connection.setDoOutput(true);

            JSONObject requestJson = new JSONObject();
            requestJson.put("request", data);

            connection.setRequestProperty(
                    "Content-Length", String.valueOf(requestJson.toString().getBytes().length));
            connection.setUseCaches(false);
            try (OutputStream connectionOutput = connection.getOutputStream()) {
                connectionOutput.write(requestJson.toString().getBytes());
                connectionOutput.flush();
            }

            NetworkResult networkResult = getNetworkResultFromConnection(connection);
            PWLog.debug(
                    TAG,
                    "\n"
                            + "| Pushwoosh request:\n"
                            + "| - URL: " + url.toString() + "\n"
                            + "| - Payload: " + requestJson.toString() + "\n"
                            + "| - Response: " + networkResult.getResponse().toString() + "\n");

            return networkResult;
        } catch (MalformedURLException e) {
            // Reset base URL only for malformed URL (invalid format like "httz://...")
            if (baseUrl.equals(baseRequestUrl)) {
                PWLog.warn(TAG, "Malformed URL detected, resetting to default: " + e.getMessage());
                baseRequestUrl = registrationPrefs.getDefaultBaseUrl();
            }
            throw e;
        } catch (Exception e) {
            PWLog.error(TAG, "Request failed: " + e.getMessage(), e);
            throw e;
        }
    }

    private NetworkResult getNetworkResultFromConnection(HttpURLConnection connection) throws IOException {
        InputStream inputStream;
        JSONObject responseJson = new JSONObject();
        int status = connection.getResponseCode();
        int pushwooshStatus = 0;
        if (isErrorResponseCode(connection.getResponseCode())) {
            inputStream = new BufferedInputStream(connection.getErrorStream());
            pushwooshStatus = status;
            try {
                responseJson.put("status_code", pushwooshStatus);
                responseJson.put("status_message", connection.getResponseMessage());
            } catch (JSONException e) {
                PWLog.error(TAG, e.getMessage());
            }
        } else {
            inputStream = new BufferedInputStream(connection.getInputStream());
        }

        try {
            if (connection.getContentLength() != 0) {
                try (ByteArrayOutputStream dataCache = new ByteArrayOutputStream()) {

                    // Fully read data
                    byte[] buff = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buff)) >= 0) {
                        dataCache.write(buff, 0, len);
                    }

                    responseJson = new JSONObject(dataCache.toString().trim());
                    pushwooshStatus = responseJson.getInt("status_code");
                } catch (Exception e) {
                    PWLog.error(TAG, e.getMessage());
                }
            }
        } finally {
            inputStream.close();
        }
        return new NetworkResult(status, pushwooshStatus, responseJson);
    }

    private boolean isErrorResponseCode(int code) {
        return code >= 400 && code < 600;
    }

    static class NetworkResult {
        static final int STATUS_OK = 200;
        static final int STATUS_NOT_FOUND = 404;

        private final int pushwooshStatus;
        private final int status;
        private final JSONObject response;

        NetworkResult(int networkCode, int pushwooshCode, JSONObject data) {
            status = networkCode;
            pushwooshStatus = pushwooshCode;
            response = data;
        }

        int getStatus() {
            return status;
        }

        int getPushwooshStatus() {
            return pushwooshStatus;
        }

        JSONObject getResponse() {
            return response;
        }
    }
}
