package com.pushwoosh.xiaomi.internal.specific;

import static com.pushwoosh.internal.specific.DeviceSpecificProvider.XIAOMI_TYPE;

import com.pushwoosh.internal.registrar.PushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecific;
import com.pushwoosh.xiaomi.internal.registrar.XiaomiPushRegistrar;

public class XiaomiDeviceSpecific implements DeviceSpecific {
    private static final int XIAOMI_DEVICE_TYPE = 19;
    private static final String XIAOMI_PROJECT_ID = "XIAOMI_DEVICE";
    private XiaomiPushRegistrar pushRegistrar;

    public XiaomiDeviceSpecific(XiaomiPushRegistrar pushRegistrar) {
        this.pushRegistrar = pushRegistrar;
    }
    @Override
    public PushRegistrar pushRegistrar() { return pushRegistrar; }

    @Override
    public String permission(String packageName) {
        return null;
    }

    @Override
    public int deviceType() {
        return XIAOMI_DEVICE_TYPE;
    }

    @Override
    public String projectId() {
        return XIAOMI_PROJECT_ID;
    }

    @Override
    public String type() {
        return XIAOMI_TYPE;
    }
}
