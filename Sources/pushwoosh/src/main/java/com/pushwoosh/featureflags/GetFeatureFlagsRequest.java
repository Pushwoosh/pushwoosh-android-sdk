package com.pushwoosh.featureflags;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.network.PushRequest;

import org.json.JSONException;
import org.json.JSONObject;

final class GetFeatureFlagsRequest extends PushRequest<FeatureFlagsResponse> {
    static final String METHOD = "getFeatureFlags";
    private static final String API_PATH = "api/v2/device-api/";

    private final String application;
    private final String hwid;
    private final String etag;

    GetFeatureFlagsRequest(@NonNull String application, @NonNull String hwid, @NonNull String etag) {
        this.application = application;
        this.hwid = hwid;
        this.etag = etag;
    }

    @Override
    public String getMethod() {
        return METHOD;
    }

    @Override
    public boolean shouldWrapRequest() {
        return false;
    }

    @NonNull @Override
    public String getApiPath() {
        return API_PATH;
    }

    @NonNull @Override
    public String getApplicationCode() {
        return application;
    }

    @Override
    protected JSONObject getParams() throws JSONException {
        // /api/v2 is strict proto-JSON: the base params v, device_type, userId are rejected.
        JSONObject params = new JSONObject();
        params.put("application", application);
        params.put("hwid", hwid);
        if (!etag.isEmpty()) {
            params.put("etag", etag);
        }
        return params;
    }

    @NonNull @Override
    public FeatureFlagsResponse parseResponse(@NonNull JSONObject response) throws JSONException {
        return FeatureFlagsResponse.from(response);
    }
}
