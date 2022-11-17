package com.pushwoosh.repository;

import android.util.Pair;

import com.pushwoosh.exception.NotificationIdNotFoundException;

import java.util.List;

public interface StatusBarNotificationStorage {
    void put(long pushwooshId, int statusBarId);
    int remove(long pushwooshId) throws NotificationIdNotFoundException;
    void update(List<Pair<Long, Integer>> ids); // Drops table and inserts new ids
    int get(long pushwooshId) throws NotificationIdNotFoundException;
}
