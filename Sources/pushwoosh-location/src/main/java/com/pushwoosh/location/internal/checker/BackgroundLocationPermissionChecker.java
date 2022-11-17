package com.pushwoosh.location.internal.checker;

import android.Manifest;
import android.content.Context;
import android.os.Build;

import com.pushwoosh.internal.checker.Checker;

public class BackgroundLocationPermissionChecker implements Checker {
    private final Context context;

    public BackgroundLocationPermissionChecker(Context context) {
        this.context = context;
    }

    @Override
    public boolean check() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return RuntimePermissionHelper.hasSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
        }
        return true;
    }
}
