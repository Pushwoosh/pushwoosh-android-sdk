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

package com.pushwoosh.internal.prefs;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

public class TestSharedPreferences implements SharedPreferences {
	private final Map<String, Object> map = new HashMap<>();

	private TestEditor testEditor = new TestEditor(map);

	@Override
	public Map<String, ?> getAll() {
		return map;
	}

	@Nullable
	@Override
	public String getString(String key, @Nullable String defValue) {
		return getValue(key, defValue);
	}

	private <T> T getValue(String key, @Nullable T defValue) {
		if (!map.containsKey(key)) {
			return defValue;
		}
		final Object o = map.get(key);
		try {
			return (T) o;
		} catch (ClassCastException e) {

		}

		return defValue;
	}

	@Nullable
	@Override
	public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
		return getValue(key, defValues);
	}

	@Override
	public int getInt(String key, int defValue) {
		return getValue(key, defValue);
	}

	@Override
	public long getLong(String key, long defValue) {
		return getValue(key, defValue);
	}

	@Override
	public float getFloat(String key, float defValue) {
		return getValue(key, defValue);
	}

	@Override
	public boolean getBoolean(String key, boolean defValue) {
		return getValue(key, defValue);
	}

	@Override
	public boolean contains(String key) {
		return map.containsKey(key);
	}

	@Override
	public Editor edit() {
		return testEditor;
	}

	@Override
	public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

	}

	@Override
	public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {

	}

	private static class TestEditor implements Editor{
		private final Map<String, Object> map = new HashMap<>();
		private final Map<String, Object> sharedMap;

		private TestEditor(Map<String, Object> map) {
			this.sharedMap = map;
		}

		@Override
		public Editor putString(String key, @Nullable String value) {
			map.put(key, value);
			return this;
		}

		@Override
		public Editor putStringSet(String key, @Nullable Set<String> values) {
			map.put(key, values);
			return this;
		}

		@Override
		public Editor putInt(String key, int value) {
			map.put(key, value);
			return this;
		}

		@Override
		public Editor putLong(String key, long value) {
			map.put(key, value);
			return this;
		}

		@Override
		public Editor putFloat(String key, float value) {
			map.put(key, value);
			return this;
		}

		@Override
		public Editor putBoolean(String key, boolean value) {
			map.put(key, value);
			return this;
		}

		@Override
		public Editor remove(String key) {
			map.remove(key);
			return this;
		}

		@Override
		public Editor clear() {
			map.clear();
			return this;
		}

		@Override
		public boolean commit() {
			sharedMap.clear();
			sharedMap.putAll(map);
			return true;
		}

		@Override
		public void apply() {
			sharedMap.clear();
			sharedMap.putAll(map);
		}
	}
}
