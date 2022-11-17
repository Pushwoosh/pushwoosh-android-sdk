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
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;

/**
 * Date: 16.05.2017
 * Time: 14:45
 *
 * @author Savin Mikhail
 */
public class LocationPrefs {

	private static final String PREFERENCE = "com.pushwoosh.location";
	private static final String PROPERTY_GEOLOCATION_STARTED = "geolocation_started";

	private final PreferenceBooleanValue geolocationStarted;

	public LocationPrefs(PrefsProvider prefsProvider) {
		SharedPreferences preferences = prefsProvider.providePrefs(PREFERENCE);
		geolocationStarted = new PreferenceBooleanValue(preferences, PROPERTY_GEOLOCATION_STARTED, false);
	}

	public PreferenceBooleanValue geolocationStarted() {
		return geolocationStarted;
	}

	/**
	 * Create {@link com.pushwoosh.internal.platform.prefs.migration.MigrationScheme} associated with this class.
	 * Don't forget add field here if it will be added to this class
	 * @param prefsProvider - prefsProvider which will provide prefs for migrationScheme
	 * @return MigrationScheme to correct migration from one prefs to another
	 */
	public static MigrationScheme provideMigrationScheme(PrefsProvider prefsProvider) {
		MigrationScheme migrationScheme = new MigrationScheme(PREFERENCE);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.BOOLEAN, PROPERTY_GEOLOCATION_STARTED);
		return migrationScheme;
	}
}
