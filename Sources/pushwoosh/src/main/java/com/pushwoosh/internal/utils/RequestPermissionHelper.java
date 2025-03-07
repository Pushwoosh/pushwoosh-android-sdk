package com.pushwoosh.internal.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;

public class RequestPermissionHelper {
    public static final String TAG = "RequestPermissionHelper";
    public static final String EXTRA_PERMISSIONS = "extra_permissions";

    public static void requestPermissionsForClass(Class<? extends PermissionActivity> cls, Context context, String[] permissions) {
        boolean needRequestPermissions = false;
        try {
            for (String permission : permissions) {
                needRequestPermissions = needRequestPermissions || ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED;
            }
        } catch (Exception e) {
            PWLog.error("an error occurred while trying to requestPermissions", e);
        }

        if (needRequestPermissions) {
            PWLog.info(TAG, "Requesting permissions");
            Intent intent = new Intent(context, cls);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(EXTRA_PERMISSIONS, permissions);
            context.startActivity(intent);
        }
    }
}
