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

package com.pushwoosh.inapp.network.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import androidx.annotation.NonNull;

import com.pushwoosh.inapp.exception.ResourceParseException;
import com.pushwoosh.internal.utils.JsonUtils;

import org.json.JSONException;
import org.json.JSONObject;


import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_GDPR;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_HASH;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_PRIORITY;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_REQUIRED;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_TAGS;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_TIME_STAMP;
import static com.pushwoosh.inapp.network.model.Resource.Column.KEY_URL;

class ResourceParseUtils {

	@NonNull
	static Resource parseRichMedia(String richMedia) throws ResourceParseException {
		try {
			JSONObject richMediaJson = new JSONObject(richMedia);

			String hash = richMediaJson.optString(KEY_HASH);
			boolean required = richMediaJson.optBoolean(KEY_REQUIRED, false);
			int priority = richMediaJson.optInt(KEY_PRIORITY, 0);
			long ts = richMediaJson.getLong(KEY_TIME_STAMP);
			String url = richMediaJson.getString(KEY_URL);
			String gdpr = richMediaJson.optString(KEY_GDPR);

			Uri uri = Uri.parse(url);
			String code = uri.getLastPathSegment();
			code = parseRichMediaCode(url, code);

			Map<String, Object> tags = parseTags(richMediaJson);

			return new Resource(code, url, hash, ts, InAppLayout.TOP, tags, required, priority,null, gdpr);
		} catch (Exception e) {
			throw new ResourceParseException("Can't parse richMedia", e);
		}
	}

	@NonNull
	private static Map<String, Object> parseTags(JSONObject richMediaJson) throws JSONException {
		JSONObject tagsJson = new JSONObject("{}");
		if (richMediaJson.has(KEY_TAGS)) {
			tagsJson = richMediaJson.getJSONObject(KEY_TAGS);
		}

		return JsonUtils.jsonToMap(tagsJson);
	}

	@NonNull
	private static String parseRichMediaCode(String url, String code) {
		if (code == null) {
			throw new IllegalArgumentException("Missing code in richMedia url: " + url);
		}

		if (code.contains(".")) {
			code = code.substring(0, code.lastIndexOf("."));
		}

		code = "r-" + code; // avoid rich media and inapp conflicts
		return code;
	}

	static Map<String, String> convertTags(Map<String, Object> tags) {
		if (tags == null) {
			return Collections.emptyMap();
		}

		HashMap<String, String> result = new HashMap<>();
		for (Map.Entry<String, Object> entry : tags.entrySet()) {
			if (entry.getValue() != null) {
				result.put(entry.getKey(), entry.getValue().toString());
			}
		}

		return result;
	}

}
