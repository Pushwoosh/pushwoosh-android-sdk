package com.pushwoosh.huawei;

import android.content.Context;

import com.pushwoosh.huawei.internal.registrar.HuaweiPushRegistrar;
import com.pushwoosh.huawei.internal.specific.DeviceSpecificIniter;
import com.pushwoosh.huawei.utils.HuaweiUtils;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;

public class HuaweiInitializer {
    public static void init(Context context) {
        if (HuaweiUtils.isHuaweiDevice(context)) {
            new DeviceSpecificProvider.Builder()
                    .setDeviceSpecific(DeviceSpecificIniter.create(new HuaweiPushRegistrar()))
                    .build(true);
        } else {
            final String message = "This is not a Huawei device. The service is not available.";
            PWLog.debug(message);
        }
    }
}
