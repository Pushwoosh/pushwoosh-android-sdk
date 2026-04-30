package com.pushwoosh.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.os.Bundle;

import com.pushwoosh.repository.util.PushBundleDatabaseEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PushBundleStorageTest {
    private PushBundleStorage storage;

    @Before
    public void setUp() throws Exception {
        Application application = RuntimeEnvironment.application;
        storage = new PushBundleStorageImpl(application);
    }

    private Bundle getTestBundle() {
        Bundle pushBundle = new Bundle();
        pushBundle.putString("silent", "1");
        pushBundle.putString("pw_silent", "0");
        pushBundle.putString("pw_msg", "test_msg");
        pushBundle.putString("pw_command", "test_command");
        pushBundle.putString("p", "test_hash");
        pushBundle.putString("md", "test_md");
        pushBundle.putBoolean("local", true);
        pushBundle.putString("ibc", "#112233");
        pushBundle.putString("led", "#112233");
        pushBundle.putString("vib", "1");
        pushBundle.putString("s", "0");
        pushBundle.putString("title", "test_msg");
        pushBundle.putString("header", "test_header");
        pushBundle.putString("pri", "test_pri");
        pushBundle.putString("visibility", "0");
        pushBundle.putString("pw_badges", "10");
        pushBundle.putString("pw_actions", "test_actions");
        pushBundle.putString("b", "http://www.test.com/test.jpg");
        pushBundle.putString("ci", "http://www.test.com/test.jpg");
        pushBundle.putString("i", "http://www.test.com/test.jpg");
        pushBundle.putInt("led_on_ms", 100);
        pushBundle.putString("led_off_ms", "10");
        pushBundle.putString("pw_msg_tag", "test_tag");
        pushBundle.putString("pw_lockscreen", "test");
        pushBundle.putString("u", "{'pw_channel' : 'test'}");
        pushBundle.putString("packs", "test");
        pushBundle.putString("value", "test_value");
        pushBundle.putString("rm", "test_rm");
        pushBundle.putString("l", "test_l");
        pushBundle.putString("pw_channel", "test_channel");
        pushBundle.putString("h", "test_h");
        pushBundle.putString("r", "test_r");
        pushBundle.putString("custom root param", "root param value");
        pushBundle.putLong("google.sent_time", 1573565995876L);
        pushBundle.putDouble("double.test", Double.MAX_VALUE);
        pushBundle.putLongArray("long_array.test", new long[] {1573565995876L, 1573565995876L, 1573565995876L});
        pushBundle.putDoubleArray("double_array.test", new double[] {
            Double.MAX_VALUE, Double.MAX_EXPONENT, Double.MIN_VALUE, Double.MIN_NORMAL, Double.MIN_EXPONENT
        });
        pushBundle.putByteArray("byte_array.test", new byte[] {Byte.MAX_VALUE, Byte.MIN_VALUE});
        return pushBundle;
    }

    @Test
    public void insertionTest() {
        long id;
        try {
            id = storage.putPushBundle(getTestBundle());
        } catch (Exception e) {
            id = -1;
        }
        assertTrue(id > 0);
    }

    @Test
    public void insertQueryTest() {
        Bundle pushBundle;
        try {
            long id = storage.putPushBundle(getTestBundle());
            pushBundle = storage.getPushBundle(id);
        } catch (Exception e) {
            pushBundle = null;
        }
        assertNotNull(pushBundle);
        assertEquals(pushBundle.getString("silent"), getTestBundle().getString("silent"));
        assertEquals(pushBundle.getString("pw_silent"), getTestBundle().getString("pw_silent"));
        assertEquals(pushBundle.getString("pw_msg"), getTestBundle().getString("pw_msg"));
        assertEquals(pushBundle.getString("pw_command"), getTestBundle().getString("pw_command"));
        assertEquals(pushBundle.getString("p"), getTestBundle().getString("p"));
        assertEquals(pushBundle.getString("md"), getTestBundle().getString("md"));
        assertEquals(pushBundle.getString("local"), getTestBundle().getString("local"));
        assertEquals(pushBundle.getString("ibc"), getTestBundle().getString("ibc"));
        assertEquals(pushBundle.getString("led"), getTestBundle().getString("led"));
        assertEquals(pushBundle.getString("vib"), getTestBundle().getString("vib"));
        assertEquals(pushBundle.getString("s"), getTestBundle().getString("s"));
        assertEquals(pushBundle.getString("title"), getTestBundle().getString("title"));
        assertEquals(pushBundle.getString("pri"), getTestBundle().getString("pri"));
        assertEquals(pushBundle.getString("visibility"), getTestBundle().getString("visibility"));
        assertEquals(pushBundle.getString("pw_badges"), getTestBundle().getString("pw_badges"));
        assertEquals(pushBundle.getString("pw_actions"), getTestBundle().getString("pw_actions"));
        assertEquals(pushBundle.getString("b"), getTestBundle().getString("b"));
        assertEquals(pushBundle.getString("ci"), getTestBundle().getString("ci"));
        assertEquals(pushBundle.getString("i"), getTestBundle().getString("i"));
        assertEquals(pushBundle.getInt("led_on_ms"), getTestBundle().getInt("led_on_ms"));
        assertEquals(pushBundle.getString("led_off_ms"), getTestBundle().getString("led_off_ms"));
        assertEquals(pushBundle.getString("pw_msg_tag"), getTestBundle().getString("pw_msg_tag"));
        assertEquals(pushBundle.getString("pw_lockscreen"), getTestBundle().getString("pw_lockscreen"));
        assertEquals(pushBundle.getString("u"), getTestBundle().getString("u"));
        assertEquals(pushBundle.getString("packs"), getTestBundle().getString("packs"));
        assertEquals(pushBundle.getString("value"), getTestBundle().getString("value"));
        assertEquals(pushBundle.getString("rm"), getTestBundle().getString("rm"));
        assertEquals(pushBundle.getString("l"), getTestBundle().getString("l"));
        assertEquals(pushBundle.getString("pw_channel"), getTestBundle().getString("pw_channel"));
        assertEquals(pushBundle.getString("h"), getTestBundle().getString("h"));
        assertEquals(pushBundle.getString("r"), getTestBundle().getString("r"));
        assertEquals(pushBundle.getString("custom root param"), getTestBundle().getString("custom root param"));
        assertEquals(pushBundle.getLong("google.sent_time"), getTestBundle().getLong("google.sent_time"));
        assertEquals(pushBundle.getDouble("double.test"), getTestBundle().getDouble("double.test"), 0);
        assertNotNull(pushBundle.get("double_array.test"));
        assertNotNull(pushBundle.get("byte_array.test"));
        assertNotNull(pushBundle.get("long_array.test"));
        assertEquals(pushBundle.get("double_array.test"), Arrays.toString((double[])
                getTestBundle().get("double_array.test"))); // arrays doesn't convert back from string
        assertEquals(pushBundle.get("byte_array.test"), Arrays.toString((byte[])
                getTestBundle().get("byte_array.test"))); // arrays doesn't convert back from string
        assertEquals(pushBundle.get("long_array.test"), Arrays.toString((long[])
                getTestBundle().get("long_array.test"))); // arrays doesn't convert back from string
    }

    @Test
    public void insertRemoveBundle() {
        long id;
        try {
            id = storage.putPushBundle(getTestBundle());
            storage.removePushBundle(id);
        } catch (Exception e) {
            id = -100;
        }

        assertNotEquals(id, -100);
    }

    @Test
    public void insertRemoveThenGetBundleWithException() {
        long id;
        try {
            id = storage.putPushBundle(getTestBundle());
            storage.removePushBundle(id);
            storage.removePushBundle(id);
            storage.getPushBundle(id);
        } catch (Exception e) {
            id = -100;
        }

        assertEquals(id, -100);
    }

    @Test
    public void getAllTest() {
        long firstId;
        long lastId;
        try {
            firstId = storage.putGroupPushBundle(getTestBundle(), 1004, "test_group");
            storage.putGroupPushBundle(getTestBundle(), 1005, "test_group");
            lastId = storage.putGroupPushBundle(getTestBundle(), 1006, "test_group");
        } catch (Exception e) {
            firstId = -100;
            lastId = -100;
        }
        List<Bundle> bundles = storage.getGroupPushBundles();
        assertEquals(3, bundles.size());
        storage.removeGroupPushBundle(lastId);
        bundles = storage.getGroupPushBundles();
        assertEquals(2, bundles.size());
        storage.removeGroupPushBundle(firstId);
        bundles = storage.getGroupPushBundles();
        assertEquals(1, bundles.size());
        storage.removeGroupPushBundles();
        bundles = storage.getGroupPushBundles();
        assertEquals(0, bundles.size());
    }

    private Bundle bundleWithTag(String tag, String header) {
        Bundle b = getTestBundle();
        if (tag == null) {
            b.remove("pw_msg_tag");
        } else {
            b.putString("pw_msg_tag", tag);
        }
        b.putString("header", header);
        return b;
    }

    @Test
    public void putGroupPushBundle_sameTagSameNotifId_replacesPreviousRow() throws Exception {
        storage.putGroupPushBundle(bundleWithTag("tag-A", "first"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag("tag-A", "second"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag("tag-A", "third"), 0, "g");

        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(1, rows.size());
        assertEquals("third", rows.get(0).getString("header"));
        assertEquals("tag-A", rows.get(0).getString("pw_msg_tag"));
    }

    @Test
    public void putGroupPushBundle_differentTags_keepsBothRows() throws Exception {
        storage.putGroupPushBundle(bundleWithTag("tag-A", "first"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag("tag-B", "second"), 0, "g");

        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(2, rows.size());
    }

    @Test
    public void putGroupPushBundle_nullTags_keepsAllRows() throws Exception {
        storage.putGroupPushBundle(bundleWithTag(null, "first"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag(null, "second"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag(null, "third"), 0, "g");

        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(3, rows.size());
        for (Bundle row : rows) {
            assertNull(row.getString("pw_msg_tag"));
        }
    }

    @Test
    public void getLastPushBundleEntryForGroup_returnsLatestAfterReplace() throws Exception {
        long firstRowId = storage.putGroupPushBundle(bundleWithTag("tag-A", "first"), 0, "g");
        long secondRowId = storage.putGroupPushBundle(bundleWithTag("tag-A", "second"), 0, "g");

        assertNotEquals(firstRowId, secondRowId);

        PushBundleDatabaseEntry entry = storage.getLastPushBundleEntryForGroup("g");
        assertEquals(secondRowId, entry.getRowId());
        assertEquals("second", entry.getPushBundle().getString("header"));
    }

    @Test
    public void putGroupPushBundle_sameTagDifferentGroups_keepsBothRows() throws Exception {
        storage.putGroupPushBundle(bundleWithTag("tag-A", "first"), 0, "group-1");
        storage.putGroupPushBundle(bundleWithTag("tag-A", "second"), 0, "group-2");

        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(2, rows.size());
    }

    @Test
    public void putGroupPushBundle_emptyStringTag_dedupsLikeAnyOtherValue() throws Exception {
        // Empty string is NOT NULL in SQLite — UNIQUE INDEX should treat "" == "".
        Bundle b1 = bundleWithTag("", "first");
        Bundle b2 = bundleWithTag("", "second");
        storage.putGroupPushBundle(b1, 0, "g");
        storage.putGroupPushBundle(b2, 0, "g");

        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(1, rows.size());
        assertEquals("second", rows.get(0).getString("header"));
    }

    @Test
    public void putGroupPushBundle_bundleWithoutTagKey_treatedAsNull() throws Exception {
        // Bundle that does not contain "pw_msg_tag" at all — getString returns null →
        // each row stays distinct (NULL distinct semantics in UNIQUE INDEX).
        Bundle b1 = getTestBundle();
        b1.remove("pw_msg_tag");
        b1.putString("header", "first");
        Bundle b2 = getTestBundle();
        b2.remove("pw_msg_tag");
        b2.putString("header", "second");

        storage.putGroupPushBundle(b1, 0, "g");
        storage.putGroupPushBundle(b2, 0, "g");

        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(2, rows.size());
    }

    @Test
    public void putGroupPushBundle_replaceUpdatesPayload() throws Exception {
        // After REPLACE the row content (push_bundle_json) reflects the latest push,
        // not the original — getLastPushBundleEntryForGroup returns the merged payload.
        storage.putGroupPushBundle(bundleWithTag("tag-X", "first"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag("tag-X", "second"), 0, "g");
        long lastRowId = storage.putGroupPushBundle(bundleWithTag("tag-X", "third"), 0, "g");

        PushBundleDatabaseEntry entry = storage.getLastPushBundleEntryForGroup("g");
        assertEquals(lastRowId, entry.getRowId());
        assertEquals("third", entry.getPushBundle().getString("header"));
    }

    @Test
    public void putGroupPushBundle_mixedTaggedAndUntagged_keepsExpectedRows() throws Exception {
        // Realistic scenario: 3 same-tag pushes + 2 untagged pushes + 1 different-tag push
        // → 1 (deduped) + 2 (distinct nulls) + 1 = 4 rows.
        storage.putGroupPushBundle(bundleWithTag("tag-S", "S1"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag("tag-S", "S2"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag("tag-S", "S3"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag(null, "U1"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag(null, "U2"), 0, "g");
        storage.putGroupPushBundle(bundleWithTag("tag-D", "D1"), 0, "g");

        List<Bundle> rows = storage.getGroupPushBundles();
        assertEquals(4, rows.size());
    }
}
