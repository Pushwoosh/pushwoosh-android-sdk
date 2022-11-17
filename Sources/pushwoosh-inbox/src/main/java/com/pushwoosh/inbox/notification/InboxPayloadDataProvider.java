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

package com.pushwoosh.inbox.notification;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

public class InboxPayloadDataProvider {

	@Nullable
	public static String getInboxId(Bundle bundle) {
		return bundle.getString("pw_inbox", null);
	}

	@Nullable
	public static String getTitle(Bundle bundle) {
		return bundle.getString("header", null);
	}

	@Nullable
	public static String getUrl(JSONObject jsonObject) throws JSONException {
		if (!jsonObject.has("l")) {
			return null;
		}
		return jsonObject.getString("l");
	}

	@Nullable
	public static String getRemoteUrl(JSONObject jsonObject) throws JSONException {
		if (!jsonObject.has("r")) {
			return null;
		}

		return jsonObject.getString("r");
	}

	@Nullable
	public static JSONObject getRichMedia(JSONObject jsonObject) throws JSONException {
		if (!jsonObject.has("rm")) {
			return null;
		}

		return new JSONObject(jsonObject.getString("rm"));
	}

	public static long getSentTime(Bundle pushBundle) {
		return pushBundle.getLong("google.sent_time", System.currentTimeMillis());
	}

	@Nullable
	public static String getMessage(Bundle bundle) {
		return bundle.getString("title");
	}

	@Nullable
	public static String getInboxParams(Bundle pushBundle) {
		return pushBundle.getString("inbox_params");
	}

	public static InboxMessageType getInboxType(Bundle pushBundle) {
		JSONObject jsonObject = JsonUtils.bundleToJsonWithUserData(pushBundle);
		try {
			return getFromJson(jsonObject);
		} catch (JSONException e) {
			PWLog.noise("Failed to parse inbox type form bundle");
		}

		return InboxMessageType.PLAIN;
	}

	public static InboxMessageType getInboxType(String actionPayload) {
		if (TextUtils.isEmpty(actionPayload)) {
			return InboxMessageType.PLAIN;
		}
		try {
			JSONObject jsonPayload = new JSONObject(actionPayload);
			return getFromJson(jsonPayload);
		} catch (JSONException e) {
			PWLog.noise("Failed to parse inbox type from actionPayload");
		}

		return InboxMessageType.PLAIN;
	}

	private static InboxMessageType getFromJson(JSONObject jsonPayload) throws JSONException {
		String remoteUrl;
		if (InboxPayloadDataProvider.getRichMedia(jsonPayload) != null) {
			return InboxMessageType.RICH_MEDIA;
		} else if ((remoteUrl = InboxPayloadDataProvider.getUrl(jsonPayload)) != null) {
			if (remoteUrl.startsWith("http")) {
				return InboxMessageType.URL;
			} else {
				return InboxMessageType.DEEP_LINK;
			}
		} else if (InboxPayloadDataProvider.getRemoteUrl(jsonPayload) != null) {
			return InboxMessageType.REMOTE_URL;
		}

		return InboxMessageType.PLAIN;
	}

	public static String getHash(Bundle pushBundle) {
		return pushBundle.getString("p", null);
	}
}
