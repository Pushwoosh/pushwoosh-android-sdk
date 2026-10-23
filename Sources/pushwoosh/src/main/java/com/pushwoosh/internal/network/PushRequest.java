package com.pushwoosh.internal.network;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.repository.RepositoryModule;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class PushRequest<S> {

    public abstract String getMethod();

    public boolean shouldWrapRequest() {
        return true;
    }

    /**
     * API path this request is served under, so the request manager can resolve the full address from one
     * reverse-proxy snapshot at send time.
     *
     * @return path relative to the host root, with no leading slash and a trailing slash, applied to a root that
     * ends with {@code json/1.3/}; {@code null} means the base URL is used as configured. An address built from a
     * path is not rotatable: a {@code base_url} in the response is not applied to it.
     */
    @Nullable public String getApiPath() {
        return null;
    }

    /**
     * Application code this request is addressed to, read from the same field that goes into the body.
     * <p>
     * Used to build the canonical app-code host when the configured root cannot carry an
     * {@link #getApiPath() API path}, so the address and the body can never name two different
     * applications even if the registered application code changes while the request is queued.
     *
     * @return the application code carried by this request, or {@code null} when it carries none —
     * a request that declares an API path and no application code cannot be rerouted and is blocked
     */
    @Nullable public String getApplicationCode() {
        return null;
    }

    protected JSONObject getParams() throws JSONException, InterruptedException {
        final JSONObject baseParams = new JSONObject();

        baseParams.put("application", getApplicationId());
        String hwid = getHwid();
        baseParams.put("hwid", hwid);
        baseParams.put("v", GeneralUtils.SDK_VERSION);

        // check for Amazon (Kindle) or Google device
        baseParams.put("device_type", DeviceSpecificProvider.getInstance().deviceType());

        String currentUserId = getUserId();
        if (!TextUtils.isEmpty(currentUserId)) {
            baseParams.put("userId", currentUserId);
        }

        buildParams(baseParams);

        return baseParams;
    }

    @NonNull protected String getHwid() throws InterruptedException {
        return PushwooshPlatform.getInstance().pushwooshRepository().getHwid();
    }

    protected String getUserId() {
        return RepositoryModule.getRegistrationPreferences().userId().get();
    }

    protected String getApplicationId() {
        return RepositoryModule.getRegistrationPreferences().applicationId().get();
    }

    /**
     * Add params to request
     *
     * @param params - current params
     */
    protected void buildParams(JSONObject params) throws JSONException {
        // subclass
    }

    /**
     * Method for parsing response which returning from service for current request
     *
     * @param response - JSONObject response
     * @return Response model which connected with this request.
     */
    @Nullable public S parseResponse(@NonNull JSONObject response) throws JSONException {
        return null;
    }
}
