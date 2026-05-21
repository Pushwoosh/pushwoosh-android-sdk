package com.pushwoosh.internal.preference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.pushwoosh.internal.prefs.TestSharedPreferences;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PreferenceJsonObjectValueTest {
    private static final String KEY = "k";

    // Verifies that set() persists the JSON to prefs and updates the cached value.
    @Test
    public void setPersistsValueAndUpdatesCache() throws Exception {
        TestSharedPreferences prefs = new TestSharedPreferences();
        PreferenceJsonObjectValue value = new PreferenceJsonObjectValue(prefs, KEY);

        JSONObject input = new JSONObject();
        input.put("a", 1);
        input.put("b", "x");

        value.set(input);

        JSONObject cached = value.get();
        assertNotNull(cached);
        assertEquals(1, cached.getInt("a"));
        assertEquals("x", cached.getString("b"));

        String serialized = prefs.getString(KEY, null);
        assertNotNull(serialized);
        JSONObject roundTrip = new JSONObject(serialized);
        assertEquals(1, roundTrip.getInt("a"));
        assertEquals("x", roundTrip.getString("b"));
    }

    // Verifies that set() stores a deep clone so later mutations of the input do not affect cached value.
    @Test
    public void setStoresDeepCloneNotInputReference() throws Exception {
        TestSharedPreferences prefs = new TestSharedPreferences();
        PreferenceJsonObjectValue value = new PreferenceJsonObjectValue(prefs, KEY);

        JSONObject input = new JSONObject();
        input.put("a", 1);

        value.set(input);

        input.put("b", 2);

        JSONObject cached = value.get();
        assertNotNull(cached);
        assertFalse(cached.has("b"));
        assertEquals(1, cached.getInt("a"));

        JSONObject persisted = new JSONObject(prefs.getString(KEY, "{}"));
        assertFalse(persisted.has("b"));
    }

    // Verifies that set(null) clears the cached value.
    @Test
    public void setNullClearsCachedValue() throws Exception {
        TestSharedPreferences prefs = new TestSharedPreferences();
        PreferenceJsonObjectValue value = new PreferenceJsonObjectValue(prefs, KEY);

        JSONObject input = new JSONObject();
        input.put("a", 1);
        value.set(input);

        value.set(null);

        assertNull(value.get());
    }

    // Verifies that set() normalizes an empty JSONObject (names()==null) into a fresh empty JSONObject and persists
    // "{}".
    @Test
    public void setEmptyJsonObjectNormalizesToEmptyAndPersistsBraces() {
        TestSharedPreferences prefs = new TestSharedPreferences();
        PreferenceJsonObjectValue value = new PreferenceJsonObjectValue(prefs, KEY);

        value.set(new JSONObject());

        JSONObject cached = value.get();
        assertNotNull(cached);
        assertEquals(0, cached.length());
        assertEquals("{}", prefs.getString(KEY, null));
    }

    // Verifies that merge() unions keys with last-write-wins semantics on top of an existing value.
    @Test
    public void mergeOverExistingValueUnionsKeysLastWriteWins() throws Exception {
        TestSharedPreferences prefs = new TestSharedPreferences();
        PreferenceJsonObjectValue value = new PreferenceJsonObjectValue(prefs, KEY);

        JSONObject base = new JSONObject();
        base.put("a", 1);
        base.put("b", 2);
        value.set(base);

        JSONObject delta = new JSONObject();
        delta.put("b", 99);
        delta.put("c", 3);
        value.merge(delta);

        JSONObject cached = value.get();
        assertNotNull(cached);
        assertEquals(3, cached.length());
        assertEquals(1, cached.getInt("a"));
        assertEquals(99, cached.getInt("b"));
        assertEquals(3, cached.getInt("c"));

        JSONObject persisted = new JSONObject(prefs.getString(KEY, "{}"));
        assertEquals(3, persisted.length());
        assertEquals(1, persisted.getInt("a"));
        assertEquals(99, persisted.getInt("b"));
        assertEquals(3, persisted.getInt("c"));
    }

    // Verifies that merge() starts from an empty JSONObject when current value is null and is not a no-op.
    @Test
    public void mergeWhenCurrentValueIsNullStartsFromEmpty() throws Exception {
        TestSharedPreferences prefs = new TestSharedPreferences();
        PreferenceJsonObjectValue value = new PreferenceJsonObjectValue(prefs, KEY);
        assertNull(value.get());

        JSONObject delta = new JSONObject();
        delta.put("x", 1);
        value.merge(delta);

        JSONObject cached = value.get();
        assertNotNull(cached);
        assertEquals(1, cached.length());
        assertEquals(1, cached.getInt("x"));

        JSONObject persisted = new JSONObject(prefs.getString(KEY, "{}"));
        assertTrue(persisted.has("x"));
        assertEquals(1, persisted.getInt("x"));
    }
}
