package com.pushwoosh.location.internal.checker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.pushwoosh.internal.checker.Checker;

public class BackgroundLocationPermissionDeclaredChecker implements Checker {
    private final Context context;

    public BackgroundLocationPermissionDeclaredChecker(Context context) {
        this.context = context;
    }

    @Override
    public boolean check() {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_PERMISSIONS);
            for (String permission : packageInfo.requestedPermissions) {
                if (TextUtils.equals(permission, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    return true;
                }
            }
        } catch (Throwable t) {
            return false;
        }
        return false;
    }
}
