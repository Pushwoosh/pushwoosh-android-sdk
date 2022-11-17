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

package com.pushwoosh.location.internal.checker;

import android.Manifest;
import android.content.Context;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.checker.Checker;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.RequestPermissionHelper;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.internal.utils.LocationPermissionActivity;

public class LocationPermissionChecker implements Checker {
	private static final String TAG = "LocationPermissionChecker";

	@Nullable
	private final Context context;

	public LocationPermissionChecker(@Nullable final Context context) {
		this.context = context;
	}

	@Override
	public boolean check() {
		if (RuntimePermissionHelper.hasSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ||
				RuntimePermissionHelper.hasSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)) {
			return true;
		}
		PWLog.error(TAG, "Geolocation permissions not granted");
		return false;
	}

	public void requestPermissions(final String[] strings) {
		if (LocationConfig.showLocationDialogs()) {
			RequestPermissionHelper.requestPermissionsForClass(LocationPermissionActivity.class, context, strings);
		}
	}
}
