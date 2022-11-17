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

package com.pushwoosh.location.tracker;

import android.location.Location;

import com.google.android.gms.location.LocationRequest;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.internal.checker.LocationStateChecker;
import com.pushwoosh.location.internal.utils.LocationConfig;

import java.util.concurrent.Executor;

import static com.pushwoosh.location.internal.utils.LocationConfig.LOCATION_HIGH_INTERVAL;
import static com.pushwoosh.location.internal.utils.LocationConfig.LOCATION_LOW_INTERVAL;


final class GoogleLocationTracker implements LocationTracker, GoogleLocationProvider.GoogleLocationListener, GoogleLocationProvider.RequestLocationListener {
	static final String SUB_TAG = "[LocationTracker]";

	private final LocationTrackerCallback locationTrackerCallback;
	private final LocationUpdateListener locationUpdateListener;
	private final GoogleLocationProvider locationProvider;
	private final LocationStateChecker locationStateChecker;

	private boolean highAccuracy = false;
	private boolean needRequestLocationUpdates = false;
	private boolean notifyCallback = false;

	GoogleLocationTracker(final LocationTrackerCallback locationTrackerCallback,
						  final LocationUpdateListener locationUpdateListener,
						  final GoogleLocationProvider locationProvider,
						  LocationStateChecker locationStateChecker) {
		this.locationTrackerCallback = locationTrackerCallback;
		this.locationUpdateListener = locationUpdateListener;
		this.locationProvider = locationProvider;
		this.locationProvider.setGoogleLocationListener(this);
		this.locationProvider.setRequestLocationListener(this);
		this.locationStateChecker = locationStateChecker;
	}

	private void notifyFailedCallback() {
		if (!notifyCallback) {
			this.locationTrackerCallback.failedProvidingLocation();
			notifyCallback = true;
		}
	}

	@Override
	public void onConnected() {
		locationProvider.getLastLocation(lastLocation -> {
			locationUpdated(lastLocation);

			if (needRequestLocationUpdates) {
				updateLocationTracking(highAccuracy);
				needRequestLocationUpdates = false;
			}
		});
	}

	@Override
	public void locationUpdated(final Location location) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + " location updated: " + location);
		locationUpdateListener.locationUpdated(location);
	}

	private void updateLocationTracking(boolean highAccuracy) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + " updateLocationTracking with highAccuracy: " + highAccuracy);

		this.highAccuracy = highAccuracy;
		locationProvider.updateLocationTracker(createLocationRequest(highAccuracy));
	}

	private void notifySuccessCallback() {
		if (!notifyCallback) {
			locationTrackerCallback.successProvidingLocation();
			notifyCallback = true;
		}
	}

	private LocationRequest createLocationRequest(boolean highAccuracy) {
		LocationRequest locationRequest = new LocationRequest();
		locationRequest.setInterval(highAccuracy ? LOCATION_HIGH_INTERVAL : LOCATION_LOW_INTERVAL);
		locationRequest.setPriority(highAccuracy ? LocationRequest.PRIORITY_HIGH_ACCURACY : LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

		return locationRequest;
	}

	@Override
	public void requestLocationUpdates(boolean highAccuracy) {
		if (!locationProvider.isConnected()) {
			needRequestLocationUpdates = true;
			this.locationProvider.connect();
			return;
		}

		updateLocationTracking(highAccuracy);
	}

	@Override
	public void getLocation(OnGetLastLocationCallback onGetLastLocationCallback) {
		locationProvider.getLastLocation(onGetLastLocationCallback);
	}

	@Override
	public void getLocation(Executor executor, OnGetLastLocationCallback onGetLastLocationCallback) {
		locationProvider.getLastLocation(executor, onGetLastLocationCallback);
	}

	@Override
	public void onDestroy() {
		needRequestLocationUpdates = true;
		locationProvider.cancel();
	}

	@Override
	public boolean isLocationAvailable() {
		boolean locationAvailable = locationProvider.isLocationAvailable();
		if(!locationAvailable){
			locationAvailable = locationStateChecker.check();
		}
		return locationAvailable;
	}

	@Override
	public void successRequestLocation() {
		notifySuccessCallback();
	}

	@Override
	public void failedRequestLocation() {
		notifyFailedCallback();
	}
}
