package com.pushwoosh.internal.utils;

import android.app.KeyguardManager;
import android.os.Build;
import android.os.PowerManager;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

public class LockScreenUtils {
    public static boolean isScreenLocked() {
        //if the app is on the lockscreen
        KeyguardManager kgMgr = AndroidPlatformModule.getManagerProvider().getKeyguardManager();

        //Note: this doesn't work if screenlock is set to none in settings-->security-->screenlock
        boolean lockScreenIsShowing = kgMgr != null && kgMgr.inKeyguardRestrictedInputMode();

        PowerManager powerManager = AndroidPlatformModule.getManagerProvider().getPowerManager();
        boolean isScreenAwake = powerManager == null || (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH ? powerManager.isScreenOn() : powerManager.isInteractive());

        return (lockScreenIsShowing || !isScreenAwake);
    }
}
