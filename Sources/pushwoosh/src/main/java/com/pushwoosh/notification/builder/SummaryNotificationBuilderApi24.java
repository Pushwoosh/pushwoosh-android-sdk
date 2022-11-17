package com.pushwoosh.notification.builder;

import android.app.Notification;
import android.content.Context;
import android.os.Build;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

@RequiresApi(Build.VERSION_CODES.N)
public class SummaryNotificationBuilderApi24 implements SummaryNotificationBuilder {
    private final NotificationCompat.Builder builder;

    SummaryNotificationBuilderApi24(Context context, String channelId) {
        builder = new NotificationCompat.Builder(context, channelId);
    }

    @Override
    public SummaryNotificationBuilder setSmallIcon(int icon) {
        builder.setSmallIcon(icon);
        if (icon == -1) {
            if (AndroidPlatformModule.getApplicationContext() != null) {
                builder.setSmallIcon(AppIconHelper.getAppIconResId(AndroidPlatformModule.getApplicationContext()));
            }
        }
        return this;
    }

    @Override
    public SummaryNotificationBuilder setNumber(int number) {
        builder.setNumber(number);
        return this;
    }

    @Override
    public SummaryNotificationBuilder setStyle(NotificationCompat.InboxStyle style) {
        builder.setStyle(style);
        return this;
    }

    @Override
    public SummaryNotificationBuilder setAutoCancel(boolean autoCancel) {
        builder.setAutoCancel(autoCancel);
        return this;
    }

    @Override
    public SummaryNotificationBuilder setGroup(String group) {
        builder.setGroup(group);
        return this;
    }

    @Override
    public SummaryNotificationBuilder setGroupSummary(boolean isGroupSummary) {
        builder.setGroupSummary(isGroupSummary);
        return this;
    }

    @Override
    public SummaryNotificationBuilder setColor(int color) {
        builder.setColor(color);
        return this;
    }

    @Override
    public Notification build() {
        return builder.build();
    }
}
