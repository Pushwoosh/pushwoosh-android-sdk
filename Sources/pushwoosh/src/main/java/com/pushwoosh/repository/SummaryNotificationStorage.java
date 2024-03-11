package com.pushwoosh.repository;

import android.util.Pair;

import com.pushwoosh.exception.GroupIdNotFoundException;
import com.pushwoosh.exception.NotificationIdNotFoundException;

import java.util.List;

public interface SummaryNotificationStorage {
    void put(String groupId, int notificationId);
    int remove(String groupId) throws GroupIdNotFoundException;
    void update(List<Pair<String, Integer>> ids); // Drops table and inserts new ids
    int getNotificationId(String groupId) throws GroupIdNotFoundException;
    String getGroup(int notificationId) throws NotificationIdNotFoundException;
}
