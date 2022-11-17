package com.pushwoosh.testingapp;

import android.graphics.Color;

import com.pushwoosh.notification.PushwooshSummaryNotificationFactory;

public class TestingAppSummaryNotificationFactory extends PushwooshSummaryNotificationFactory {

    @Override
    public String summaryNotificationMessage(int notificationsAmount) {
        return notificationsAmount + " new messages";
    }

    @Override
    public int summaryNotificationIconResId() {
        return super.summaryNotificationIconResId();
    }

    @Override
    public boolean autoCancelSummaryNotification() {
        return super.autoCancelSummaryNotification();
    }

    @Override
    public int summaryNotificationColor() {
        return Color.RED;
    }
}
