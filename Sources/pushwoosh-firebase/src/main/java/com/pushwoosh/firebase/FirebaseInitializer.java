package com.pushwoosh.firebase;

import android.content.Context;

import com.pushwoosh.firebase.internal.specific.FcmDeviceSpecificIniter;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;

public class FirebaseInitializer {
    public static void init(Context context) {
        new DeviceSpecificProvider.Builder()
                .setDeviceSpecific(FcmDeviceSpecificIniter.create())
                .build(true);
    }
}
