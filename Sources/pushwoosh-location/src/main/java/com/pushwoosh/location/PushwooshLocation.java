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

package com.pushwoosh.location;

import androidx.annotation.Nullable;

import com.pushwoosh.function.Callback;
import com.pushwoosh.location.internal.LocationModule;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;

/**
 * PushwooshLocation is a static class responsible for pushwoosh geolocation tracking. <br>
 * By default pushwoosh-location library automatically adds following permissions: <br>
 * <a href="https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_FINE_LOCATION">permission.ACCESS_FINE_LOCATION</a> <br>
 * <a href="https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_COARSE_LOCATION">android.permission.ACCESS_COARSE_LOCATION</a> <br>
 * <br><br>
 * For Android 6 and higher these permissions should be requested dynamically before invoking PushwooshLocation.startLocationTracking()
 */
public class PushwooshLocation {
	/**
	 * Starts location tracking for geo push notifications.
	 */
	public static void startLocationTracking() {
		startLocationTracking(null);
	}

	/**
	 * Starts location tracking for geo push notifications.
	 *
	 * @param callback return {@link com.pushwoosh.function.Result#isSuccess()} if user accept all needed permissions and enable location
	 */
	public static void startLocationTracking(@Nullable Callback<Void, LocationNotAvailableException> callback) {
		LocationModule.nearestZonesManager().start(callback);
	}

	/**
	 * Stops geolocation tracking.
	 */
	public static void stopLocationTracking() {
		LocationModule.nearestZonesManager().stop();
	}

	/**
	 * Requests background location permission. Works on Android 10 or above. On Android 12 opens
	 * the application's location permission settings. Before calling this method make sure the
	 * application already has ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION permission and
	 * ACCESS_BACKGROUND_LOCATION permission is declared in the AndroidManifest.xml.
	 */
	public static void requestBackgroundLocationPermission() {
		LocationModule.nearestZonesManager().requestBackgroundLocationPermission();
	}
}
