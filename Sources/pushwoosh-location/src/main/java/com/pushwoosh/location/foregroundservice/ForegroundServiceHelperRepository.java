package com.pushwoosh.location.foregroundservice;

public class ForegroundServiceHelperRepository {

    private static ForegroundServiceHelper foregroundServiceHelper;

    public static ForegroundServiceHelper getForegroundServiceHelper() {
        return foregroundServiceHelper;
    }

    public static void setForegroundServiceHelper(ForegroundServiceHelper foregroundServiceHelper) {
        ForegroundServiceHelperRepository.foregroundServiceHelper = foregroundServiceHelper;
    }
}
