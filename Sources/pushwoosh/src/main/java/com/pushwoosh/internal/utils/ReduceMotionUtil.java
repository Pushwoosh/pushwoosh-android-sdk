package com.pushwoosh.internal.utils;

import android.content.Context;
import android.provider.Settings;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

/**
 * Detects that the user has turned system animations off (Animator duration scale == 0), the same
 * signal {@code pushwoosh-inapp-ui}'s {@code ReduceMotionUtil} uses. Duplicated here on purpose:
 * the dependency is one-way (pushwoosh-inapp-ui depends on core), so core cannot reuse that class.
 */
public class ReduceMotionUtil {
    private static final String TAG = "ReduceMotionUtil";

    private ReduceMotionUtil() {}

    /** True when animations are off. Reads the setting on every call; on any failure returns false. */
    public static boolean isReduceMotionEnabled() {
        try {
            Context context = AndroidPlatformModule.getApplicationContext();
            if (context == null) {
                return false;
            }
            return Settings.Global.getFloat(context.getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
                    == 0f;
        } catch (Exception e) {
            PWLog.warn(TAG, "Failed to read ANIMATOR_DURATION_SCALE, assuming animations on", e);
            return false;
        }
    }
}
