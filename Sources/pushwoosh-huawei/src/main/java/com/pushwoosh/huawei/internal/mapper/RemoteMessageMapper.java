package com.pushwoosh.huawei.internal.mapper;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.huawei.hms.push.RemoteMessage;

import java.util.Map;

public class RemoteMessageMapper {

    private static final String KEY_PW_MSG_TAG = "pw_msg_tag";

    @NonNull public static Bundle mapToBundle(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getDataOfMap();
        Bundle bundle = new Bundle();
        if (data != null) {
            for (Map.Entry<String, String> entry : data.entrySet()) {
                bundle.putString(entry.getKey(), entry.getValue());
            }
        }

        // Mirror FCM behaviour: HMS collapse_key → pw_msg_tag (parity with iOS apns_collapse_id).
        // Don't overwrite an explicit pw_msg_tag set via custom data.
        String collapseKey = remoteMessage.getCollapseKey();
        if (!TextUtils.isEmpty(collapseKey) && TextUtils.isEmpty(bundle.getString(KEY_PW_MSG_TAG))) {
            bundle.putString(KEY_PW_MSG_TAG, collapseKey);
        }
        return bundle;
    }
}
