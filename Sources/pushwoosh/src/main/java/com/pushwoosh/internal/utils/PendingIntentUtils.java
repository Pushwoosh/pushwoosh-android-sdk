package com.pushwoosh.internal.utils;

import android.app.PendingIntent;
import android.os.Build;

public class PendingIntentUtils {
    public static int addImmutableFlag(int flag) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? flag | PendingIntent.FLAG_IMMUTABLE : flag;
    }
}
