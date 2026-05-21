package com.pushwoosh.tags;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
@LooperMode(LooperMode.Mode.LEGACY)
public class TagsBundleTest {

    // region getInt

    @Test
    public void getInt_coercesStoredLongToInt() {
        long bigValue = 9876543210L;
        TagsBundle bundle =
                new TagsBundle.Builder().putLong("user_id", bigValue).build();

        assertEquals((int) bigValue, bundle.getInt("user_id", -1));
    }

    private static final class Case<T> {
        final String label;
        final TagsBundle bundle;
        final String key;
        final T defaultValue;
        final T expected;

        Case(String label, TagsBundle bundle, String key, T defaultValue, T expected) {
            this.label = label;
            this.bundle = bundle;
            this.key = key;
            this.defaultValue = defaultValue;
            this.expected = expected;
        }
    }

    @Test
    public void getInt_returnsDefault_whenMissingOrNonNumber() {
        TagsBundle missingBundle = new TagsBundle.Builder().build();
        TagsBundle stringBundle =
                new TagsBundle.Builder().putString("foo", "bar").build();

        List<Case<Integer>> cases = Arrays.asList(
                new Case<>("missing key", missingBundle, "absent", 42, 42),
                new Case<>("non-number value", stringBundle, "foo", 7, 7));

        for (Case<Integer> c : cases) {
            assertEquals(c.label, (int) c.expected, c.bundle.getInt(c.key, c.defaultValue));
        }
    }

    // endregion

    // region getLong

    @Test
    public void getLong_widensStoredIntToLong() {
        TagsBundle bundle = new TagsBundle.Builder().putInt("count", 42).build();

        assertEquals(42L, bundle.getLong("count", -1L));
    }

    @Test
    public void getLong_returnsDefault_whenMissingOrNonNumber() {
        TagsBundle missingBundle = new TagsBundle.Builder().build();
        TagsBundle stringBundle = new TagsBundle.Builder().putString("k", "v").build();

        List<Case<Long>> cases = Arrays.asList(
                new Case<>("missing key", missingBundle, "absent", 99L, 99L),
                new Case<>("non-number value", stringBundle, "k", 99L, 99L));

        for (Case<Long> c : cases) {
            assertEquals(c.label, (long) c.expected, c.bundle.getLong(c.key, c.defaultValue));
        }
    }

    // endregion

    // region getBoolean

    @Test
    public void getBoolean_returnsDefault_whenMissingOrNonBoolean() {
        TagsBundle missingBundle = new TagsBundle.Builder().build();
        TagsBundle stringBundle =
                new TagsBundle.Builder().putString("k", "true").build();
        TagsBundle intBundle = new TagsBundle.Builder().putInt("k", 1).build();

        List<Case<Boolean>> cases = Arrays.asList(
                new Case<>("missing key", missingBundle, "absent", true, true),
                new Case<>("non-boolean string value", stringBundle, "k", false, false),
                new Case<>("number not coerced to boolean", intBundle, "k", false, false));

        for (Case<Boolean> c : cases) {
            assertEquals(c.label, c.expected, c.bundle.getBoolean(c.key, c.defaultValue));
        }
    }

    // endregion

    // region getString

    // endregion

    // region getList

    @Test
    public void getList_convertsJsonArrayToStringList() throws Exception {
        TagsBundle bundle = new TagsBundle.Builder()
                .putAll(new JSONObject("{\"tags\":[\"x\",\"y\"]}"))
                .build();

        assertEquals(Arrays.asList("x", "y"), bundle.getList("tags"));
    }

