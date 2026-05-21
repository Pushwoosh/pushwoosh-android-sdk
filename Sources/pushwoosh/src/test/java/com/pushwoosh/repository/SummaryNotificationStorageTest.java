package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import android.app.Application;
import android.util.Pair;

import com.pushwoosh.exception.GroupIdNotFoundException;
import com.pushwoosh.exception.NotificationIdNotFoundException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;

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

    @Test
    public void getGroup_existingNotificationId_returnsGroup() throws Exception {
        storage.put("group-X", 777);

        assertEquals("group-X", storage.getGroup(777));
    }

    @Test
    public void getGroup_unknownNotificationId_throwsNotificationIdNotFoundException() {
        try {
            storage.getGroup(12345);
            fail("Expected NotificationIdNotFoundException");
        } catch (NotificationIdNotFoundException expected) {
            // ok
        }
    }

    @Test
    public void getNotificationId_unknownGroupId_throwsGroupIdNotFoundException() {
        try {
            storage.getNotificationId("missing-group");
            fail("Expected GroupIdNotFoundException");
        } catch (GroupIdNotFoundException expected) {
            // ok
        }
    }

    @Test
    public void update_replacesAllEntries() throws Exception {
        storage.put("old-A", 1);
        storage.put("old-B", 2);

        storage.update(Arrays.asList(new Pair<>("new-A", 10), new Pair<>("new-B", 20)));

        assertEquals(10, storage.getNotificationId("new-A"));
        assertEquals(20, storage.getNotificationId("new-B"));
        try {
            storage.getNotificationId("old-A");
            fail("Old entry old-A should be removed");
        } catch (GroupIdNotFoundException expected) {
            // ok
        }
        try {
            storage.getNotificationId("old-B");
            fail("Old entry old-B should be removed");
        } catch (GroupIdNotFoundException expected) {
            // ok
        }
    }

    @Test
    public void update_nullList_clearsTable() {
        storage.put("group-A", 1);

        storage.update(null);

        try {
            storage.getNotificationId("group-A");
            fail("Entry should be cleared by update(null)");
        } catch (GroupIdNotFoundException expected) {
            // ok
        }
    }

    @Test
    public void update_emptyList_clearsTable() {
        storage.put("group-A", 1);

        storage.update(Collections.emptyList());

        try {
            storage.getNotificationId("group-A");
            fail("Entry should be cleared by update(emptyList)");
        } catch (GroupIdNotFoundException expected) {
            // ok
        }
    }
}
