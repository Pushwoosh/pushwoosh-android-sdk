package com.pushwoosh.featureflags;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

final class FeatureFlagsResponse {
    @NonNull final JSONObject flagsJson;

    @NonNull final Map<String, FeatureFlag> flags;

    @NonNull final String etag;

    final boolean notModified;
    final boolean partial;

    private FeatureFlagsResponse(
            @NonNull JSONObject flagsJson,
            @NonNull Map<String, FeatureFlag> flags,
            @NonNull String etag,
            boolean notModified,
            boolean partial) {
        this.flagsJson = flagsJson;
        this.flags = flags;
        this.etag = etag;
        this.notModified = notModified;
        this.partial = partial;
    }

    @NonNull static FeatureFlagsResponse from(@NonNull JSONObject response) throws JSONException {
        boolean notModified = response.optBoolean("not_modified", false);
        JSONObject flagsJson = response.optJSONObject("flags");
        if (flagsJson == null) {
            if (!notModified) {
                throw new JSONException("getFeatureFlags response has no flags");
            }
            flagsJson = new JSONObject();
        }
        Object etag = response.opt("etag");
        return new FeatureFlagsResponse(
                flagsJson,
                FeatureFlagsParser.parse(flagsJson),
                etag instanceof String ? (String) etag : "",
                notModified,
                response.optBoolean("partial", false));
    }
}
