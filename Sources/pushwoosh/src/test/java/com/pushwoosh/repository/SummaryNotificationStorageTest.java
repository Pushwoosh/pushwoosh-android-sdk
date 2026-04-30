package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.app.Application;

import com.pushwoosh.exception.GroupIdNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class SummaryNotificationStorageTest {
    private SummaryNotificationStorage storage;

    @Before
    public void setUp() {
        Application application = RuntimeEnvironment.application;
        storage = new SummaryNotificationStorageImpl(application);
    }

    @Test
    public void remove_groupIdWithUnderscores_removesEntry() throws Exception {
        // Reproduces the historical SQL bug where unquoted identifier-like values
        // ("group_undefined") were parsed as column names by SQLite.
        storage.put("group_undefined", 1002);
        assertEquals(1002, storage.getNotificationId("group_undefined"));

        storage.remove("group_undefined");

        try {
            storage.getNotificationId("group_undefined");
            fail("Entry should be removed");
        } catch (GroupIdNotFoundException expected) {
            // ok
        }
    }

    @Test
    public void remove_groupIdWithSpecialCharacters_removesEntry() throws Exception {
        String groupId = "group with spaces and 'quotes'";
        storage.put(groupId, 42);
        storage.remove(groupId);

        try {
            storage.getNotificationId(groupId);
            fail("Entry should be removed");
        } catch (GroupIdNotFoundException expected) {
            // ok
        }
    }

    @Test
    public void remove_keepsEntriesForOtherGroups() throws Exception {
        storage.put("group-A", 1);
        storage.put("group-B", 2);

        storage.remove("group-A");

        assertEquals(2, storage.getNotificationId("group-B"));
    }
}
