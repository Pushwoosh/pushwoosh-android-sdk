package com.pushwoosh.amazon;

import android.content.Context;

import com.pushwoosh.amazon.internal.AmazonUtils;
import com.pushwoosh.amazon.internal.specific.DeviceSpecificIniter;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;

public class AmazonInitializer {
    public static void init(Context context) {
        if (AmazonUtils.isAmazonDevice()) {
            new DeviceSpecificProvider.Builder()
                    .setDeviceSpecific(DeviceSpecificIniter.create())
                    .build(AmazonUtils.isAmazonDevice());
        } else {
            final String message = "This is not an Amazon device. The service is not available.";
            PWLog.debug(message);
        }
    }
}
