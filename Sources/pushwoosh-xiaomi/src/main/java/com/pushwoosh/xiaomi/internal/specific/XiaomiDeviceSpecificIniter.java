package com.pushwoosh.xiaomi.internal.specific;

import com.pushwoosh.internal.specific.DeviceSpecific;
import com.pushwoosh.xiaomi.internal.registrar.XiaomiPushRegistrar;

public class XiaomiDeviceSpecificIniter {

    public static DeviceSpecific create(XiaomiPushRegistrar pushRegistrar) {
        return new XiaomiDeviceSpecific(pushRegistrar);
    }
}
