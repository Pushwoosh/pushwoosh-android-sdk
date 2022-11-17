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

package com.pushwoosh.thirdpart.com.ironz.binaryprefs.dump;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.pushwoosh.thirdpart.com.ironz.binaryprefs.Preferences;

@SuppressWarnings("unused")
public final class DumpReceiver extends BroadcastReceiver {

	private static final String PREF_NAME = "pref_name";
	private static final String PREF_KEY = "pref_key";

	private static final Map<String, Preferences> BINARY_PREFERENCES_HASH_MAP = new ConcurrentHashMap<>();

	@Override
	public void onReceive(Context context, Intent intent) {

		String prefName = intent.getStringExtra(PREF_NAME);

		if (!BINARY_PREFERENCES_HASH_MAP.containsKey(prefName)) {
			Log.e(DumpReceiver.class.getName(), String.format("Cannot find '%s' preference for dumping!", prefName));
			return;
		}

		Preferences preferences = BINARY_PREFERENCES_HASH_MAP.get(prefName);
		Map<String, ?> all = preferences.getAll();

		if (intent.hasExtra(PREF_KEY)) {
			String key = intent.getStringExtra(PREF_KEY);
			Log.d(DumpReceiver.class.getName(), key + ": " + all.get(key) + "\n");
			return;
		}

		for (String key : all.keySet()) {
			Object o = all.get(key);
			Log.d(DumpReceiver.class.getName(), key + ": " + o + "\n");
		}
	}

	public static void register(String name, Preferences preferences) {
		BINARY_PREFERENCES_HASH_MAP.put(name, preferences);
	}

	public static void unregister(String name) {
		BINARY_PREFERENCES_HASH_MAP.remove(name);
	}
}