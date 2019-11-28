package com.pushwoosh.sample;

import com.pushwoosh.notification.PushwooshSummaryNotificationFactory;

public class SummaryNotificationFactorySample extends PushwooshSummaryNotificationFactory {

    @Override
    public String summaryNotificationMessage(int messagesCount) {
        return messagesCount + " new messages";
    }

    @Override
    public int summaryNotificationIconResId() {
        return R.drawable.ic_notification;
    }
}
