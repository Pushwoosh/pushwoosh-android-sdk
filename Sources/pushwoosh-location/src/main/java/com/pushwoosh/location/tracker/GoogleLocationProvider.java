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

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.Task;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.foregroundservice.ForegroundServiceHelper;
import com.pushwoosh.location.internal.checker.GoogleApiChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;
import com.pushwoosh.location.internal.event.LocationPermissionEvent;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.internal.utils.ResolutionActivity;

import java.util.Collections;
import java.util.concurrent.Executor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;
import static com.pushwoosh.location.tracker.GoogleLocationTracker.SUB_TAG;

/**
 * This class encapsulates all logic connected with googleApiClient for {@link GoogleLocationTracker}
 */
class GoogleLocationProvider implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
	@Nullable
	private final Context context;
	private final GoogleApiChecker googleApiChecker;
	private final LocationPermissionChecker locationPermissionChecker;
	@Nullable
	private GoogleLocationListener googleLocationListener;
	@Nullable
	private RequestLocationListener requestLocationListener;

	@Nullable
	private final GoogleApiClient googleApiClient;
	private Location location;

	private LocationRequest locationRequest;
	private boolean checkedState;
	private ForegroundServiceHelper foregroundServiceHelper;
	private final GoogleLocationCallback googleLocationCallback;

	GoogleLocationProvider(@Nullable final Context context,
						   final GoogleApiChecker googleApiChecker,
						   final LocationPermissionChecker locationPermissionChecker,
						   @Nullable ForegroundServiceHelper foregroundServiceHelper) {
		this.context = context;
		this.googleApiChecker = googleApiChecker;
		if (context == null) {
			PWLog.error("Incorrect state of application. Context is empty");
			googleApiClient = null;
		} else {
			googleApiClient = new GoogleApiClient.Builder(context)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.addApi(LocationServices.API)
					.build();
		}
		this.locationPermissionChecker = locationPermissionChecker;
		this.foregroundServiceHelper = foregroundServiceHelper;

		googleApiChecker.setGoogleApiClient(googleApiClient);

		this.googleLocationCallback = new GoogleLocationCallback();

		EventBus.subscribe(ResolutionActivity.ResolutionEvent.class, event -> notifyRequestLocationListener(event.isSuccess()));
		EventBus.subscribe(LocationPermissionEvent.class, event -> {
			checkedState = false;
		});
	}

	void setGoogleLocationListener(@Nullable final GoogleLocationListener googleLocationListener) {
		this.googleLocationListener = googleLocationListener;

		if (googleLocationListener == null) {
			return;
		}

		if (isConnected()) {
			this.googleLocationListener.onConnected();

			if (location != null) {
				googleLocationListener.locationUpdated(location);
			}
		}

	}

	void setRequestLocationListener(@Nullable final RequestLocationListener requestLocationListener) {
		this.requestLocationListener = requestLocationListener;
	}

	@SuppressLint("MissingPermission")
	private void notifyRequestLocationListener(final boolean isSuccess) {
		if (requestLocationListener == null || googleApiClient == null) {
			return;
		}

		if (isSuccess) {
			startForegroundService();
			if (!locationPermissionChecker.check() || locationRequest == null || !isConnected()
					|| context == null) {
				requestLocationListener.failedRequestLocation();
				return;
			}
			getFusedLocationProviderClient(context)
					.requestLocationUpdates(locationRequest, googleLocationCallback, Looper.myLooper());
			requestLocationListener.successRequestLocation();
		} else {
			requestLocationListener.failedRequestLocation();
		}
	}

	private void startForegroundService() {
		if (foregroundServiceHelper != null) {
			foregroundServiceHelper.startService();
		}
	}

	void connect() {
		if (googleApiClient == null) {
			return;
		}

		if (!googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
			googleApiClient.connect();
		}
	}

	@Override
	public void onConnected(@Nullable final Bundle bundle) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + " connected to google play service");

		//save last location
		//noinspection MissingPermission
		if (googleLocationListener != null) {
			googleLocationListener.onConnected();
		}
	}

	void getLastLocation(OnGetLastLocationCallback onGetLastLocationCallback) {
		getLastLocation(null, onGetLastLocationCallback);
	}

	@SuppressLint("MissingPermission")
	void getLastLocation(Executor executor, OnGetLastLocationCallback onGetLastLocationCallback) {
		if (!locationPermissionChecker.check() || !googleApiChecker.check() || context == null) {
			onGetLastLocationCallback.onGetLastLocation(null);
			return;
		}

		Task<Location> lastLocationTask =
				LocationServices.getFusedLocationProviderClient(context).getLastLocation();
		if (executor == null) {
			lastLocationTask.addOnCompleteListener(onComplete -> {
				if (onComplete.isSuccessful()) {
					onGetLastLocationCallback.onGetLastLocation(onComplete.getResult());
				}
			});
		} else {
			lastLocationTask.addOnCompleteListener(executor, onComplete -> {
				if (onComplete.isSuccessful()) {
					onGetLastLocationCallback.onGetLastLocation(onComplete.getResult());
				}
			});
		}
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult result) {
		// Refer to the javadoc for ConnectionResult to see what error codes might be returned in
		// onConnectionFailed.
		checkedState = false;
		PWLog.error(LocationConfig.TAG, SUB_TAG + " connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// The connection to Google Play services was lost for some reason. We call connect() to
		// attempt to re-establish the connection.
		PWLog.info(LocationConfig.TAG, SUB_TAG + " connection suspended");

		if (googleApiClient == null) {
			return;
		}

		googleApiClient.reconnect();
	}

	void updateLocationTracker(LocationRequest locationRequest) {
		if (!googleApiChecker.check() || context == null) {
			return;
		}
		getFusedLocationProviderClient(context)
				.removeLocationUpdates(googleLocationCallback);

		LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder()
				.addLocationRequest(locationRequest)
				.build();

		this.locationRequest = locationRequest;

		Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(context).checkLocationSettings(locationSettingsRequest);
		task.addOnCompleteListener(completedTask -> {
			try {
				LocationSettingsResponse response = task.getResult(ApiException.class);
				notifyRequestLocationListener(true);
			} catch (ApiException exception) {
				PWLog.noise(LocationConfig.TAG, SUB_TAG + " Requesting location has status code " + exception.getStatusCode());
				switch (exception.getStatusCode()) {
					case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
						if (checkedState) {
							return;
						}
						checkedState = true;
						// Location settings are not satisfied. But could be fixed by showing the user
						// a dialog.
						if (LocationConfig.showLocationDialogs()) {
							ResolutionActivity.resolutionSettingApi(context, locationSettingsRequest);
						} else {
							notifyRequestLocationListener(false);
						}
						break;
					case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
						notifyRequestLocationListener(false);
						break;
				}
			}
		});
	}

	boolean isConnected() {
		return googleApiClient != null && googleApiClient.isConnected();
	}

	void cancel() {
		if (googleApiClient != null && googleApiClient.isConnected() && context != null) {
			getFusedLocationProviderClient(context)
					.removeLocationUpdates(googleLocationCallback);
			googleApiClient.disconnect();
		}

		checkedState = false;
		location = null;
	}

	@SuppressLint("MissingPermission")
	boolean isLocationAvailable() {
		if (!isConnected() || locationPermissionChecker.check() || context == null) {
			return false;
		}
		return LocationServices.FusedLocationApi.getLocationAvailability(googleApiClient).isLocationAvailable();
	}

	interface GoogleLocationListener {
		void onConnected();

		void locationUpdated(Location location);
	}

	interface RequestLocationListener {
		void successRequestLocation();

		void failedRequestLocation();
	}

	class GoogleLocationCallback extends LocationCallback {
		@Override
		public void onLocationResult(LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			GoogleLocationProvider.this.location = location;
			if (googleLocationListener != null) {
				googleLocationListener.locationUpdated(location);
			}
		}

		@Override
		public void onLocationAvailability(LocationAvailability locationAvailability) {
			super.onLocationAvailability(locationAvailability);
		}
	}
}
