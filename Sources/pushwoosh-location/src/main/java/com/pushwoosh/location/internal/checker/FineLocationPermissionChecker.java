package com.pushwoosh.location.internal.checker;

import android.Manifest;
import android.content.Context;

import com.pushwoosh.internal.checker.Checker;

public class FineLocationPermissionChecker implements Checker {
    private final Context context;

    public FineLocationPermissionChecker(Context context) {
        this.context = context;
    }

    @Override
    public boolean check() {
        return RuntimePermissionHelper.hasSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }
}
