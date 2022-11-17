/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.internal.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

/**
 * Created by kai on 02.02.2018.
 */

public class AppVersionProvider {
	public int getCurrentVersion() {
		Context context = AndroidPlatformModule.getApplicationContext();
		int version = 0;
		if (context == null)
			return version;
		try {
			version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return version;
	}

	private SharedPreferences getPrefs() {
		return prefs;
	}

	private Integer getLastLaunchVersion() {
		try {
			if (getPrefs().contains("LastLaunchVersion")) {
				return getPrefs().getInt("LastLaunchVersion", -1);
			} else {
				return null;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private void setLastLaunchVersion() {
		try {
			getPrefs().edit().putInt("LastLaunchVersion", getCurrentVersion()).apply();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private SharedPreferences prefs;
	private boolean firstLaunch = false;
	private boolean firstLaunchAfterUpdate = false;
	private boolean handleLaunch = false;
	private final Object handleLaunchLock = new Object();

	public AppVersionProvider(SharedPreferences prefs) {
		this.prefs = prefs;
	}

	public void handleLaunch() {
		synchronized (handleLaunchLock) {
			if (handleLaunch)
				return;
			Integer lastLaunchVersion = getLastLaunchVersion();
			if (lastLaunchVersion == null) {
				firstLaunch = true;
				firstLaunchAfterUpdate = false;
			} else if (lastLaunchVersion != getCurrentVersion()) {
				firstLaunch = false;
				firstLaunchAfterUpdate = true;
			} else {
				firstLaunch = false;
				firstLaunchAfterUpdate = false;
			}
			setLastLaunchVersion();
			handleLaunch = true;
		}
	}

	public boolean getFirstLaunchAndDropValue() {
		handleLaunch();
		boolean value = firstLaunch;
		firstLaunch = false;
		return value;
	}

	public boolean getFirstLaunchAfterUpdateAndDropValue() {
		handleLaunch();
		boolean value = firstLaunchAfterUpdate;
		firstLaunchAfterUpdate = false;
		return value;
	}

	public boolean isFirstLaunch() {
		handleLaunch();
		return firstLaunch;
	}
}
