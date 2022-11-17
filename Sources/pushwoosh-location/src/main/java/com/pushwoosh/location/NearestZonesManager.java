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

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.geofence.GeoZonesUpdater;
import com.pushwoosh.location.geofence.GeofenceTracker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionDeclaredChecker;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;
import com.pushwoosh.location.internal.event.LocationPermissionEvent;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.location.scheduler.ServiceScheduler;
import com.pushwoosh.location.storage.LocationPrefs;
import com.pushwoosh.location.tracker.LocationTracker;
import com.pushwoosh.location.tracker.LocationTrackerCallback;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;

public class NearestZonesManager implements GeoZonesUpdater, LocationTrackerCallback {

	private final GeofenceTracker geofenceTracker;
	private final LocationPrefs locationPrefs;
	@Nullable
	private final LocationTracker locationTracker;
	private final LocationPermissionChecker locationPermissionChecker;
	private final FineLocationPermissionChecker fineLocationPermissionChecker;
	private final BackgroundLocationPermissionDeclaredChecker backgroundLocationPermissionDeclaredChecker;
	private final BackgroundLocationPermissionChecker backgroundLocationPermissionChecker;

	private final ServiceScheduler serviceScheduler;
	private final Handler handler = new Handler(Looper.getMainLooper());

	private WeakReference<Callback<Void, LocationNotAvailableException>> callback;

	public NearestZonesManager(GeofenceTracker geofenceTracker,
							   LocationPrefs locationPrefs,
							   @Nullable final LocationTracker locationTracker,
							   final LocationPermissionChecker locationPermissionChecker,
							   final FineLocationPermissionChecker fineLocationPermissionChecker,
							   final BackgroundLocationPermissionDeclaredChecker backgroundLocationPermissionDeclaredChecker,
							   final BackgroundLocationPermissionChecker backgroundLocationPermissionChecker,
							   final ServiceScheduler serviceScheduler) {

		this.geofenceTracker = geofenceTracker;
		this.locationPrefs = locationPrefs;
		this.locationTracker = locationTracker;
		this.locationPermissionChecker = locationPermissionChecker;
		this.fineLocationPermissionChecker = fineLocationPermissionChecker;
		this.backgroundLocationPermissionDeclaredChecker = backgroundLocationPermissionDeclaredChecker;
		this.backgroundLocationPermissionChecker = backgroundLocationPermissionChecker;
		this.serviceScheduler = serviceScheduler;

		EventBus.subscribe(LocationPermissionEvent.class, event -> {
			if (locationPermissionChecker.check()) {
				startUpdateZones();
			} else {
				stop();
				failedNotifyCallback();
			}
		});
	}

	private void startUpdateZones() {
		if (locationTracker == null) {
			return;
		}

		this.locationTracker.requestLocationUpdates(false);
	}

	private void failedNotifyCallback() {
		handler.post(() -> {
			if (callback != null && callback.get() != null) {
				callback.get().process(Result.fromException(new LocationNotAvailableException()));
			}
		});
	}

	private void successNotifyCallback() {
		handler.post(() -> {
			if (callback != null && callback.get() != null) {
				callback.get().process(Result.fromData(null));
			}
		});
	}

	@SuppressLint("MissingPermission")
	@Override
	public void requestUpdateGeoZones(Callback<Boolean, PushwooshException> callback) {
		if (locationPermissionChecker.check() && locationPrefs.geolocationStarted().get()) {
			if (locationTracker == null) {
				callback.process(Result.fromData(false));
				return;
			}

			locationTracker.getLocation(location -> {
				if (location != null) {
					serviceScheduler.requestUpdateNearestGeoZones();
					callback.process(Result.fromData(true));
				} else {
					locationTracker.requestLocationUpdates(false);
					callback.process(Result.fromData(false));
				}
			});
		}
	}

	@Override
	public void failedProvidingLocation() {
		stop();
		failedNotifyCallback();
	}

	@Override
	public void successProvidingLocation() {
		successNotifyCallback();
	}

	void start(final Callback<Void, LocationNotAvailableException> callback) {
		this.callback = new WeakReference<>(callback);
		locationPrefs.geolocationStarted().set(true);
		geofenceTracker.startTracking();

		tryUpdateZones();
	}

	void stop() {
		locationPrefs.geolocationStarted().set(false);

		geofenceTracker.onDestroy();
		if (locationTracker != null) {
			locationTracker.onDestroy();
		}

		serviceScheduler.cancel();
		serviceScheduler.requestLocationDisabled();
	}

	private void tryUpdateZones() {
		if (!locationPermissionChecker.check()) {
			locationPermissionChecker.requestPermissions(LocationConfig.LOCATION_PERMISSIONS);
			return;
		}

		startUpdateZones();
	}


	public void updateZones(final Location location, final List<GeoZone> geoZones) {
		geofenceTracker.updateZones(geoZones == null ? Collections.emptyList() : geoZones, location);
	}

	public void updateState(final boolean sleep) {
		serviceScheduler.scheduleNearestGeoZones(sleep);
	}

	void deviceRebooted() {
		serviceScheduler.deviceRebooted();
	}

	public void requestLocation() {
		if (locationPrefs.geolocationStarted().get()) {
			tryUpdateZones();
		}
	}

	public void requestBackgroundLocationPermission() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
			return;
		}
		if (!locationPermissionChecker.check()) {
			PWLog.error("The application did not receive Fine/Coarse location permission yet.");
			return;
		}
		if (!backgroundLocationPermissionDeclaredChecker.check()) {
			PWLog.error("Background location permission is not declared in AndroidManifest.");
			return;
		}
		if (!backgroundLocationPermissionChecker.check()) {
			locationPermissionChecker.requestPermissions(LocationConfig.BACKGROUND_LOCATION_PERMISSION);
		}
	}
}
