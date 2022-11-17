package com.pushwoosh.huawei.utils;

import android.content.Context;

import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.pushwoosh.internal.utils.PWLog;

public class HuaweiUtils {
    private static final String TAG = "HuaweiUtils";
    private static final String LIBRARY_NOT_INTEGRATED_ERROR = "Huawei HMS Core SDK - Push Kit " +
            "not integrated. Follow the guide to integrate the library " +
            "to your project https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides-V5/android-integrating-sdk-0000001050040084-V5";

    public static boolean isHuaweiDevice(Context context) {
        try {
            return HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context)
                    == ConnectionResult.SUCCESS;
        } catch (NoClassDefFoundError error) {
            PWLog.error(TAG, LIBRARY_NOT_INTEGRATED_ERROR);
            return false;
        } catch (Throwable t) {
            // To avoid other exceptions
            PWLog.error(TAG, "Unexpected error occurred:");
            PWLog.error(TAG, t.getMessage());
            return false;
        }
    }
}
