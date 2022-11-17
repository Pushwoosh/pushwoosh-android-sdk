package com.pushwoosh.huawei.internal.specific;

import com.pushwoosh.huawei.internal.registrar.HuaweiPushRegistrar;
import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecific;

import static com.pushwoosh.internal.specific.DeviceSpecificProvider.HUAWEI_TYPE;

public class HuaweiDeviceSpecific implements DeviceSpecific {
    private static final int HUAWEI_DEVICE_TYPE = 17;
    private static final String HUAWEI_PROJECT_ID = "HUAWEI_DEVICE";

    private HuaweiPushRegistrar pushRegistrar;

    public HuaweiDeviceSpecific(HuaweiPushRegistrar pushRegistrar) {
        this.pushRegistrar = pushRegistrar;
    }

    @Override
    public PushRegistrar pushRegistrar() {
        return pushRegistrar;
    }

    @Override
    public String permission(String packageName) {
        return null;
    }

    @Override
    public int deviceType() {
        return HUAWEI_DEVICE_TYPE;
    }

    @Override
    public String projectId() {
        return HUAWEI_PROJECT_ID;
    }

    @Override
    public String type() {
        return HUAWEI_TYPE;
    }
}
