package com.pushwoosh;

import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.channel.NotificationChannelPermissionController;

public class PermissionController {
    public final String TAG = PermissionController.class.getSimpleName();

    public static final int DISABLE_ALL = 0;
    public static final int ENABLE_ALERT = 4;
    public static final int ENABLE_ALERT_AND_SOUND = 6;
    private Context context;
    private NotificationManager notificationManager;

    public PermissionController(Context context, NotificationManager notificationManager) {
        this.context = context;
        this.notificationManager = notificationManager;
    }

    public int getBitMaskPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
           return isAlertEnable() ? getBitMaskByChannel() : DISABLE_ALL;
        } else {
            return isAlertEnable() ? ENABLE_ALERT_AND_SOUND : DISABLE_ALL;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private int getBitMaskByChannel() {
        if (notificationManager != null) {
           return NotificationChannelPermissionController.getBitMask(notificationManager);
        } else {
            PWLog.error(TAG, "notificationManager is null");
            return ENABLE_ALERT_AND_SOUND;
        }
    }


    private boolean isAlertEnable() {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
}
