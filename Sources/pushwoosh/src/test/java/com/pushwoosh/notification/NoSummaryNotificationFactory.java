package com.pushwoosh.notification;

// SummaryNotificationFactory() is package-private, so a factory a test registers must live here,
// not next to the test class: see FakeValidSummaryNotificationFactory for the same constraint.
public class NoSummaryNotificationFactory extends SummaryNotificationFactory {
    public NoSummaryNotificationFactory() {
        super();
    }

    @Override
    public boolean shouldGenerateSummaryNotification() {
        return false;
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
