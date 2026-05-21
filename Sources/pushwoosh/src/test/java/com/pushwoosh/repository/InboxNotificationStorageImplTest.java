package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class InboxNotificationStorageImplTest {

    private InboxNotificationStorageImpl storage;

    @Before
    public void setUp() {
        Application application = RuntimeEnvironment.application;
        storage = new InboxNotificationStorageImpl(application);
    }

    @After
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    // Verifies that a put entry can be read back with the same notification id and tag.
    @Test
    public void putThenGet_returnsStoredNotificationIdAndTag() {
        storage.putNotificationIdAndTag("msg-1", 42, "tag-1");

        assertEquals(Integer.valueOf(42), storage.getNotificationId("msg-1"));
        assertEquals("tag-1", storage.getNotificationTag("msg-1"));
    }

    // Verifies that a duplicate inboxMessageId put does not replace the first-stored row (no UNIQUE constraint, so CONFLICT_REPLACE degrades to plain insert) and unrelated keys remain readable.
    @Test
    public void put_duplicateInboxMessageId_returnsFirstStoredPairAndKeepsOthers() {
        storage.putNotificationIdAndTag("msg-1", 1, "old");
        storage.putNotificationIdAndTag("msg-2", 9, "t9");
        storage.putNotificationIdAndTag("msg-1", 2, "new");

        assertEquals(Integer.valueOf(1), storage.getNotificationId("msg-1"));
        assertEquals("old", storage.getNotificationTag("msg-1"));
        assertEquals(Integer.valueOf(9), storage.getNotificationId("msg-2"));
        assertEquals("t9", storage.getNotificationTag("msg-2"));
    }

    // Verifies that lookups for an unknown inboxMessageId return null instead of throwing.
    @Test
    public void get_missingInboxMessageId_returnsNull() {
        storage.putNotificationIdAndTag("msg-1", 42, "tag-1");

        assertNull(storage.getNotificationId("missing"));
        assertNull(storage.getNotificationTag("missing"));
    }

    // Verifies that onUpgrade drops and recreates the table, wiping existing rows while allowing fresh inserts afterwards.
    @Test
    public void onUpgrade_dropsAndRecreatesTable_wipingExistingRows() {
        storage.putNotificationIdAndTag("msg-1", 42, "tag-1");
        SQLiteDatabase db = storage.getWritableDatabase();

        storage.onUpgrade(db, 1, 2);

        assertNull(storage.getNotificationId("msg-1"));
        assertNull(storage.getNotificationTag("msg-1"));

        storage.putNotificationIdAndTag("msg-2", 7, "fresh");
        assertEquals(Integer.valueOf(7), storage.getNotificationId("msg-2"));
        assertEquals("fresh", storage.getNotificationTag("msg-2"));
    }

    // Verifies that inboxMessageIds with quotes, spaces, and underscores are stored and looked up safely via parameterized selection.
    @Test
    public void putAndGet_inboxMessageIdWithSpecialCharacters_roundTrips() {
        String id = "msg with 'quotes' and _underscore";

        storage.putNotificationIdAndTag(id, 7, "tag");

        assertEquals(Integer.valueOf(7), storage.getNotificationId(id));
        assertEquals("tag", storage.getNotificationTag(id));
    }
}
