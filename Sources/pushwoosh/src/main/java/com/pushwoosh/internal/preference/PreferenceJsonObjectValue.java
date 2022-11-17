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

package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PreferenceJsonObjectValue implements PreferenceValue {
	@Nullable
	private final SharedPreferences preferences;
	private final String key;

	@Nullable
	private JSONObject value;

	public PreferenceJsonObjectValue(@Nullable SharedPreferences preferences, String key) {
		this.key = key;
		this.preferences = preferences;
		try {
			String serialized = preferences == null ? null : preferences.getString(key, null);
			if (serialized != null) {
				this.value = new JSONObject(serialized);
			}
		} catch (Exception e) {
			PWLog.exception(e);
		}
	}

	@Nullable
	public JSONObject get() {
		return this.value;
	}

	public void set(JSONObject value) {
		if (preferences == null) {
			PWLog.error("Incorrect state of the app preferences is null");
			return;
		}
		JSONObject cloneValue = null;
		SharedPreferences.Editor editor = preferences.edit();
		if (value != null) {
			cloneValue = copy(value);
			if (cloneValue != null) {
				String serialized = cloneValue.toString();
				editor.putString(key, serialized);
			} else {
				editor.putString(key, null);
			}
		} else {
			editor.putString(key, null);
		}
		editor.apply();

		this.value = cloneValue;
	}

	private JSONObject copy(JSONObject original) {
		try {
			JSONArray names = original.names();
			if (names == null) {
				return new JSONObject();
			}
			List<String> namesList = new ArrayList<>();
			for (int i = 0; i < names.length(); i++) {
				namesList.add(names.getString(i));
			}
			String[] namesArray = namesList.toArray(new String[0]);
			return new JSONObject(original, namesArray);
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void merge(@NonNull JSONObject value) {
		JSONObject merged = this.value != null ? this.value : new JSONObject();
		JsonUtils.mergeJson(value, merged);
		set(merged);
	}
}
