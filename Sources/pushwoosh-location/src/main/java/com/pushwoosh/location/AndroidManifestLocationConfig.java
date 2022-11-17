package com.pushwoosh.location;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

public class AndroidManifestLocationConfig {
    private static final String TAG = "Config";

    private boolean isStartForegroundService = false;
    private String textForegroundServiceNotification = null;
    private String channelNameForegroundServiceNotification = null;

    public AndroidManifestLocationConfig(Context context) {
        if(context == null){
            return;
        }
        ApplicationInfo applicationInfo = getApplicationInfo(context);
        if (applicationInfo == null || applicationInfo.metaData == null) {
            PWLog.warn(TAG, "no metadata found");
            return;
        }

        isStartForegroundService = applicationInfo.metaData.getBoolean("com.pushwoosh.start_foreground_service", false);
        textForegroundServiceNotification = applicationInfo.metaData.getString("com.pushwoosh.foreground_service_notification_text", "Work in progress");
        channelNameForegroundServiceNotification = applicationInfo.metaData.getString("com.pushwoosh.foreground_service_notification_channel_name", "Foreground service");
    }

    @Nullable
    private ApplicationInfo getApplicationInfo(Context context) {
        ApplicationInfo applicationInfo = null;
        try {
            applicationInfo  = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        } catch (Exception e) {
            PWLog.exception(e);
        }
        return applicationInfo;
    }


    public boolean isStartForegroundService() {
        return isStartForegroundService;
    }

    public String getTextForegroundServiceNotification() {
        return textForegroundServiceNotification;
    }


    public String getChannelNameForegroundServiceNotification() {
        return channelNameForegroundServiceNotification;
    }
}
