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

package com.pushwoosh.firebase.internal.mapper;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class RemoteMessageMapper {

    private static final String KEY_PW_MSG_TAG = "pw_msg_tag";

    @NonNull public static Bundle mapToBundle(@Nullable RemoteMessage remoteMessage) {
        if (remoteMessage == null) {
            return new Bundle();
        }

        Map<String, String> data = remoteMessage.getData();
        Bundle bundle = new Bundle();
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }
        }

        // Use FCM collapse_key as notification tag so a new push with the same key
        // replaces the previous one in the shade (parity with iOS apns_collapse_id).
        // Don't overwrite an explicit pw_msg_tag set via custom data.
        String collapseKey = remoteMessage.getCollapseKey();
        if (!TextUtils.isEmpty(collapseKey) && TextUtils.isEmpty(bundle.getString(KEY_PW_MSG_TAG))) {
            bundle.putString(KEY_PW_MSG_TAG, collapseKey);
        }
        return bundle;
    }
}
