package com.pushwoosh.xiaomi.utils;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import com.pushwoosh.internal.utils.PWLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class XiaomiUtils {
    private static final String TAG = "XiaomiUtils";
    private static final String LIBRARY_NOT_INTEGRATED_ERROR = "Xiaomi integration error";

    public static boolean isXiaomiDevice() {
        try {
            @SuppressLint("PrivateApi") final Class<?> propertyClass = Class.forName("android.os.SystemProperties");
            final Method method = propertyClass.getMethod("get", String.class);
            final String versionName = (String) method.invoke(propertyClass, "ro.miui.ui.version.name");
            return !TextUtils.isEmpty(versionName);
        } catch (ClassNotFoundException e) {
            PWLog.error(TAG, LIBRARY_NOT_INTEGRATED_ERROR);
            PWLog.error(TAG, e);
            return false;
        } catch (Throwable t) {
            PWLog.error(TAG, "Unexpected error occurred:");
            PWLog.error(TAG, t.getMessage());
            return false;
        }
    }
}
