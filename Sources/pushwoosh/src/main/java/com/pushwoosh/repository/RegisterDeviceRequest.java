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

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.platform.utils.GeneralUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RegisterDeviceRequest extends AppOpenRequest {
    private final String deviceId;
    private final String tagsJson;
    private final int platform;

    @Nullable private final RegistrationSnapshot snapshot;

    /**
     * SMS / WhatsApp registration path. The fields not passed here are read live when the request is serialized.
     */
    public RegisterDeviceRequest(String deviceId, String tagsJson, int platform) {
        this.deviceId = deviceId;
        this.tagsJson = tagsJson;
        this.platform = platform;
        this.snapshot = null;
    }

    /**
     * Push registration that sends the values captured in {@code snapshot}, whatever the registration state is when
     * the request is serialized.
     */
    RegisterDeviceRequest(@NonNull RegistrationSnapshot snapshot, @Nullable String tagsJson) {
        this.deviceId = snapshot.pushToken();
        this.tagsJson = tagsJson;
        this.platform = snapshot.deviceType();
        this.snapshot = snapshot;
    }

    @Override
    public String getMethod() {
        return "registerDevice";
    }

    @Override
    protected String getApplicationId() {
        return snapshot != null ? snapshot.applicationCode() : super.getApplicationId();
    }

    @NonNull @Override
    protected String getHwid() throws InterruptedException {
        return snapshot != null ? snapshot.hwid() : super.getHwid();
    }

    @Override
    protected int getDeviceType() {
        return platform;
    }

    @Nullable @Override
    protected String getLanguage() {
        return snapshot != null ? snapshot.language() : super.getLanguage();
    }

    @Nullable @Override
    protected String getLocale() {
        return snapshot != null ? snapshot.locale() : super.getLocale();
    }

    @Override
    protected boolean reportsPushPermissionStatus() {
        return platform != DeviceRegistrar.PLATFORM_SMS && platform != DeviceRegistrar.PLATFORM_WHATSAPP;
    }

    @Override
    protected void buildParams(JSONObject params) throws JSONException {
        super.buildParams(params);

        if (platform == DeviceRegistrar.PLATFORM_SMS) {
            params.put("hwid", deviceId);
        } else if (platform == DeviceRegistrar.PLATFORM_WHATSAPP) {
            params.put("hwid", "whatsapp:" + deviceId);
        }
        params.put("push_token", deviceId);
        if (!TextUtils.isEmpty(tagsJson)) {
            JSONObject tagsObject = new JSONObject(tagsJson);
            params.put("tags", tagsObject);
        }

        ArrayList<String> rawResources = GeneralUtils.getRawResourses();
        if (rawResources != null) {
            JSONArray array = new JSONArray(rawResources);
            params.put("sounds", array);
        }
    }
}
