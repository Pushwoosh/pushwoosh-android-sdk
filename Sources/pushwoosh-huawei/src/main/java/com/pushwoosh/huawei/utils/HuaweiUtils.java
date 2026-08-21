package com.pushwoosh.huawei.utils;

import android.content.Context;
import android.os.Build;

import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.pushwoosh.internal.utils.PWLog;

/**
 * Utility class for detecting Huawei/Honor devices that should use HMS Push Kit.
 * <p>
 * Provides device detection logic to determine whether to use HMS Push or FCM.
 * Uses a 4-level validation: manufacturer check, HMS availability, GMS prioritization,
 * and Push Kit presence.
 */
public class HuaweiUtils {
    private static final String TAG = "HuaweiUtils";
    private static final String LIBRARY_NOT_INTEGRATED_ERROR =
            "Huawei HMS Core SDK - Push Kit not integrated. Follow the guide to integrate the library "
                    + "to your project https://developer.huawei.com/consumer/en/doc/development/HMSCore-Guides-V5/android-integrating-sdk-0000001050040084-V5";

    /**
     * Determines if the device should use HMS Push Kit for push notifications.
     * <p>
     * Returns {@code true} only if:
     * <ul>
     *   <li>Device manufacturer/brand contains "huawei" or "honor"</li>
     *   <li>HMS Core is installed and available</li>
     *   <li>Google Play Services is NOT available (GMS takes priority if present)</li>
     *   <li>HMS Push Kit classes are on the classpath</li>
     * </ul>
     *
     * @param context Application context
     * @return {@code true} if device should use HMS Push, {@code false} otherwise
     */
    public static boolean isHuaweiDevice(Context context) {
        if (!isHuaweiManufacturer()) {
            PWLog.debug(TAG, "Not a Huawei/Honor manufacturer: " + Build.MANUFACTURER + " / " + Build.BRAND);
            return false;
        }

        PWLog.debug(TAG, "Detected Huawei/Honor manufacturer: " + Build.MANUFACTURER + " / " + Build.BRAND);

        if (!isHmsCoreAvailable(context)) {
            PWLog.warn(TAG, "Huawei/Honor device but HMS Core not available");
            return false;
        }

        if (isGooglePlayServicesAvailable(context)) {
            PWLog.info(TAG, "Huawei/Honor device has both GMS and HMS - preferring GMS");
            return false;
        }

        if (!isHmsPushKitAvailable()) {
            return false;
        }

        PWLog.info(TAG, "Huawei/Honor device with HMS Core only - using HMS Push");
        return true;
    }

    /**
     * Checks if device manufacturer/brand contains "huawei" or "honor".
     * <p>
     * Uses substring matching (contains) instead of exact matching for forward compatibility
     * with potential manufacturer name changes. Case-insensitive comparison.
     *
     * @return {@code true} if manufacturer or brand contains "huawei" or "honor"
     */
    private static boolean isHuaweiManufacturer() {
        String manufacturer = Build.MANUFACTURER;
        String brand = Build.BRAND;

        String manufacturerLower = manufacturer != null ? manufacturer.toLowerCase() : "";
        String brandLower = brand != null ? brand.toLowerCase() : "";

        return manufacturerLower.contains("huawei")
                || manufacturerLower.contains("honor")
                || brandLower.contains("huawei")
                || brandLower.contains("honor");
    }

    /**
     * Checks if HMS Core is installed and available.
     * <p>
     * Returns {@code true} only if HMS Core returns {@link ConnectionResult#SUCCESS}.
     * Handles {@link NoClassDefFoundError} when HMS SDK is not integrated.
     *
     * @param context Application context
     * @return {@code true} if HMS Core is available
     */
    private static boolean isHmsCoreAvailable(Context context) {
        try {
            int result = HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context);

            if (result == ConnectionResult.SUCCESS) {
                return true;
            } else {
                PWLog.debug(TAG, "HMS Core not available, result code: " + result);
                return false;
            }
        } catch (NoClassDefFoundError error) {
            PWLog.error(TAG, LIBRARY_NOT_INTEGRATED_ERROR);
            return false;
        } catch (Throwable t) {
            PWLog.error(TAG, "Error checking HMS availability", t);
            return false;
        }
    }

    /**
     * Checks that HMS Push Kit classes are on the classpath.
     * <p>
     * The other checks key on {@code hms:base} ({@link HuaweiApiAvailability}) — a transitive
     * dependency of any HMS kit. An app with Huawei Maps/Analytics but without {@code hms:push}
     * would otherwise claim the push slot with no way to obtain a token.
     * <p>
     * Package-private for testing.
     *
     * @return {@code true} if {@code com.huawei.hms:push} is on the classpath
     */
    static boolean isHmsPushKitAvailable() {
        try {
            // direct class touch, not Class.forName: the app's R8 may rename Huawei classes
            HmsInstanceId.class.getName();
            return true;
        } catch (LinkageError error) {
            // LinkageError, not NoClassDefFoundError: a binary mismatch between hms kits throws
            // VerifyError/IncompatibleClassChangeError, which would crash the provider on every start
            PWLog.error(TAG, LIBRARY_NOT_INTEGRATED_ERROR);
            return false;
        }
    }

    /**
     * Checks if Google Play Services is available using reflection.
     * <p>
     * Uses reflection to avoid compile-time dependency on GMS.
     * Returns {@code true} if GMS is available (result code 0).
     * Used to prioritize GMS over HMS on devices with both services.
     * <p>
     * Package-private for testing.
     *
     * @param context Application context
     * @return {@code true} if GMS is available
     */
    static boolean isGooglePlayServicesAvailable(Context context) {
        return checkPlayServicesAvailability("com.google.android.gms.common.GoogleApiAvailability", context);
    }

    /**
     * Checks availability of a Play Services API using reflection.
     * <p>
     * Uses reflection to invoke getInstance() and isGooglePlayServicesAvailable()
     * on the specified class. Returns {@code true} if result code is 0 (SUCCESS).
     * <p>
     * Package-private for testing with fake implementations.
     *
     * @param className Fully qualified class name to check (e.g. GoogleApiAvailability)
     * @param context Application context
     * @return {@code true} if service available (result code 0)
     */
    static boolean checkPlayServicesAvailability(String className, Context context) {
        try {
            Class<?> apiAvailability = Class.forName(className);
            Object instance = apiAvailability.getMethod("getInstance").invoke(null);
            Integer result = (Integer) apiAvailability
                    .getMethod("isGooglePlayServicesAvailable", Context.class)
                    .invoke(instance, context);

            return result != null && result == 0;
        } catch (ClassNotFoundException e) {
            PWLog.debug(TAG, "Play Services not integrated: " + className);
            return false;
        } catch (Throwable t) {
            PWLog.debug(TAG, "Error checking Play Services availability", t);
            return false;
        }
    }
}
