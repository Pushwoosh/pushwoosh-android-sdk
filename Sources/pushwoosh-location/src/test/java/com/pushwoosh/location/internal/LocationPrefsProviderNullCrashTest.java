/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.location.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.location.storage.GeoZoneStorage;
import com.pushwoosh.location.storage.LocationPrefs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

/**
 * Regression guard for crash-locationmodule-prefsprovider-null-static-init: a null {@code prefsProvider}
 * (pre-/failed-init {@code AndroidPlatformModule.getPrefsProvider()}) used to NPE in the {@code GeoZoneStorage}
 * / {@code LocationPrefs} ctors. The fix null-guards the receiver and degrades to a default-valued storage
 * (empty zones / {@code geolocationStarted == false}); these tests assert that graceful behavior at both
 * ctors, including via the real {@code LocationModule:101} / {@code :285} expressions.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class LocationPrefsProviderNullCrashTest {

    // ---------------------------------------------------------------------------------------------
    // GeoZoneStorage — null prefsProvider degrades to an empty-but-usable storage (was NPE at :47).
    // ---------------------------------------------------------------------------------------------

    // Verifies that GeoZoneStorage(null) constructs gracefully and exposes an empty geo-zone set.
    @Test
    public void geoZoneStorage_nullPrefsProvider_constructsWithEmptyZones() {
        GeoZoneStorage storage = new GeoZoneStorage(null);
        assertNotNull("null prefsProvider must not crash GeoZoneStorage ctor", storage);
        assertNotNull("zones must default to a non-null (empty) collection", storage.getGeoZones());
        assertTrue(
                "zones must be empty when there is no backing prefs",
                storage.getGeoZones().isEmpty());
    }

    // ---------------------------------------------------------------------------------------------
    // LocationPrefs — null prefsProvider degrades to the default flag value (was NPE at :49).
    // ---------------------------------------------------------------------------------------------

    // Verifies that LocationPrefs(null) constructs gracefully and reports the default flag value.
    @Test
    public void locationPrefs_nullPrefsProvider_constructsWithDefaultFlag() {
        LocationPrefs prefs = new LocationPrefs(null);
        assertNotNull("null prefsProvider must not crash LocationPrefs ctor", prefs);
        assertFalse(
                "geolocationStarted must fall back to its default (false) with no backing prefs",
                prefs.geolocationStarted().get());
    }

    // ---------------------------------------------------------------------------------------------
    // Reach guards: the null flows from the REAL getPrefsProvider() accessor into the ctor, exactly the
    // LocationModule:101 (GeoZoneStorage) and :285 (LocationPrefs) expressions.
    // ---------------------------------------------------------------------------------------------

    // Verifies that the LocationModule:101 expression no longer crashes when getPrefsProvider() is null.
    @Test
    public void reach_geoZoneStorage_viaNullGetPrefsProvider_isGraceful() {
        try (MockedStatic<AndroidPlatformModule> apm = mockStatic(AndroidPlatformModule.class)) {
            apm.when(AndroidPlatformModule::getPrefsProvider).thenReturn(null);
            // mirrors LocationModule.java:101
            GeoZoneStorage storage = new GeoZoneStorage(AndroidPlatformModule.getPrefsProvider());
            assertNotNull("LocationModule:101 expression must construct gracefully on null provider", storage);
            assertTrue(storage.getGeoZones().isEmpty());
        }
    }

    // Verifies that the LocationModule:285 expression no longer crashes when getPrefsProvider() is null.
    @Test
    public void reach_locationPrefs_viaNullGetPrefsProvider_isGraceful() {
        try (MockedStatic<AndroidPlatformModule> apm = mockStatic(AndroidPlatformModule.class)) {
            apm.when(AndroidPlatformModule::getPrefsProvider).thenReturn(null);
            // mirrors LocationModule.java:285 (locationPrefs())
            LocationPrefs prefs = new LocationPrefs(AndroidPlatformModule.getPrefsProvider());
            assertNotNull("LocationModule:285 expression must construct gracefully on null provider", prefs);
            assertFalse(prefs.geolocationStarted().get());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Discriminator — proves the guard is CONDITIONAL (`== null ? null : providePrefs()`), not an
    // unconditional null. A value persisted in a real backing prefs before construction must round-trip
    // back: if the fix degenerated to always passing null, this read would return the default and fail.
    // ---------------------------------------------------------------------------------------------

    // Verifies that a non-null provider is actually read (the guard does not bypass real prefs).
    @Test
    public void nonNullProvider_isStillRead_flagRoundTrips() {
        SharedPreferences sp = org.robolectric.RuntimeEnvironment.getApplication()
                .getSharedPreferences("test_location_prefs", android.content.Context.MODE_PRIVATE);
        sp.edit().putBoolean("geolocation_started", true).apply();

        PrefsProvider provider = Mockito.mock(PrefsProvider.class);
        when(provider.providePrefs(Mockito.anyString())).thenReturn(sp);

        LocationPrefs prefs = new LocationPrefs(provider);
        assertTrue(
                "a real provider must be read: the persisted geolocationStarted=true must round-trip",
                prefs.geolocationStarted().get());
    }
}
