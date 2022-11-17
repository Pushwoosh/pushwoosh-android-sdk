package com.pushwoosh.huawei.internal.specific;

import com.pushwoosh.huawei.internal.registrar.HuaweiPushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecific;

public class DeviceSpecificIniter {

    public static DeviceSpecific create(HuaweiPushRegistrar pushRegistrar) {
        return new HuaweiDeviceSpecific(pushRegistrar);
    }
}
