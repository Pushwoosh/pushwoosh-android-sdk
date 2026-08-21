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
 * Executes Pushwoosh backend requests over HTTP.
 * <p>
 * Wraps a {@link PushRequest} into a JSON envelope, dispatches it via {@link HttpTransport},
 * parses the response and returns a {@link Result} of typed data or {@link NetworkException}.
 * Gates outgoing traffic on: reverse proxy availability, base URL presence,
 * "remove all device data" state, and the {@link ServerCommunicationManager} switch.
 * Async calls run on the network executor; callbacks are delivered on the main thread.
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

    @Nullable private volatile ReverseProxy reverseProxy;

    private final boolean reverseProxyRequired;

    private final HttpTransport httpTransport;

    PushwooshRequestManager(
            RegistrationPrefs registrationPrefs,
            ServerCommunicationManager serverCommunicationManager,
            boolean reverseProxyRequired,
            @NonNull HttpTransport httpTransport) {
        this.registrationPrefs = registrationPrefs;
        this.serverCommunicationManager = serverCommunicationManager;
        this.reverseProxyRequired = reverseProxyRequired;
        this.httpTransport = httpTransport;
    }

    /**
     * Delivers a request outcome on the main thread inside the SDK's error barrier.
     * <p>
     * Shared with {@code FakeRequestManager} in tests on purpose: a hand-written copy of this hop
     * cannot notice a regression in it.
     */
    static <R> void deliverOnMain(
            @Nullable Callback<R, NetworkException> callback, @NonNull Result<R, NetworkException> result) {
        if (callback == null) {
            return;
        }
        BackgroundExecutor.main(() -> {
            try {
                callback.process(result);
            } catch (Exception e) {
                PWLog.error(TAG, "Error processing callback", e);
            }
        });
    }

    @Override
    public <Response> void sendRequest(final PushRequest<Response> request) {
        sendRequest(request, null);
    }

    public <Response> void sendRequest(
            PushRequest<Response> request, @Nullable Callback<Response, NetworkException> callback) {
        sendRequest(request, null, callback);
    }

    /**
     * Sends the request asynchronously on the network executor.
     * <p>
     * The callback (when provided) is delivered on the main thread.
     *
     * @param baseUrl optional override; if {@code null}, the currently registered base URL is used
     */
    @Override
    public <Response> void sendRequest(
            final PushRequest<Response> request,
            final String baseUrl,
            final Callback<Response, NetworkException> callback) {
        BackgroundExecutor.network(() -> {
            Result<Response, NetworkException> result = sendRequestSync(request, baseUrl);
            deliverOnMain(callback, result);
        });
    }

    /**
     * Synchronous variant of {@link #sendRequest}; must be called from a background thread.
     */
    @NonNull public <Response> Result<Response, NetworkException> sendRequestSync(PushRequest<Response> request) {
        return sendRequestSync(request, baseRequestUrl);
    }

    /**
     * Persists and applies a new backend base URL.
     * <p>
     * The value is normalized by {@link RegistrationPrefs#updateBaseUrl} before being stored;
     * blank or invalid values are rejected.
     *
     * @return {@code true} if the URL was accepted and stored
     */
    @Override
    public boolean updateBaseUrl(final String baseUrl) {
        String normalized = registrationPrefs.updateBaseUrl(baseUrl);
        if (normalized != null) {
            baseRequestUrl = normalized;
            return true;
        }
        return false;
    }

    /**
     * Configures a reverse proxy URL and the custom headers attached to each request.
     * <p>
     * When set, all outgoing requests are routed through this URL, overriding any
     * caller-supplied base URL. The URL is normalized by {@link RegistrationPrefs#normalizeBaseUrl};
     * a rejected URL leaves both the proxy and the headers untouched. Pass {@code null} url to route
     * requests back to the base URL — the custom headers are dropped along with it, as they only
     * apply to the proxy endpoint; pass {@code null} headers to clear them. The URL and the headers
     * are published as one value, so a concurrent request never sees a mix of the two configurations.
     *
     * @return {@code true} if the settings were applied, {@code false} if the URL was rejected
     */
    @Override
    public boolean setReverseProxyUrl(String url, Map<String, String> headers) {
        if (url == null) {
            reverseProxy = null;
            return true;
        }
        String normalized = RegistrationPrefs.normalizeBaseUrl(url);
        if (normalized == null) {
            PWLog.error(TAG, "Reverse proxy URL rejected: " + url);
            return false;
        }
        reverseProxy = new ReverseProxy(normalized, headers);
        return true;
    }

    /**
     * Runs pre-flight gating and dispatches to {@link #executeRequest} on success.
     * <p>
     * Blocks the request when:
     * <ul>
     *   <li>reverse proxy is required but not configured</li>
     *   <li>no base URL is available</li>
     *   <li>device data has been wiped ({@code removeAllDeviceData})</li>
     *   <li>server communication is paused via {@link ServerCommunicationManager}</li>
     * </ul>
     */
    @NonNull private <Response> Result<Response, NetworkException> sendRequestSync(
            PushRequest<Response> request, String baseUrl) {
        if (baseUrl == null) {
            baseUrl = baseRequestUrl;
        }
        ReverseProxy proxy = reverseProxy;
        if (reverseProxyRequired && proxy == null) {
            PWLog.error(TAG, "Reverse proxy is required but not configured. Request blocked: " + request.getMethod());
            return Result.fromException(new NetworkException("Reverse proxy is required but not configured"));
        }
        Endpoint endpoint = resolveEndpoint(baseUrl, proxy);
        if (endpoint == null) {
            PWLog.error(TAG, "Base URL is not configured. Request blocked: " + request.getMethod());
            return Result.fromException(new NetworkException("Base URL is not configured"));
        }
        if (registrationPrefs.removeAllDeviceData().get()) {
            PWLog.warn(TAG, DEVICE_REMOVED_MSG + ". Request blocked: " + request.getMethod());
            return Result.fromException(new NetworkException(DEVICE_REMOVED_MSG));
        }
        if (serverCommunicationManager != null && !serverCommunicationManager.isServerCommunicationAllowed()) {
            PWLog.warn(TAG, COMMUNICATION_STOPPED_MSG + ". Request blocked: " + request.getMethod());
            return Result.fromException(new NetworkException(COMMUNICATION_STOPPED_MSG));
        }

        return executeRequest(request, endpoint);
    }

    /**
     * Performs the HTTP exchange and maps the response to a {@link Result}.
     * <p>
     * A success requires both transport status 200 and Pushwoosh envelope {@code status_code} 200;
     * anything else (including thrown exceptions) is wrapped into a {@link ConnectionException}.
     * On success, the server-provided {@code base_url} is applied via {@link #applyBaseUrlRotation}
     * when the endpoint is rotatable.
     */
    @NonNull private <Response> Result<Response, NetworkException> executeRequest(
            PushRequest<Response> request, Endpoint endpoint) {
        Exception exception;
        int statusCode = 0, pushwooshStatusCode = 0;
        try {
            HttpResponse httpResponse = httpTransport.makeRequest(
                    endpoint.url, buildPayload(request), request.getMethod(), endpoint.headers, getApiToken());

            statusCode = httpResponse.statusCode;

            ParsedEnvelope parsed = parseEnvelope(httpResponse);
            JSONObject envelope = parsed.envelope;
            pushwooshStatusCode = parsed.pushwooshStatusCode;

            if (200 == statusCode && 200 == pushwooshStatusCode) {
                applyBaseUrlRotation(envelope, endpoint);
                return Result.fromData(request.parseResponse(extractResponseBody(envelope)));
            }
            exception = new NetworkException(envelope.toString());
        } catch (Exception ex) {
            exception = ex;
        }
        PWLog.error(TAG, "Request failed: " + exception.getMessage(), exception);
        return Result.fromException(new ConnectionException(exception.getMessage(), statusCode, pushwooshStatusCode));
    }

    /**
     * Extracts the Pushwoosh envelope from an HTTP response.
     * <p>
     * If the body is missing or unparseable, a synthetic envelope is populated with the
     * transport-level status code and message instead.
     */
    @NonNull private static ParsedEnvelope parseEnvelope(HttpResponse httpResponse) {
        JSONObject envelope = new JSONObject();
        int pushwooshStatusCode = 0;
        if (httpResponse.isError()) {
            try {
                envelope.put("status_code", httpResponse.statusCode);
                envelope.put("status_message", httpResponse.statusMessage);
            } catch (JSONException e) {
                PWLog.error(TAG, e.getMessage());
            }
            pushwooshStatusCode = httpResponse.statusCode;
        }
        if (!httpResponse.body.isEmpty()) {
            try {
                envelope = new JSONObject(httpResponse.body);
                pushwooshStatusCode = envelope.getInt("status_code");
            } catch (Exception e) {
                PWLog.error(TAG, "Failed to parse response envelope", e);
            }
        }
        return new ParsedEnvelope(envelope, pushwooshStatusCode);
    }

    /**
     * Applies the {@code base_url} from the envelope when the endpoint is marked rotatable.
     * <p>
     * Follows server-side load-balancing hints. Only the canonical registered URL is rotatable —
     * reverse proxy and caller-supplied URLs are pinned.
     */
    private void applyBaseUrlRotation(JSONObject envelope, Endpoint endpoint) {
        if (envelope.has("base_url") && endpoint.rotatable) {
            updateBaseUrl(envelope.optString("base_url"));
        }
    }

    /**
     * Builds the JSON body to send.
     * <p>
     * When {@link PushRequest#shouldWrapRequest()} is {@code true} the params are wrapped
     * as {@code {"request": ...}}; otherwise the raw params are returned as-is.
     */
    @NonNull private static JSONObject buildPayload(PushRequest<?> request) throws JSONException, InterruptedException {
        JSONObject data = request.getParams();
        return request.shouldWrapRequest() ? new JSONObject().put("request", data) : data;
    }

    @NonNull private static JSONObject extractResponseBody(JSONObject envelope) {
        JSONObject responseData = envelope.optJSONObject("response");
        return responseData != null ? responseData : new JSONObject();
    }

    private String getApiToken() {
        return "Token " + registrationPrefs.apiToken().get();
    }

    /**
     * Picks the effective endpoint for the request.
     * <p>
     * Reverse proxy takes precedence; otherwise the caller-supplied base URL is used.
     * Returns {@code null} when nothing is configured. The endpoint is marked rotatable
     * only when its URL matches the currently registered {@code baseRequestUrl}.
     *
     * @param proxy the reverse proxy snapshot taken by the caller, {@code null} when not configured
     */
    @Nullable private Endpoint resolveEndpoint(@Nullable String callerBaseUrl, @Nullable ReverseProxy proxy) {
        if (proxy != null) {
            return new Endpoint(proxy.url, proxy.headers, false);
        }
        if (TextUtils.isEmpty(callerBaseUrl)) {
            return null;
        }
        boolean rotatable = Objects.equals(callerBaseUrl, baseRequestUrl);
        return new Endpoint(callerBaseUrl, Collections.emptyMap(), rotatable);
    }

    private static final class ReverseProxy {
        final String url;
        final Map<String, String> headers;

        ReverseProxy(String url, @Nullable Map<String, String> headers) {
            this.url = url;
            this.headers =
                    headers != null ? Collections.unmodifiableMap(new HashMap<>(headers)) : Collections.emptyMap();
        }
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

    private static final class ParsedEnvelope {
        final JSONObject envelope;
        final int pushwooshStatusCode;

        ParsedEnvelope(JSONObject envelope, int pushwooshStatusCode) {
            this.envelope = envelope;
            this.pushwooshStatusCode = pushwooshStatusCode;
        }
    }
}
