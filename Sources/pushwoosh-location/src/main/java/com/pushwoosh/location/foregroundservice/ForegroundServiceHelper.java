package com.pushwoosh.location.foregroundservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.pushwoosh.internal.utils.ImageUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.AndroidManifestLocationConfig;

import static androidx.core.app.NotificationCompat.PRIORITY_MIN;

public class ForegroundServiceHelper {

    public static final String TAG = ForegroundServiceHelper.class.getSimpleName();
    public static final int FOREGROUND_SERVICE_ID = 101;

    private AndroidManifestLocationConfig config;
    private Context context;
    private ImageUtils imageUtils;

    public ForegroundServiceHelper(@Nullable Context context, AndroidManifestLocationConfig config, ImageUtils imageUtils) {
        this.context = context;
        this.config = config;
        this.imageUtils = imageUtils;
    }

    public void startService() {
        if (context == null) {
            PWLog.error(TAG, "Context equals is null");
            return;
        }
        if (config.isStartForegroundService()) {
            Intent intent = new Intent(context, ForegroundService.class);
            //Android 12 does not allow starting service in background and crashes app
            if (Build.VERSION.SDK_INT >= 31)
            {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }

        }
    }

    private Notification getNotification(String channelId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        Bitmap icon = getIcon();
        if (icon != null) {
            builder.setSmallIcon(context.getApplicationInfo().icon);
            builder.setLargeIcon(getIcon());
        }
        String textContext = getContentText();
        if (textContext != null) {
            builder.setContentText(getContentText());
        }
        return builder
                .setContentTitle(getTitle())
                .setChannelId(channelId)
                .setPriority(PRIORITY_MIN)
                .build();
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String channelName = config.getChannelNameForegroundServiceNotification();
        String channelId = formatCannelId(channelName);
        NotificationChannel chan = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (service != null) {
            service.createNotificationChannel(chan);
        } else {
            PWLog.error(TAG, "Notification manager is null");
        }
        return channelId;
    }

    private String formatCannelId(String channelName) {
        return channelName
                .replace(" ","")
                .toLowerCase();
    }

    private String getContentText() {
        return config.getTextForegroundServiceNotification();
    }

    private CharSequence getTitle() {
        ApplicationInfo applicationInfo = context.getApplicationInfo();
        int stringId = applicationInfo.labelRes;
        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : context.getString(stringId);
    }

    private Bitmap getIcon() {
        try {
            String packageName = context.getPackageName();
            Drawable drawable = context.getPackageManager().getApplicationIcon(packageName);
            if (drawable == null) {
                return null;
            }
            return imageUtils.drawableToBitmap(drawable);
        } catch (PackageManager.NameNotFoundException e) {
            PWLog.error(TAG, e);
        }
        return null;
    }

    public void startForeground(Service service) {
        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
        }
        Notification notification = getNotification(channelId);
        service.startForeground(FOREGROUND_SERVICE_ID, notification);
    }
}