package com.pushwoosh.huawei.internal.mapper;

import android.os.Bundle;

import com.huawei.hms.push.RemoteMessage;

import java.util.Map;

import androidx.annotation.NonNull;

public class RemoteMessageMapper {
    @NonNull
    public static Bundle mapToBundle(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getDataOfMap();
        Bundle bundle = new Bundle();
        if (data == null) {
            return bundle;
        }
        for (Map.Entry<String, String> entry : data.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }

        return bundle;
    }
}
