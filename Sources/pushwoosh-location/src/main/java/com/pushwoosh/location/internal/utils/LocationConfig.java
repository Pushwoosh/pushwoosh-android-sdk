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

package com.pushwoosh.location.internal.utils;

import android.Manifest;

import java.util.concurrent.TimeUnit;

public class LocationConfig {
	public static final String TAG = "PushwooshLocation";

	public static final long LOCATION_HIGH_INTERVAL = TimeUnit.SECONDS.toMillis(30);
	public static final long LOCATION_LOW_INTERVAL = TimeUnit.MINUTES.toMillis(5);
	public final static long NEAREST_ZONES_FAST_UPDATE = TimeUnit.MINUTES.toMillis(15);
	public final static long NEAREST_ZONES_LONG_UPDATE = TimeUnit.MINUTES.toMillis(30);
	public final static long MAX_GEO_ZONES_CACHE_TIME = TimeUnit.MINUTES.toMillis(10);

	private static final boolean SHOW_LOCATION_NOTIFICATION_DIALOG = true;
	private static final boolean DISABLE_LOCATION_IN_SERVICE = true;

	public static String[] LOCATION_PERMISSIONS = new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION};
	public static String[] BACKGROUND_LOCATION_PERMISSION = new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION};
	/**
	 * Flag for change behavior connected with dialogs which help developers for implementation location feature (Show permission dialog and google api dialogs)
	 * @return true if need to show dialogs
	 */
	public static boolean showLocationDialogs() {
		return SHOW_LOCATION_NOTIFICATION_DIALOG;
	}

	/**
	 * Flag for change behavior when location disabled on device for this app
	 * @return true if need send to service (null, null) location when location not available for this app. {issue PUSH-5404 }
	 */
	public static boolean disableLocationInService(){
		return DISABLE_LOCATION_IN_SERVICE;
	}
}
