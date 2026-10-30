package com.pushwoosh.location.foregroundservice;

import android.annotation.SuppressLint;

public class ForegroundServiceHelperRepository {

    // The only producer, LocationModule, builds the helper from the application context
    @SuppressLint("StaticFieldLeak")
    private static ForegroundServiceHelper foregroundServiceHelper;

    public static ForegroundServiceHelper getForegroundServiceHelper() {
        return foregroundServiceHelper;
    }

    public static void setForegroundServiceHelper(ForegroundServiceHelper foregroundServiceHelper) {
        ForegroundServiceHelperRepository.foregroundServiceHelper = foregroundServiceHelper;
    }
}
