package com.pushwoosh.notification;

public class FakeNoCtorSummaryNotificationFactory extends SummaryNotificationFactory {
    @SuppressWarnings("unused")
    public FakeNoCtorSummaryNotificationFactory(int unused) {
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
