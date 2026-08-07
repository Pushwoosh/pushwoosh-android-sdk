package com.pushwoosh.inapp.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.os.Build;

import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceJsonObjectValue;
import com.pushwoosh.repository.NotificationPrefs;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class InAppTagsTest {

    private NotificationPrefs prefs;
    private PreferenceJsonObjectValue tagsPref;
    private PreferenceBooleanValue osVersionAllowed;
    private PreferenceBooleanValue deviceModelAllowed;

    @Before
    public void setUp() {
        prefs = mock(NotificationPrefs.class);
        tagsPref = mock(PreferenceJsonObjectValue.class);
        osVersionAllowed = mock(PreferenceBooleanValue.class);
        deviceModelAllowed = mock(PreferenceBooleanValue.class);
        when(prefs.tags()).thenReturn(tagsPref);
        when(prefs.isCollectingDeviceOsVersionAllowed()).thenReturn(osVersionAllowed);
        when(prefs.isCollectingDeviceModelAllowed()).thenReturn(deviceModelAllowed);
    }

    private void givenCache(String json) throws Exception {
        when(tagsPref.get()).thenReturn(json == null ? null : new JSONObject(json));
    }

    @Test
    public void collect_cachedTags_valuesAreStringified() throws Exception {
        givenCache("{\"UserName\":\"Alexey\",\"Timezone\":25200,\"Premium\":true}");

        Map<String, String> tags = InAppTags.collect(prefs);

        assertEquals("Alexey", tags.get("UserName"));
        // toString() coercion, same as Rich Media: the substitution engine only speaks String.
        assertEquals("25200", tags.get("Timezone"));
        assertEquals("true", tags.get("Premium"));
    }

    @Test
    public void collect_nullValueInCache_isDropped() throws Exception {
        givenCache("{\"UserName\":null,\"City\":\"Berlin\"}");

        Map<String, String> tags = InAppTags.collect(prefs);

        assertFalse(tags.containsKey("UserName"));
        assertEquals("Berlin", tags.get("City"));
    }

    @Test
    public void collect_emptyCache_returnsEmptyMap() throws Exception {
        givenCache("{}");

        assertTrue(InAppTags.collect(prefs).isEmpty());
    }

    @Test
    public void collect_nullCache_returnsEmptyMap() throws Exception {
        givenCache(null);

        assertTrue(InAppTags.collect(prefs).isEmpty());
    }

    @Test
    public void collect_prefsThrow_returnsEmptyMapWithoutThrowing() {
        when(tagsPref.get()).thenThrow(new RuntimeException("broken cache"));

        // Must not escape: a broken tag cache means placeholder defaults, never a dropped in-app.
        assertTrue(InAppTags.collect(prefs).isEmpty());
    }

    @Test
    public void collect_nullPrefs_returnsEmptyMap() {
        assertTrue(InAppTags.collect(null).isEmpty());
    }

    @Test
    public void collect_privacyFlagsEnabled_addsDeviceTags() throws Exception {
        givenCache("{}");
        when(osVersionAllowed.get()).thenReturn(true);
        when(deviceModelAllowed.get()).thenReturn(true);

        Map<String, String> tags = InAppTags.collect(prefs);

        assertEquals(Build.VERSION.RELEASE, tags.get("OS Version"));
        assertEquals(DeviceUtils.getDeviceName(), tags.get("Device Model"));
    }

    @Test
    public void collect_privacyFlagsDisabled_omitsDeviceTags() throws Exception {
        givenCache("{}");
        when(osVersionAllowed.get()).thenReturn(false);
        when(deviceModelAllowed.get()).thenReturn(false);

        Map<String, String> tags = InAppTags.collect(prefs);

        assertFalse(tags.containsKey("OS Version"));
        assertFalse(tags.containsKey("Device Model"));
    }

    // Verifies that each privacy flag gates its OWN key: with only OS Version allowed, Device Model stays
    // out. The both-on / both-off pair cannot see a flag-to-key swap; this asymmetric case can.
    @Test
    public void collect_onlyOsVersionAllowed_addsOsVersionWithoutDeviceModel() throws Exception {
        givenCache("{}");
        when(osVersionAllowed.get()).thenReturn(true);
        when(deviceModelAllowed.get()).thenReturn(false);

        Map<String, String> tags = InAppTags.collect(prefs);

        assertEquals(Build.VERSION.RELEASE, tags.get("OS Version"));
        assertFalse(tags.containsKey("Device Model"));
    }

    @Test
    public void collect_geoTags_areNormalized() throws Exception {
        givenCache("{\"Country\":\"in\",\"City\":\"in, trivandrum\"}");

        Map<String, String> tags = InAppTags.collect(prefs);

        assertEquals("India", tags.get("Country"));
        assertEquals("trivandrum", tags.get("City"));
    }

    @Test
    public void collect_unknownCountryCode_isDropped() throws Exception {
        givenCache("{\"Country\":\"zz\"}");

        assertFalse(InAppTags.collect(prefs).containsKey("Country"));
    }
}
