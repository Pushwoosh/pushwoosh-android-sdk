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

package com.pushwoosh.internal.platform.prefs.migration;

import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.utils.PWLog;

import java.util.HashMap;
import java.util.Map;

/**
 * MigrationScheme to migrate from one {@link com.pushwoosh.internal.platform.prefs.PrefsProvider} to another
 * @see com.pushwoosh.repository.NotificationPrefs
 * @see com.pushwoosh.repository.RegistrationPrefs
 */
public class MigrationScheme {
	private static final String TAG = "MigrationScheme";

	private final String prefsName;
	private final Map<String, Object> scheme = new HashMap<>();

	public MigrationScheme(String prefsName) {
		this.prefsName = prefsName;
	}

	/**
	 * Add value to scheme from previous {@link com.pushwoosh.internal.platform.prefs.PrefsProvider}
	 * @param prefsProvider - previous prefs provider
	 * @param type - type for current key
	 * @param key - key into prefs
	 */
	public void put(PrefsProvider prefsProvider, AvailableType type, String key) {
		final SharedPreferences sharedPreferences = prefsProvider.providePrefs(prefsName);
		if (sharedPreferences == null) {
			return;
		}

		if (!sharedPreferences.contains(key)) {
			sharedPreferences.getAll();

			if (!sharedPreferences.contains(key)) {
				return;
			}
		}
		Object value;
		try {
			switch (type) {

				case STRING:
					value = sharedPreferences.getString(key, "");
					break;
				case BOOLEAN:
					value = sharedPreferences.getBoolean(key, false);
					break;
				case LONG:
					value = sharedPreferences.getLong(key, 0);
					break;
				case INT:
					value = sharedPreferences.getInt(key, 0);
					break;
				default:
					return;
			}

			scheme.put(key, value);
		} catch (Exception e) {
			PWLog.noise(TAG, "Failed providing data with key: " + key);
		}
	}

	public void putString(String key, String value) {
		scheme.put(key, value);
	}

	public void putBoolean(String key, boolean value) {
		scheme.put(key, value);
	}

	public void putLong(String key, long value) {
		scheme.put(key, value);
	}

	public void putInt(String key, int value) {
		scheme.put(key, value);
	}

	/**
	 * Implement scheme to new prefs provider
	 * @param prefsProvider - new prefs provider
	 */
	void implementScheme(PrefsProvider prefsProvider) {
		PWLog.noise(TAG, "Implement scheme with scheme: " + scheme);

		final SharedPreferences sharedPreferences = prefsProvider.providePrefs(prefsName);
		if (sharedPreferences == null) {
			PWLog.error("Incorrect state of the app preferences is null");
			return;
		}

		final SharedPreferences.Editor edit = sharedPreferences.edit();
		for (Map.Entry<String, Object> entry : scheme.entrySet()) {
			try {
				final String key = entry.getKey();
				final Object value = entry.getValue();
				if (value instanceof Boolean) {
					edit.putBoolean(key, (Boolean) value);
				} else if (value instanceof String) {
					edit.putString(key, (String) value);
				} else if (value instanceof Long) {
					edit.putLong(key, (Long) value);
				} else if (value instanceof Integer) {
					edit.putInt(key, (Integer) value);
				} else {
					PWLog.noise(TAG, "Unknown format for key: " + key);
				}
			} catch (Exception e) {
				PWLog.noise(TAG, "Failed put value to editor");
			}
		}
		edit.apply();
	}

	/**
	 * Types that can be added to prefs
	 */
	public enum AvailableType {
		STRING,
		BOOLEAN,
		LONG,
		INT
	}
}
