package com.pushwoosh.internal.utils;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;

/**
 * PROBLEM: DeviceUtils.getDeviceUUID() works via callback! HWID flows asynchronously.
 * SDK is considered ready only after two asynchronous events:
 * ApplicationIdReadyEvent
 * InitHwidEvent (from HWID callback)
 */
public class SdkStatusChecker {

    public static boolean isInitialized() {
        return isDeviceProviderOk() && isContextOk() && isPushwooshOk();
    }

    public static boolean isPushwooshOk() {
        return PushwooshPlatform.getInstance() != null;
    }

    public static boolean isContextOk() {
        return AndroidPlatformModule.isInit();
    }

    public static boolean isDeviceProviderOk() {
        return DeviceSpecificProvider.isInited();
    }
}
