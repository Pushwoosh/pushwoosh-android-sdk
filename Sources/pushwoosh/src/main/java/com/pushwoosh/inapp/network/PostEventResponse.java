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

package com.pushwoosh.inapp.network;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.pushwoosh.inapp.InAppModule;
import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.inapp.storage.InAppStorage;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

public class PostEventResponse {
	private final String code;
	private final String richMediaJson;
	private final Resource resource;
	private final boolean isRequired;

	@WorkerThread
	PostEventResponse(JSONObject response) throws JSONException {
		code = response.optString("code");
		richMediaJson = response.optString("richmedia");
		isRequired = response.optBoolean("required", false);
		if (!code.isEmpty()) {
			InAppStorage inAppStorage = InAppModule.getInAppStorage();
			if(inAppStorage == null){
				resource = null;
			} else {
				resource = inAppStorage.getResource(code);
			}
		} else if (!richMediaJson.isEmpty()) {
			if (InAppModule.getInAppRepository() != null) {
				InAppModule.getInAppRepository().prefetchRichMedia(richMediaJson);
				resource = tryParseRichMedia(richMediaJson);
			} else {
				resource = null;
			}
		} else {
			resource = null;
		}
	}

	@Nullable
	public Resource getResource() {
		return resource;
	}

	public String getCode() {
		return code;
	}

	public boolean isRequired() {
		return isRequired;
	}

	private Resource tryParseRichMedia(String richMediaJson) {
		try {
			return Resource.parseRichMedia(richMediaJson);
		} catch (ResourceParseException e) {
			PWLog.error("Failed to parse rich media json", e);
			return null;
		}
	}
}
