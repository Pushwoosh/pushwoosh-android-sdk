package com.pushwoosh.xiaomi;

import android.content.Context;

import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.xiaomi.internal.registrar.XiaomiPushRegistrar;
import com.pushwoosh.xiaomi.internal.specific.XiaomiDeviceSpecificIniter;
import com.pushwoosh.xiaomi.utils.XiaomiUtils;

public class XiaomiInitializer {

    public static void init(Context context) {
        PWLog.debug("XIAOMI");
        if (XiaomiUtils.isXiaomiDevice()) {
            new DeviceSpecificProvider.Builder()
                    .setDeviceSpecific(XiaomiDeviceSpecificIniter.create(new XiaomiPushRegistrar()))
                    .build(true);
        } else {
            final String message = "This is not a Xiaomi device. The service is not available.";
            PWLog.error(message);
        }
    }
}
