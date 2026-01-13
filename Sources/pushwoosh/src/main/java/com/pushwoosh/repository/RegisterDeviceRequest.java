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

import com.pushwoosh.internal.platform.utils.GeneralUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RegisterDeviceRequest extends AppOpenRequest {
	private final String deviceId;
	private final String tagsJson;
	private final int platform;
	private final String appCode;

	RegisterDeviceRequest(String deviceId, String tagsJson, int platform) {
		this.deviceId = deviceId;
		this.tagsJson = tagsJson;
		this.platform = platform;
		this.appCode = RepositoryModule.getRegistrationPreferences().applicationId().get();
	}

	@Override
	public String getMethod() {
		return "registerDevice";
	}

	@Override
	protected void buildParams(JSONObject params) throws JSONException {
		super.buildParams(params);

		params.put("device_type", platform);
		params.put("application", appCode);
		if (platform == DeviceRegistrar.PLATFORM_SMS) {
			params.put("hwid", deviceId);
		} else if (platform == DeviceRegistrar.PLATFORM_WHATSAPP) {
			params.put("hwid", "whatsapp:"+deviceId);
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
