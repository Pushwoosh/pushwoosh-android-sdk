package com.pushwoosh.notification;

import androidx.annotation.ColorInt;

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
}
