package com.pushwoosh.notification;

import androidx.annotation.ColorInt;
import static com.pushwoosh.repository.NotificationPrefs.DEFAULT_NOTIFICATION_GROUP;

public class PushwooshSummaryNotificationFactory extends SummaryNotificationFactory {

    @Override
    public String summaryNotificationMessage(int notificationsAmount) {
        return "";
    }

    @Override
    public int summaryNotificationIconResId() {
        return -1; // default small icon
    }

    @Override
    @ColorInt
    public int summaryNotificationColor() {
        return -1; // default color
    }

    @Override
    public String summaryNotificationGroup() {
        return DEFAULT_NOTIFICATION_GROUP;
    }
}
