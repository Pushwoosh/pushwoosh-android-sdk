package com.pushwoosh.internal.utils;

import java.lang.reflect.Field;

/**
 * This class is used to allow running tests on JDK 8, should be removed as soon as tests are updated
 * to support JDK 11
 */
public class TiramisuApiHelper {
    public static final int TIRAMISU_API = 33;
    public static final String PERMISSION_POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS";

    public static String getReleaseOrCodeName() {
        try {
            Class<?> clazz = Class.forName("android.os.Build$VERSION");
            Field field = clazz.getField("RELEASE_OR_CODENAME");
            return (String) field.get(null);
        } catch (Exception e) {
            return "UNAVAILABLE";
        }
    }
}
