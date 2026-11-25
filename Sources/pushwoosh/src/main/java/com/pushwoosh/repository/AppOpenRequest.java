/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.PermissionController;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.notification.PushPermissionStatusTracker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

class AppOpenRequest extends PushRequest<Void> {
    private static final Pattern CUSTOM_GMT_OFFSET_ID = Pattern.compile("^GMT[+-]\\d{2}:\\d{2}$");

    @Nullable private String sentPushPermissionStatus;

    public String getMethod() {
        return "applicationOpen";
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        String language = getLanguage();
        if (language != null) {
            params.put("language", language);
        }
        String locale = getLocale();
        if (locale != null) {
            params.put("locale", locale);
        }
        params.put("timezone", timezoneParam());

        String packageName = AndroidPlatformModule.getAppInfoProvider().getPackageName();
        params.put("android_package", packageName);

        if (RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceModelAllowed()
                .get()) {
            params.put("device_model", DeviceUtils.getDeviceName());
            params.put("device_name", DeviceUtils.isTablet() ? "Tablet" : "Phone");
        }
        if (RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceOsVersionAllowed()
                .get()) {
            String androidVersion = android.os.Build.VERSION.RELEASE;
            params.put("os_version", androidVersion);
        }

        String versionName = AndroidPlatformModule.getAppInfoProvider().getVersionName();
        if (versionName != null) {
            params.put("app_version", versionName);
        }

        PermissionController permissionController =
                AndroidPlatformModule.getInstance().getPermissionController();
        if (permissionController != null) {
            params.put("notificationTypes", permissionController.getBitMaskPermission());
        }

        sentPushPermissionStatus = reportsPushPermissionStatus() ? PushPermissionStatusTracker.getStatus() : null;
        if (sentPushPermissionStatus != null) {
            params.put("push_permission_status", sentPushPermissionStatus);
        }
    }

    /** Whether {@code language} and {@code locale} may be collected and sent at the moment of the call. */
    static boolean isLocaleCollectionAllowed() {
        return RepositoryModule.getNotificationPreferences()
                .isCollectingDeviceLocaleAllowed()
                .get();
    }

    /**
     * Value sent as {@code language}.
     *
     * @return the language to send, or {@code null} to omit the key
     */
    @Nullable protected String getLanguage() {
        if (!isLocaleCollectionAllowed()) {
            return null;
        }
        return RepositoryModule.getRegistrationPreferences().language().get();
    }

    /**
     * Value sent as {@code locale}.
     *
     * @return the locale to send, or {@code null} to omit the key
     */
    @Nullable protected String getLocale() {
        if (!isLocaleCollectionAllowed()) {
            return null;
        }
        return RepositoryModule.getRegistrationPreferences().localeForRequest();
    }

    private static Object timezoneParam() {
        TimeZone zone = TimeZone.getDefault();
        if (CUSTOM_GMT_OFFSET_ID.matcher(zone.getID()).matches()) {
            // Server drops custom GMT+-hh:mm ids and stores 0, so send the offset instead.
            return TimeUnit.SECONDS.convert(zone.getOffset(System.currentTimeMillis()), TimeUnit.MILLISECONDS);
        }
        return zone.getID();
    }

    protected boolean reportsPushPermissionStatus() {
        return true;
    }

    /**
     * The push permission status this request put on the wire, or null if it carried none.
     */
    @Nullable String getSentPushPermissionStatus() {
        return sentPushPermissionStatus;
    }

    @Nullable @Override
    public Void parseResponse(@NonNull JSONObject response) throws JSONException {
        return super.parseResponse(response);
    }
}
