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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.migration;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.Preferences;
import com.pushwoosh.thirdpart.com.ironz.binaryprefs.PreferencesEditor;

import java.util.*;

/**
 * Migrates all {@link SharedPreferences} instances into {@link Preferences}.
 */
public final class MigrateProcessor {

	private final List<SharedPreferences> migrate = new ArrayList<>();

	public void add(SharedPreferences preferences) {
		migrate.add(preferences);
	}

	public void migrateTo(Preferences preferences) {
		for (SharedPreferences sharedPreferences : migrate) {
			applyOne(sharedPreferences, preferences);
		}
	}

	@SuppressLint("ApplySharedPref")
	private void applyOne(SharedPreferences from, Preferences to) {

		Map<String, ?> all = from.getAll();

		if (all.isEmpty()) {
			return;
		}

		PreferencesEditor editor = to.edit();

		for (String key : all.keySet()) {
			migrateValue(all, editor, key);
		}
		boolean commit = editor.commit();

		if (commit) {
			from.edit().clear().commit();
		}
	}

	private void migrateValue(Map<String, ?> all, PreferencesEditor editor, String key) {
		Object value = all.get(key);
		if (value instanceof String) {
			editor.putString(key, (String) value);
		}
		if (value instanceof Set) {
			//noinspection unchecked
			editor.putStringSet(key, (Set<String>) value);
		}
		if (value instanceof Integer) {
			editor.putInt(key, (Integer) value);
		}
		if (value instanceof Long) {
			editor.putLong(key, (Long) value);
		}
		if (value instanceof Float) {
			editor.putFloat(key, (Float) value);
		}
		if (value instanceof Boolean) {
			editor.putBoolean(key, (Boolean) value);
		}
	}
}