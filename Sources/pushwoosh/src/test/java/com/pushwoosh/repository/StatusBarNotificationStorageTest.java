package com.pushwoosh.repository;

import static org.junit.Assert.*;

import android.app.Application;
import android.util.Pair;

import com.pushwoosh.exception.NotificationIdNotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import dalvik.annotation.TestTarget;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23)
public class StatusBarNotificationStorageTest {
    private StatusBarNotificationStorage storage;
    long pushwooshId = 123456789;
    int statusBarId = 1;
    Application application;

    @Before
    public void setUp() throws Exception {
        application = RuntimeEnvironment.application;
        storage = new StatusBarNotificationStorageImpl(application);
        storage.put(pushwooshId, statusBarId);
    }

    @Test
    public void getNotificationIdTest() throws Exception {
        storage.put(pushwooshId, 2);
        assertEquals(2, storage.get(pushwooshId));

        storage.put(123L, 2);
        assertEquals(2, storage.get(pushwooshId));
    }

    @Test
    public void putNotificationIdsPairTest() throws Exception {
        assertEquals(1, storage.get(pushwooshId));
    }

    @Test
    public void removeNotificationIdsPairForPushwooshId() throws Exception {
        storage.remove(pushwooshId);
        try {
            storage.get(pushwooshId);
            Assert.fail();
        }  catch (NotificationIdNotFoundException e) {}
    }

    @Test
    public void updateNotificationStorageTest() throws Exception {
        ArrayList<Pair<Long, Integer>> ids = new ArrayList<>();
        ids.add(Pair.create(111L,123));
        ids.add(Pair.create(222L,456));
        storage.update(ids);
        try {
            storage.get(pushwooshId);
            Assert.fail();
        }  catch (NotificationIdNotFoundException e) {}
        assertEquals(123,storage.get(111L));
        assertEquals(456,storage.get(222L));
    }

    @Test
    public void updateNotificationStorageWithNullIdsTest() throws Exception {
        storage.update(null);
        try {
            storage.get(pushwooshId);
            Assert.fail();
        } catch (NotificationIdNotFoundException e) {}
    }
}