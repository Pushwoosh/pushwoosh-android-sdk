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

package com.pushwoosh.location.scheduler;

import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.internal.utils.LocationConfig;

public class ServiceScheduler {
	private static final String TAG = LocationConfig.TAG;
	private static final String SUB_TAG = "[ServiceScheduler]";
	private long lastTimeUpdate = System.currentTimeMillis();

	@Nullable
	private final Context context;

	public ServiceScheduler(@Nullable final Context context) {
		this.context = context;
	}

	public void scheduleNearestGeoZones(boolean sleep) {
		PWLog.noise(TAG, SUB_TAG + " scheduleNearestGeoZones " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - lastTimeUpdate));
		lastTimeUpdate = System.currentTimeMillis();

		SchedulerProvider.getScheduler().scheduleNearestGeoZones(context, sleep ? LocationConfig.NEAREST_ZONES_LONG_UPDATE : LocationConfig.NEAREST_ZONES_FAST_UPDATE);
	}

	public void cancel() {
		PWLog.noise(TAG, SUB_TAG + " cancel");

		SchedulerProvider.getScheduler().stop(context);
	}

	@RequiresPermission(allOf = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})
	public void requestUpdateNearestGeoZones() {
		cancel();
		PWLog.noise(TAG, SUB_TAG + " requestUpdateNearestGeoZones");

		SchedulerProvider.getScheduler().requestUpdateNearestGeoZones(context);
	}

	/**
	 * Send to service null location
	 */
	public void requestLocationDisabled() {
		if (LocationConfig.disableLocationInService()) {
			SchedulerProvider.getScheduler().requestLocationDisabled(context);
		}
	}

	/**
	 * Call this method when device rebooted
	 */
	public void deviceRebooted() {
		PWLog.noise(TAG, "deviceRebooted");
		SchedulerProvider.getScheduler().deviceRebooted(context);
	}
}
