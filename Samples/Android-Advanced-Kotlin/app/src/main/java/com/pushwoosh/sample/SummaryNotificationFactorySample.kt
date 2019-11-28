package com.pushwoosh.sample

import com.pushwoosh.notification.PushwooshSummaryNotificationFactory

class SummaryNotificationFactorySample : PushwooshSummaryNotificationFactory() {
    override fun summaryNotificationMessage(messagesCount: Int): String {
        return "$messagesCount new messages"
    }

    override fun summaryNotificationIconResId(): Int {
        return R.drawable.ic_notification
    }
}