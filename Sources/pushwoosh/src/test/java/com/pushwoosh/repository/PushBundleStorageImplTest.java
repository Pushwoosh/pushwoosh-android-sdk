package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Schema-migration coverage for {@link PushBundleStorageImpl}. CRUD and UNIQUE INDEX dedup
 * semantics live in {@link PushBundleStorageTest} — this file isolates the SQLiteOpenHelper
 * overrides (onUpgrade / onDowngrade) which require the concrete type.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PushBundleStorageImplTest {

    private PushBundleStorageImpl storage;

    @Before
    public void setUp() {
        Application application = RuntimeEnvironment.application;
        storage = new PushBundleStorageImpl(application);
    }

    @After
    public void tearDown() {
        if (storage != null) {
            storage.close();
        }
    }

    private Bundle minimalBundle(String tag, String header) {
        Bundle b = new Bundle();
        b.putString("pw_msg", "msg");
        b.putString("header", header);
        if (tag != null) {
            b.putString("pw_msg_tag", tag);
        }
        return b;
    }

    @Test
    public void onUpgrade_dropsAndRecreatesBothTables_wipingExistingRows() throws Exception {
        long singleId = storage.putPushBundle(minimalBundle(null, "single"));
        storage.putGroupPushBundle(minimalBundle("tag-A", "group"), 1, "g");
        assertEquals(1, storage.getGroupPushBundles().size());

        SQLiteDatabase db = storage.getWritableDatabase();
        storage.onUpgrade(db, 5, 6);

        assertEquals(0, storage.getGroupPushBundles().size());
        final long missingId = singleId;
        assertThrows(Exception.class, () -> storage.getPushBundle(missingId));

        long freshRowId = storage.putGroupPushBundle(minimalBundle("tag-B", "fresh"), 2, "g2");
        assertTrue(freshRowId > 0);
    }

    @Test
    public void onUpgrade_preservesUniqueIndexDedupOnGroupPushBundles() throws Exception {
        SQLiteDatabase db = storage.getWritableDatabase();
        storage.onUpgrade(db, 5, 6);

        storage.putGroupPushBundle(minimalBundle("tag-A", "first"), 0, "g");
        storage.putGroupPushBundle(minimalBundle("tag-A", "second"), 0, "g");

        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(1, rows.size());
        assertEquals("second", rows.get(0).getString("header"));
    }

    @Test
    public void onDowngrade_preservesExistingDataUnlikeOnUpgrade() throws Exception {
        storage.putGroupPushBundle(minimalBundle("tag-A", "kept"), 1, "g");
        assertEquals(1, storage.getGroupPushBundles().size());

        SQLiteDatabase db = storage.getWritableDatabase();
        storage.onDowngrade(db, 6, 4);

        // Guard against someone "fixing" onDowngrade to call onUpgrade — rows would be wiped.
        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(1, rows.size());
        assertEquals("kept", rows.get(0).getString("header"));
    }
}
