package com.pushwoosh.internal.preference;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.pushwoosh.internal.prefs.TestSharedPreferences;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class PreferenceArrayListValueTest <T extends Serializable> {
    private static final String ADD_NON_SERIALIZABLE_OBJECT_TEST_KEY = "addNonSerializableObjectTestKey";
    private static final String GEO_ZONE_ARRAY_LIST_TEST_KEY = "geoZoneArrayListTestKey";
    private static final String ANOTHER_TEST_KEY = "anotherTestKey";

    @Test
    public void geoZoneArrayListTest() {
        SerializableGeoZone zone1 = new SerializableGeoZone(
                "zone1",
                "zone1",
                10.123,
                11.123);
        SerializableGeoZone zone2 = new SerializableGeoZone(
                "zone2",
                "zone2",
                12.123,
                13.123);
        SerializableGeoZone zone3 = new SerializableGeoZone(
                "zone3",
                "zone3",
                14.123,
                15.123);
        List<SerializableGeoZone> list = new ArrayList<>();
        list.add(zone1);
        list.add(zone2);
        list.add(zone3);

        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<SerializableGeoZone> dummyGeoZonePrefs =
                new PreferenceArrayListValue<>(sharedPreferences, GEO_ZONE_ARRAY_LIST_TEST_KEY, 10,
                        SerializableGeoZone.class);
        dummyGeoZonePrefs.clear();
        dummyGeoZonePrefs.replaceAll(list);

        dummyGeoZonePrefs =
                new PreferenceArrayListValue<>(sharedPreferences, GEO_ZONE_ARRAY_LIST_TEST_KEY, 10,
                        SerializableGeoZone.class);
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
    public void addNonSerializableObjectTest() {
        NonSerializableGeoZone zone = new NonSerializableGeoZone(
                "zone1",
                "zone1",
                10.123,
                11.123);
        ArrayList<NonSerializableGeoZone> list = new ArrayList<>();
        list.add(zone);
        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<ArrayList> dummyGeoZonePrefs =
                new PreferenceArrayListValue<>(sharedPreferences, ADD_NON_SERIALIZABLE_OBJECT_TEST_KEY, 1, ArrayList.class);
        dummyGeoZonePrefs.clear();
        dummyGeoZonePrefs.add(list);

        dummyGeoZonePrefs =
                new PreferenceArrayListValue<>(sharedPreferences, ADD_NON_SERIALIZABLE_OBJECT_TEST_KEY, 1, ArrayList.class);
        List savedList = dummyGeoZonePrefs.get();

        assertEquals(savedList.size(), 0);
    }

    @Test
    public void testConstructorException() {
        SharedPreferences sharedPreferences = Mockito.mock(SharedPreferences.class);
        Mockito.when(sharedPreferences.getString(Mockito.anyString(), eq(null)))
                .thenThrow(new ClassCastException());

        PreferenceArrayListValue<ArrayList> dummyGeoZonePrefs =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 1, ArrayList.class);
        Object result = dummyGeoZonePrefs.get();

        assertSame(result.getClass(), ArrayList.class);
    }

    @Test
    public void passNullSharedPreferences() {
        PreferenceArrayListValue<String> prefs =
                new PreferenceArrayListValue<>(null, ANOTHER_TEST_KEY, 1, String.class);
        prefs.clear();

        String someString = "some string";
        prefs.add(someString);

        prefs = new PreferenceArrayListValue<>(null, ANOTHER_TEST_KEY, 1, String.class);
        List<String> savedStrings = prefs.get();

        assertEquals(savedStrings.size(), 0);
    }

    @Test
    public void testCapacity() {
        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<String> prefs =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 1, String.class);
        prefs.clear();

        prefs.add("first string");
        String secondString = "second string";
        prefs.add(secondString);

        prefs = new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 1, String.class);
        List<String> savedStrings = prefs.get();

        assertEquals(savedStrings.size(), 1);
        assertTrue(TextUtils.equals(savedStrings.get(0), secondString));
    }

    @Test
    public void testRemove() {
        TestSharedPreferences sharedPreferences = new TestSharedPreferences();
        PreferenceArrayListValue<String> prefs =
                new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 3, String.class);
        prefs.clear();

        String firstString = "first string";
        String secondString = "second string";
        String thirdString = "third string";
        prefs.add(firstString);
        prefs.add(secondString);
        prefs.add(thirdString);

        prefs = new PreferenceArrayListValue<>(sharedPreferences, ANOTHER_TEST_KEY, 3, String.class);

        prefs.remove(secondString);

        ArrayList<String> list = prefs.get();

        assertEquals(list.size(), 2);
        assertTrue(TextUtils.equals(list.get(0), firstString));
        assertTrue(TextUtils.equals(list.get(1), thirdString));
    }
}
