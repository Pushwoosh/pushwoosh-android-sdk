package com.pushwoosh.notification.builder;

import android.app.Notification;
import androidx.core.app.NotificationCompat;

public interface SummaryNotificationBuilder {
    SummaryNotificationBuilder setSmallIcon(int icon);

    SummaryNotificationBuilder setNumber(int number);

    SummaryNotificationBuilder setStyle(NotificationCompat.InboxStyle style);

    SummaryNotificationBuilder setAutoCancel(boolean autoCancel);

    SummaryNotificationBuilder setGroup(String group);

    SummaryNotificationBuilder setGroupSummary(boolean isGroupSummary);

    SummaryNotificationBuilder setColor(int color);

    Notification build();
}
