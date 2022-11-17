package com.pushwoosh.repository;

import androidx.annotation.Nullable;

public interface InboxNotificationStorage {
    void putNotificationIdAndTag(String inboxMessageId, int notificationId, String notificationTag);
    @Nullable Integer getNotificationId(String inboxMessageId);
    @Nullable String getNotificationTag(String inboxMessageId);
}
