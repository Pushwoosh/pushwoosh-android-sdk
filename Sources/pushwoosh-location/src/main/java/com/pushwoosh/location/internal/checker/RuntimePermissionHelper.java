package com.pushwoosh.location.internal.checker;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

public class RuntimePermissionHelper {
    /**
     * Determine context has access to the given permission.
     *
     * This is a workaround for RuntimeException of Parcel#readException.
     *
     * @param context context
     * @param permission permission
     * @return returns true if context has access to the given permission, false otherwise.
     */
    public static boolean hasSelfPermission(Context context, String permission) {
        if(context == null) {
            return false;
        }

        try {
            return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
