package com.pushwoosh.notification;

// SummaryNotificationFactory() is package-private, so a factory a test registers must live here,
// not next to the test class: see FakeValidSummaryNotificationFactory for the same constraint.
public class RecordingSummaryNotificationFactory extends SummaryNotificationFactory {
    public static volatile int lastAmount = -1;

    public RecordingSummaryNotificationFactory() {
        super();
    }

    @Override
    public String summaryNotificationMessage(int notificationsAmount) {
        lastAmount = notificationsAmount;
        return notificationsAmount + " new orders";
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