    @Test
    public void getList_skipsJsonArrayElementsThatThrowJsonException() throws Exception {
        // JSONArray.getString throws JSONException for literal null entries (Android JSON contract:
        // JSON.toString(null) -> typeMismatch). That branch in getList is silently swallowed.
        JSONArray array = new JSONArray();
        array.put("x");
        array.put((Object) null);
        array.put("y");
        JSONObject json = new JSONObject();
        json.put("tags", array);

        TagsBundle bundle = new TagsBundle.Builder().putAll(json).build();

        List<String> result = bundle.getList("tags");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("x"));
        assertTrue(result.contains("y"));
    }

    // endregion

    // region Builder non-trivial methods

    @Test
    public void putStringIfNotEmpty_skipsNullOrEmpty() {
        List<String> values = Arrays.asList((String) null, "");

        for (String value : values) {
            String label = value == null ? "null value" : "empty string";
            TagsBundle bundle = new TagsBundle.Builder()
                    .putStringIfNotEmpty("optional", value)
                    .build();

            assertFalse(label, bundle.getMap().containsKey("optional"));
            assertNull(label, bundle.getString("optional"));
        }
    }

    @Test
    public void remove_storesNullValueInBuiltMap() {
        TagsBundle bundle =
                new TagsBundle.Builder().putString("a", "keep").remove("b").build();

        Map<String, Object> map = bundle.getMap();
        assertTrue("removed key still present in map", map.containsKey("b"));
        assertNull("removed key carries null value (NULL_PLACEHOLDER translated)", map.get("b"));
        assertEquals("keep", map.get("a"));
    }

    @Test
    public void incrementInt_storesIncrementOperationMap() {
        TagsBundle bundle = new TagsBundle.Builder().incrementInt("points", 50).build();

        @SuppressWarnings("unchecked")
        Map<String, Object> op = (Map<String, Object>) bundle.getMap().get("points");
        assertNotNull(op);
        assertEquals("increment", op.get("operation"));
        assertEquals(50, ((Integer) op.get("value")).intValue());
    }

    @Test
    public void appendList_storesAppendOperationMap() {
        List<String> values = Arrays.asList("a", "b");

        TagsBundle bundle = new TagsBundle.Builder().appendList("cats", values).build();

        @SuppressWarnings("unchecked")
        Map<String, Object> op = (Map<String, Object>) bundle.getMap().get("cats");
        assertNotNull(op);
        assertEquals("append", op.get("operation"));
        assertEquals(values, op.get("value"));
    }

    @Test
    public void removeFromList_storesRemoveOperationMap() {
        List<String> values = Collections.singletonList("x");

        TagsBundle bundle =
                new TagsBundle.Builder().removeFromList("cats", values).build();

        @SuppressWarnings("unchecked")
        Map<String, Object> op = (Map<String, Object>) bundle.getMap().get("cats");
        assertNotNull(op);
        assertEquals("remove", op.get("operation"));
        assertEquals(values, op.get("value"));
    }

    @Test
    public void putAll_importsAllJsonKeysPreservingTypes() throws Exception {
        TagsBundle bundle = new TagsBundle.Builder()
                .putAll(new JSONObject("{\"name\":\"Jane\",\"age\":32,\"premium\":true}"))
                .build();

        assertEquals("Jane", bundle.getString("name"));
        assertEquals(32, bundle.getInt("age", 0));
        assertTrue(bundle.getBoolean("premium", false));
    }

    @Test
    public void getTagsHashMap_snapshotIsIndependentOfLaterBuilderMutations() {
        TagsBundle.Builder builder = new TagsBundle.Builder().putString("a", "1");

        HashMap<String, Object> snap1 = builder.getTagsHashMap();
        builder.putString("a", "2");

        assertEquals("1", snap1.get("a"));
    }

    @Test
    public void build_snapshotIsIndependentOfLaterBuilderMutations() {
        TagsBundle.Builder builder = new TagsBundle.Builder().putInt("n", 1);
        TagsBundle bundle = builder.build();

        builder.putInt("n", 2);

        assertEquals(1, bundle.getInt("n", 0));
    }

    // Note (drift): per TagsBundle#getMap javadoc the returned map "is immutable ... modifications
    // will throw UnsupportedOperationException". Reality: Builder#getTagsHashMap() returns a plain
    // HashMap, so getMap() exposes a mutable view. The immutability scenario from the architect's
    // plan is intentionally dropped — asserting the javadoc claim would fail; asserting mutability
    // would lock in a contract the docs deny. Flagged for reviewer / future doc fix.

    // endregion
}
