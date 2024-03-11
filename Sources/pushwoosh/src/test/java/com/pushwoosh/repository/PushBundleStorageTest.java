package com.pushwoosh.repository;

import android.app.Application;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
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
        pushBundle.putLongArray("long_array.test", new long[]{1573565995876L, 1573565995876L, 1573565995876L});
        pushBundle.putDoubleArray("double_array.test", new double[]{Double.MAX_VALUE, Double.MAX_EXPONENT, Double.MIN_VALUE, Double.MIN_NORMAL, Double.MIN_EXPONENT});
        pushBundle.putByteArray("byte_array.test", new byte[]{Byte.MAX_VALUE, Byte.MIN_VALUE});
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
        assertEquals(pushBundle.get("double_array.test"), Arrays.toString((double[]) getTestBundle().get("double_array.test"))); // arrays doesn't convert back from string
        assertEquals(pushBundle.get("byte_array.test"), Arrays.toString((byte[]) getTestBundle().get("byte_array.test"))); // arrays doesn't convert back from string
        assertEquals(pushBundle.get("long_array.test"), Arrays.toString((long[]) getTestBundle().get("long_array.test"))); // arrays doesn't convert back from string
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
}
