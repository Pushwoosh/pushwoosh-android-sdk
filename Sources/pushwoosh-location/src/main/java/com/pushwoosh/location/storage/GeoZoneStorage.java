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

package com.pushwoosh.location.storage;

import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.preference.PreferenceArrayListValue;
import com.pushwoosh.location.data.GeoZone;

import java.util.Collection;

/**
 * Used for {@link com.pushwoosh.location.geofence.GeofenceTracker} cache
 */
public class GeoZoneStorage {
	private static final String TAG = "GeoZoneStorage";
	public static final String KEY_NEAREST_GEO_ZONES = TAG + "KEY_NEAREST_GEO_ZONES";

	private final PreferenceArrayListValue<GeoZone> geoZonesPrefs;

	public GeoZoneStorage(PrefsProvider prefsProvider) {
		SharedPreferences sharedPreferences = prefsProvider.providePrefs(TAG);
		geoZonesPrefs = new PreferenceArrayListValue<>(sharedPreferences, KEY_NEAREST_GEO_ZONES, 10, GeoZone.class);
	}

	public void saveGeoZones(Collection<GeoZone> geoZones) {
		geoZonesPrefs.replaceAll(geoZones);
	}

	public Collection<GeoZone> getGeoZones() {
		return geoZonesPrefs.get();
	}
}
