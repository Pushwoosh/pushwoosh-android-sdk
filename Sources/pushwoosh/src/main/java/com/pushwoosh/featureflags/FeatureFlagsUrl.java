package com.pushwoosh.featureflags;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

final class FeatureFlagsUrl {
    private static final String TAG = "FeatureFlagsUrl";
    private static final String LEGACY_PATH = "json/1.3/";
    private static final String DEVICE_API_PATH = "api/v2/device-api/";

    private FeatureFlagsUrl() {}

    @Nullable static String endpoint(
            @Nullable String reverseProxyUrl, @Nullable String baseUrl, @NonNull String applicationCode) {
        boolean viaProxy = !TextUtils.isEmpty(reverseProxyUrl);
        String base = viaProxy ? reverseProxyUrl : baseUrl;
        if (base != null && base.endsWith(LEGACY_PATH)) {
            return base.substring(0, base.length() - LEGACY_PATH.length()) + DEVICE_API_PATH;
        }
        if (viaProxy) {
            PWLog.warn(TAG, "Reverse proxy URL does not end with " + LEGACY_PATH + ", feature flags are not requested");
            return null;
        }
        String fallback = "https://" + applicationCode + ".api.pushwoosh.com/" + DEVICE_API_PATH;
        PWLog.warn(TAG, "Base URL does not end with " + LEGACY_PATH + ", feature flags use " + fallback);
        return fallback;
    }
}
