package com.pushwoosh.notification;

public class FakeValidSummaryNotificationFactory extends SummaryNotificationFactory {
    public FakeValidSummaryNotificationFactory() {
        super();
    }

    @Override
    public String summaryNotificationMessage(int notificationsAmount) {
        return "";
    }

    @Override
    public int summaryNotificationIconResId() {
        return -1;
    }

    @Override
    public int summaryNotificationColor() {
        return -1;
    }
}
