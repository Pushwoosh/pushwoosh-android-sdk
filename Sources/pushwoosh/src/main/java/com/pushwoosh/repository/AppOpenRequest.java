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
import com.pushwoosh.inapp.businesscases.BusinessCasesManager;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.platform.utils.GeneralUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

class AppOpenRequest extends PushRequest<Void> {
    public String getMethod() {
        return "applicationOpen";
    }
    public boolean shouldUseJitter(){ return true; }


    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        params.put("language", RepositoryModule.getRegistrationPreferences().language().get());
        params.put("timezone", TimeUnit.SECONDS.convert(TimeZone.getDefault().getOffset(new Date().getTime()), TimeUnit.MILLISECONDS));

        String packageName = AndroidPlatformModule.getAppInfoProvider().getPackageName();
        params.put("android_package", packageName);

        if (RepositoryModule.getNotificationPreferences().isCollectingDeviceModelAllowed().get()) {
            params.put("device_model", DeviceUtils.getDeviceName());
            params.put("device_name", DeviceUtils.isTablet() ? "Tablet" : "Phone");
        }
        if (RepositoryModule.getNotificationPreferences().isCollectingDeviceOsVersionAllowed().get()) {
            String androidVersion = android.os.Build.VERSION.RELEASE;
            params.put("os_version", androidVersion);
        }

        String versionName = AndroidPlatformModule.getAppInfoProvider().getVersionName();
        if (versionName != null) {
            params.put("app_version", versionName);
        }

        PermissionController permissionController = AndroidPlatformModule.getInstance().getPermissionController();
        if (permissionController != null) {
            params.put("notificationTypes", permissionController.getBitMaskPermission());
        }
    }

    @Nullable
    @Override
    public Void parseResponse(@NonNull JSONObject response) throws JSONException {
        BusinessCasesManager.processBusinessCasesData(response.optJSONObject("required_inapps"));
        return super.parseResponse(response);
    }
}
