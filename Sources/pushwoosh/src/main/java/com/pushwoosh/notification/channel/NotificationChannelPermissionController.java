package com.pushwoosh.notification.channel;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.annotation.RequiresApi;
import android.text.TextUtils;

import com.pushwoosh.repository.RepositoryModule;

import static com.pushwoosh.PermissionController.DISABLE_ALL;
import static com.pushwoosh.PermissionController.ENABLE_ALERT;
import static com.pushwoosh.PermissionController.ENABLE_ALERT_AND_SOUND;

public class NotificationChannelPermissionController {


    @RequiresApi(api = Build.VERSION_CODES.O)
    public static int getBitMask(NotificationManager notificationManager) {
        String channelName = RepositoryModule.getNotificationPreferences().channelName().get();
        final String channelId = NotificationChannelInfoProvider.channelId(channelName);
        if (!TextUtils.isEmpty(channelId)) {
            NotificationChannel channel = notificationManager.getNotificationChannel(channelId);
            if (channel == null) {
                return ENABLE_ALERT_AND_SOUND;
            }
            return getBitMaskInternal(channel);
        } else {
            return ENABLE_ALERT_AND_SOUND;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static int getBitMaskInternal(NotificationChannel channel) {
        if (isDisableAll(channel)) {
            return DISABLE_ALL;
        } else if (isDisableSound(channel)) {
            return ENABLE_ALERT;
        } else {
            return ENABLE_ALERT_AND_SOUND;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static boolean isDisableSound(NotificationChannel channel) {
        return channel.getImportance() <= NotificationManager.IMPORTANCE_LOW || channel.getSound() == null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static boolean isDisableAll(NotificationChannel channel) {
        return channel.getImportance() == NotificationManager.IMPORTANCE_NONE;
    }

}
