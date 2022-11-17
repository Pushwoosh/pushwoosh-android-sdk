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

package com.pushwoosh.internal.platform.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.prefs.migration.DefaultPrefsMigration;
import com.pushwoosh.internal.platform.prefs.migration.PrefsMigration;
import com.pushwoosh.internal.utils.PWLog;

public class PrefsFactory {
	private static final String TAG = "PrefsFactory";
	private static final String PREFS_NAME = "com.pushwoosh.migration";
	private static final String KEY_LAST_VERSION = "lastVersion";
	private static final int VERSION = 3;

	private static volatile PrefsFactory instance;

	private static void init() {
		synchronized (PrefsFactory.class) {
			if (instance == null) {
				instance = new PrefsFactory();
			}
		}
	}

	private int lastVersion;

	private PrefsFactory() {
		Context applicationContext = AndroidPlatformModule.getApplicationContext();
		SharedPreferences sharedPreferences = applicationContext == null ? null : applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		lastVersion = sharedPreferences == null ? VERSION : sharedPreferences.getInt(KEY_LAST_VERSION, 1);

		if (sharedPreferences != null) {
			sharedPreferences.edit()
					.putInt(KEY_LAST_VERSION, VERSION)
					.apply();
		}

		PWLog.noise("PrefsFactory created. LastVersion: " + lastVersion + "; CurrentVersion: " + VERSION);
	}

	private PrefsProvider prevPrefsProvider() {
		return providePrefsProvider(lastVersion);
	}

	/**
	 * return prefs provider which connected with specific version of prefsFactory version.
	 * It depending on history evolution of Pushwoosh library. Don't change existed values
	 *
	 * @param version prefs factory version
	 * @return prefs provider
	 */
	private PrefsProvider providePrefsProvider(int version) {
		Context applicationContext = AndroidPlatformModule.getApplicationContext();
		if (version == 1) {
			return new ContextPrefsProvider(applicationContext);
		} else if (version == 2) {
			return new BinaryPrefsProvider(applicationContext);
		} else if (version == 3) {
			return new ContextPrefsProvider(applicationContext);
		}

		PWLog.noise(TAG, "Unknown version: " + version);
		return null;
	}

	@SuppressWarnings("ConstantConditions")
	private PrefsMigration providePrefsMigration() {
		if (VERSION == 2 && lastVersion == 1) {
			PWLog.noise("Try to create prefsMigration: DefaultPrefsMigration");
			return new DefaultPrefsMigration(providePrefsProvider(VERSION));
		}

		if (VERSION == 3 && lastVersion == 2) {
			return new DefaultPrefsMigration(providePrefsProvider(VERSION));
		}

		return null;
	}

	@Nullable
	public static PrefsMigration createPrefsMigration() {
		if (instance == null) {
			init();
		}

		return instance.providePrefsMigration();
	}

	public static PrefsProvider createPrefsProvider() {
		if (instance == null) {
			init();
		}
		return instance.providePrefsProvider(VERSION);
	}

	public static PrefsProvider getPrevPrefsProvider() {
		if (instance == null) {
			return null;
		}

		return instance.prevPrefsProvider();
	}
}
