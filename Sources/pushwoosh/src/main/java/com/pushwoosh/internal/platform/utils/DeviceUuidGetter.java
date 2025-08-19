package com.pushwoosh.internal.platform.utils;

@FunctionalInterface
public interface DeviceUuidGetter {
    void getDeviceUUID(OnGetHwidListener listener);
}
