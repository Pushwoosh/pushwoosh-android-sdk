package com.pushwoosh.internal.preference;

import static junit.framework.Assert.assertNull;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

import android.text.TextUtils;

import com.pushwoosh.internal.prefs.TestSharedPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PreferenceArrayListValueTest<T extends Serializable> {
    private static final String GEO_ZONE_ARRAY_LIST_TEST_KEY = "geoZoneArrayListTestKey";
    private static final String ANOTHER_TEST_KEY = "anotherTestKey";

    @Test
    public void geoZoneArrayListTest() {
        SerializableGeoZone zone1 = new SerializableGeoZone("zone1", "zone1", 10.123, 11.123);
        SerializableGeoZone zone2 = new SerializableGeoZone("zone2", "zone2", 12.123, 13.123);
        SerializableGeoZone zone3 = new SerializableGeoZone("zone3", "zone3", 14.123, 15.123);
        List<SerializableGeoZone> list = new ArrayList<>();
        list.add(zone1);
        list.add(zone2);
        list.add(zone3);

        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<SerializableGeoZone> dummyGeoZonePrefs = new PreferenceArrayListValue<>(
                sharedPreferences, GEO_ZONE_ARRAY_LIST_TEST_KEY, 10, SerializableGeoZone.class);
        dummyGeoZonePrefs.clear();
        dummyGeoZonePrefs.replaceAll(list);

        dummyGeoZonePrefs = new PreferenceArrayListValue<>(
                sharedPreferences, GEO_ZONE_ARRAY_LIST_TEST_KEY, 10, SerializableGeoZone.class);
        List<SerializableGeoZone> savedList = dummyGeoZonePrefs.get();

        SerializableGeoZone savedZone1 = savedList.get(0);
        SerializableGeoZone savedZone2 = savedList.get(1);
        SerializableGeoZone savedZone3 = savedList.get(2);

        checkDummyGeoZonesEquals(zone1, savedZone1);
        assertNull(savedZone1.getLocation());
        checkDummyGeoZonesEquals(zone2, savedZone2);
        assertNull(savedZone2.getLocation());
        checkDummyGeoZonesEquals(zone3, savedZone3);
        assertNull(savedZone3.getLocation());
    }

    private void checkDummyGeoZonesEquals(SerializableGeoZone z1, SerializableGeoZone z2) {
        assertTrue(TextUtils.equals(z1.getId(), z2.getId()));
        assertTrue(TextUtils.equals(z1.getName(), z2.getName()));
        assertEquals(z1.getLatitude(), z2.getLatitude());
        assertEquals(z1.getLongitude(), z2.getLongitude());
    }

    @Test
    public void getReturnsDefensiveCopy() {
        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<String> prefs =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 5, String.class);
        prefs.clear();
        prefs.add("a");

        ArrayList<String> snapshot = prefs.get();
        snapshot.add("b");
        snapshot.clear();

        List<String> fresh = prefs.get();
        assertEquals(1, fresh.size());
        assertTrue(TextUtils.equals("a", fresh.get(0)));
    }

    @Test
    public void addEvictsOldestWhenOverCapacity() {
        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<String> prefs =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 2, String.class);
        prefs.clear();
        prefs.add("a");
        prefs.add("b");
        prefs.add("c");

        PreferenceArrayListValue<String> reloaded =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 2, String.class);
        List<String> loaded = reloaded.get();

        assertEquals(2, loaded.size());
        assertTrue(TextUtils.equals("b", loaded.get(0)));
        assertTrue(TextUtils.equals("c", loaded.get(1)));
    }

    // Restored from cross-check: covers the persistence half of remove(). If save() is dropped from remove()
    // (symmetric to clear/add), the element disappears in memory but reappears after reload.
    @Test
    public void removeExistingElementPersistsAfterReload() {
        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<String> prefs =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 5, String.class);
        prefs.clear();
        prefs.add("a");
        prefs.add("b");
        prefs.add("c");

        prefs.remove("b");

        PreferenceArrayListValue<String> reloaded =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 5, String.class);
        List<String> persisted = reloaded.get();

        assertEquals(2, persisted.size());
        assertTrue(TextUtils.equals("a", persisted.get(0)));
        assertTrue(TextUtils.equals("c", persisted.get(1)));
    }

    @Test
    public void replaceAllIgnoresCapacity() {
        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<String> prefs =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 2, String.class);
        prefs.clear();
        prefs.replaceAll(Arrays.asList("a", "b", "c", "d"));

        PreferenceArrayListValue<String> reloaded =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 2, String.class);
        List<String> loaded = reloaded.get();

        assertEquals(4, loaded.size());
        assertTrue(TextUtils.equals("a", loaded.get(0)));
        assertTrue(TextUtils.equals("b", loaded.get(1)));
        assertTrue(TextUtils.equals("c", loaded.get(2)));
        assertTrue(TextUtils.equals("d", loaded.get(3)));
    }
}
