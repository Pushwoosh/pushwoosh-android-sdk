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

package com.pushwoosh.location.geofencer;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.pushwoosh.internal.event.Event;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.internal.checker.GoogleApiChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;
import com.pushwoosh.location.internal.utils.LocationConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: 04-Sep-15
 * Time: 13:43
 *
 * @author Alexander Blinov
 */
public class GoogleGeofencer implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Geofencer {
	public class GoogleGeofencerConnectedEvent implements Event {
	}
	private static final String SUB_TAG = "[Geofencer]";

	private static final int GEOFENCE_LOITERING_DELAY = 1000;


	private final Context context;
	private final GoogleApiClient googleApiClient;
	private final LocationPermissionChecker locationPermissionChecker;
	private final GoogleApiChecker googleApiChecker = new GoogleApiChecker();
	private final GeofencingClient geofencingClient;
	private final PendingIntent pendingIntent;

	GoogleGeofencer(Context context,
					final LocationPermissionChecker locationPermissionChecker,
					GeofencingClient geofencingClient,
					PendingIntent pendingIntent) {
		this.context = context.getApplicationContext();
		this.locationPermissionChecker = locationPermissionChecker;
		this.geofencingClient = geofencingClient;
		this.pendingIntent = pendingIntent;

		googleApiClient = new GoogleApiClient.Builder(this.context)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();

		this.googleApiChecker.setGoogleApiClient(googleApiClient);
	}

	@Override
	public void removeZones(List<GeoZone> zones) {
		if (zones.isEmpty()) {
			return;
		}

		PWLog.noise(LocationConfig.TAG, SUB_TAG + " remove zones: " + zones);
		List<String> ids = new ArrayList<>();
		//noinspection Convert2streamapi
		for (GeoZone geoZone : zones) {
			ids.add(geoZone.getName());
		}

		if (!googleApiChecker.check()) {
			return;
		}
		if(geofencingClient==null){
			PWLog.error(LocationConfig.TAG, SUB_TAG +"geofencingClient is null");
			return;
		}
		try {
			geofencingClient.removeGeofences(ids)
					.addOnCompleteListener(task -> PWLog.debug(LocationConfig.TAG, SUB_TAG + " try to remove geoZones status:" + task));
		} catch (SecurityException ignore) {
			// ignore
		}
	}


	@Override
	public void addZones(List<GeoZone> zones) {
		if (zones.isEmpty()) {
			return;
		}

		PWLog.noise(LocationConfig.TAG, SUB_TAG + " add new zones: " + zones);
		if (!googleApiChecker.check()) {
			return;
		}
		if(pendingIntent==null){
			PWLog.error(LocationConfig.TAG, SUB_TAG +"pendingIntent is null");
			return;
		}
		if(geofencingClient==null){
			PWLog.error(LocationConfig.TAG, SUB_TAG +"geofencingClient is null");
			return;
		}
		if (locationPermissionChecker.check()) {
			//noinspection MissingPermission
			GeofencingRequest geofencingRequest = getGeofencingRequest(zones);
			geofencingClient.addGeofences(geofencingRequest, pendingIntent)
					.addOnCompleteListener(task -> PWLog.debug(LocationConfig.TAG, SUB_TAG + " try to add geoZones status:" + task.isSuccessful()));
		}
	}

	@Override
	public void onDestroy() {
		if (googleApiClient.isConnected()) {
			googleApiClient.disconnect();
		}
	}

	@Override
	public void connect() {
		if (!googleApiClient.isConnected() && !googleApiClient.isConnecting()) {
			googleApiClient.connect();
		}
	}
	/**
	 * Runs when a GoogleApiClient object successfully connects.
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + " connected");
		EventBus.sendEvent(new GoogleGeofencerConnectedEvent());
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult result) {
		PWLog.info(LocationConfig.TAG, SUB_TAG + " connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
	}

	@Override
	public void onConnectionSuspended(int cause) {
		PWLog.error(LocationConfig.TAG, SUB_TAG + " googleGeofencer onConnectionSuspended");
		googleApiClient.reconnect();
	}

	private GeofencingRequest getGeofencingRequest(List<GeoZone> zones) {
		return new GeofencingRequest.Builder()
				// The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
				// GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
				// is already inside that geofence.
				.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
				.addGeofences(mapToGeofenceList(zones))
				.build();
	}

	private List<Geofence> mapToGeofenceList(List<GeoZone> zones) {
		List<Geofence> geofences = new ArrayList<>();

		for (GeoZone geoZone : zones) {

			geofences.add(new Geofence.Builder()
					// Set the request ID of the geofence. This is a string to identify this
					// geofence.
					.setRequestId(geoZone.getName())

					// Set the circular region of this geofence.
					.setCircularRegion(geoZone.getLat(), geoZone.getLng(), geoZone.getRange())

					// Set the expiration duration of the geofence. This geofence gets automatically
					// removed after this period of time.
					.setExpirationDuration(Geofence.NEVER_EXPIRE)

					// Set the transition types of interest. Alerts are only generated for these
					// transition. We track entry and exit transitions in this sample.
					.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
					.setLoiteringDelay(GEOFENCE_LOITERING_DELAY)
					.build());
		}
		return geofences;
	}
}
