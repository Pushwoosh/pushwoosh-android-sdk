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
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

public class InboxPayloadDataProvider {

    private static final String INBOX_TAB = "pw_inbox_tab";

    /**
     * Reads the tab a message belongs to. The key sits at the root of the payload, next to the
     * other system keys (l, md, b); custom data (u / userdata) is never searched.
     *
     * @param actionParams serialized payload of the message, may be null or not JSON at all
     * @return tab id, or null when the message belongs to no tab
     */
    @Nullable public static String getTabId(@Nullable String actionParams) {
        if (TextUtils.isEmpty(actionParams)) {
            return null;
        }
        try {
            return getTabId(new JSONObject(actionParams));
        } catch (JSONException e) {
            return null;
        }
    }

    @Nullable private static String getTabId(@NonNull JSONObject actionParams) {
        Object value = actionParams.opt(INBOX_TAB);
        if (!(value instanceof String)) {
            return null;
        }
        String tabId = (String) value;
        return tabId.isEmpty() ? null : tabId;
    }

    @Nullable public static String getInboxId(Bundle bundle) {
        return bundle.getString("pw_inbox", null);
    }

    @Nullable public static String getTitle(Bundle bundle) {
        return bundle.getString("header", null);
    }

    @Nullable public static String getUrl(JSONObject jsonObject) throws JSONException {
        if (!jsonObject.has("l")) {
            return null;
        }
        return jsonObject.getString("l");
    }

    /** `rm` is a JSON object from the push builder, or a bare zip URL with a `ts` query when set through root params. */
    @Nullable public static JSONObject getRichMedia(JSONObject jsonObject) throws JSONException {
        if (!jsonObject.has("rm")) {
            return null;
        }

        Object raw = jsonObject.get("rm");
        if (raw instanceof JSONObject) {
            return (JSONObject) raw;
        }
        String value = String.valueOf(raw).trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            JSONObject richMedia = new JSONObject();
            richMedia.put("url", value);
            richMedia.put("ts", timestampFromUrl(value));
            return richMedia;
        }
        return new JSONObject(value);
    }

    private static String timestampFromUrl(String url) {
        int query = url.indexOf('?');
        if (query < 0) {
            return "0";
        }
        for (String pair : url.substring(query + 1).split("&")) {
            if (pair.startsWith("ts=") && pair.length() > 3) {
                return pair.substring(3);
            }
        }
        return "0";
    }

    public static long getSentTime(Bundle pushBundle) {
        return pushBundle.getLong("google.sent_time", System.currentTimeMillis());
    }

    @Nullable public static String getMessage(Bundle bundle) {
        return bundle.getString("title");
    }

    @Nullable public static String getInboxParams(Bundle pushBundle) {
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
        }

        return InboxMessageType.PLAIN;
    }

    public static String getHash(Bundle pushBundle) {
        return pushBundle.getString("p", null);
    }
}
