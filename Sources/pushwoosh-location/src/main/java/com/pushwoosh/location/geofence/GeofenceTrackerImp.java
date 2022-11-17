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

package com.pushwoosh.location.geofence;

import android.location.Location;

import com.google.android.gms.location.Geofence;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.geofencer.Geofencer;
import com.pushwoosh.location.geofencer.GoogleGeofencer;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.storage.GeoZoneStorage;
import com.pushwoosh.location.tracker.LocationTracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class GeofenceTrackerImp implements GeofenceTracker {
	private static final String SUB_TAG = "[GeofenceTracker]";
	static final int MIN_RADIUS = 100;

	private final Collection<GeoZone> geoZones;
	@Nullable
	private final Geofencer geofencer;
	private final GeoZoneStorage geoZoneStorage;
	@Nullable
	private final LocationTracker locationTracker;
	private final FineLocationPermissionChecker fineLocationPermissionChecker;

	private GeoZonesUpdater geoZonesUpdater;
	List<GeoZone> pushGeoZones = new ArrayList<>();

	@Nullable
	private GeoZone mRadiusZone;
	private boolean needUpdate = true;
	private boolean geoZonesUpdated;

	GeofenceTrackerImp(@Nullable Geofencer geofencer,
					   GeoZonesUpdater geoZonesUpdater,
					   GeoZoneStorage geoZoneStorage,
					   @Nullable LocationTracker locationTracker,
					   FineLocationPermissionChecker fineLocationPermissionChecker) {
		this.geofencer = geofencer;
		this.geoZonesUpdater = geoZonesUpdater;
		this.geoZoneStorage = geoZoneStorage;
		this.locationTracker = locationTracker;
		this.fineLocationPermissionChecker = fineLocationPermissionChecker;

		geoZones = new HashSet<>(geoZoneStorage.getGeoZones());
		EventBus.subscribe(GoogleGeofencer.GoogleGeofencerConnectedEvent.class, (event) -> {
			geofencer.addZones(new ArrayList<>(this.geoZones));
		});
	}

	@Override
	public void updateZones(@NonNull final List<GeoZone> zones, final Location location) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + "updateZones currentZones: " + geoZones + "; newZones: " + zones + "; pushGeoZones: " + pushGeoZones);
		geoZonesUpdated = true;
		needUpdate = true;

		GeoZone newRadiusGeoZone = GeoZone.createRadiusZone(zones, location, MIN_RADIUS);

		removeExpiredZones(zones, newRadiusGeoZone);
		addNewZones(zones, newRadiusGeoZone);

		geoZoneStorage.saveGeoZones(geoZones);

		updatePushGeoZones(location);
	}

	private void removeExpiredZones(final List<GeoZone> newZones, final GeoZone newRadiusGeoZone) {
		List<GeoZone> expiredZones = new ArrayList<>(this.geoZones);

		expiredZones.removeAll(newZones);
		this.geoZones.removeAll(expiredZones);

		if (mRadiusZone != null && !mRadiusZone.equals(newRadiusGeoZone)) {
			expiredZones.add(mRadiusZone);
		}

		if (geofencer == null) {
			return;
		}
		geofencer.removeZones(expiredZones);
	}

	private void addNewZones(final List<GeoZone> newZones, final GeoZone newRadiusGeoZone) {
		final List<GeoZone> newGeoZones = new ArrayList<>(newZones);
		newGeoZones.removeAll(this.geoZones);

		this.geoZones.addAll(newGeoZones);

		mRadiusZone = newRadiusGeoZone;

		if (mRadiusZone != null) {
			newGeoZones.add(mRadiusZone);
		}

		if (geofencer == null) {
			return;
		}
		geofencer.addZones(newGeoZones);
	}

	private void updatePushGeoZones(final Location location) {
		if (pushGeoZones.isEmpty()) {
			return;
		}

		for (GeoZone geoZone : geoZones) {
			if (pushGeoZones.contains(geoZone) && geoZone.distanceTo(location) < geoZone.getRange()) {
				//User came to this geo zones. Service sent geo push if it was needed
				pushGeoZones.remove(geoZone);
			}
		}

		//if user enter to all geoZones then start update location less intensive
		requestLocationIfNeeded();
	}

	private void requestLocationIfNeeded() {
		if (pushGeoZones.isEmpty() && locationTracker != null) {
			locationTracker.requestLocationUpdates(false);
		}
	}

	@Override
	public void onGeofenceStateChanged(List<String> zoneIds, int geofenceTransition) {
		List<GeoZone> pushZones = new ArrayList<>();
		boolean requestUpdates = false;

		for (String zoneId : zoneIds) {
			GeoZone geoZoneById = getGeoZoneById(zoneId);
			if (geoZoneById != null) {
				pushZones.add(geoZoneById);
			}

			if (mRadiusZone != null && mRadiusZone.getName().equals(zoneId)) {
				if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
					requestUpdates = true;
				}
			}
		}

		switch (geofenceTransition) {
			case Geofence.GEOFENCE_TRANSITION_ENTER:
				enterToGeofence(pushZones, callback -> checkNeedUpdate(callback.getData()));
				return;
			case Geofence.GEOFENCE_TRANSITION_EXIT:
				if (pushGeoZones.isEmpty()) {
					break;
				}

				pushGeoZones.removeAll(pushZones);
				// if user exit from all geofence zones then start update location less intensive
				requestLocationIfNeeded();
				break;
			case Geofence.GEOFENCE_TRANSITION_DWELL:
				break;
			default:
				break;
		}

		checkNeedUpdate(requestUpdates);
	}

	private void checkNeedUpdate(boolean requestUpdates) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + "PushGeoZones updated: " + pushGeoZones);
		if (requestUpdates) {
			geoZonesUpdater.requestUpdateGeoZones(callback ->needUpdate = !callback.getData());
		}
	}

	private void enterToGeofence(final List<GeoZone> pushZones, Callback<Boolean, PushwooshException> callback) {
		// if user enter to geofence zone and distance to this geozone less than needed range
		// Start updating location more intensive to getting location which require to this zone
		if (!pushZones.isEmpty()) {
			if (locationTracker == null) {
				callback.process(Result.fromData(false));
				return;
			}

			locationTracker.getLocation(location -> {
				boolean needUpdateLocationFaster = false;
				boolean requestUpdates = false;
				for (GeoZone geoZone : pushZones) {
					if (geoZone.distanceTo(location) > geoZone.getRange()) {
						needUpdateLocationFaster = true;

						if (!geoZone.equals(mRadiusZone)) {
							pushGeoZones.add(geoZone);
						}
					} else {
						requestUpdates = true;
					}
				}

				if (needUpdateLocationFaster && fineLocationPermissionChecker.check()) {
					locationTracker.requestLocationUpdates(true);
				}

				callback.process(Result.fromData(requestUpdates));
			});
		}
	}

	@Nullable
	private GeoZone getGeoZoneById(final String zoneId) {
		for (GeoZone trackingZone : geoZones) {
			if ((trackingZone.getName() == null && zoneId == null) || (trackingZone.getName().equals(zoneId))) {
				return trackingZone;
			}
		}

		return null;
	}

	@Override
	public void onDestroy() {
		geoZonesUpdated = false;
		if (geofencer == null) {
			PWLog.noise(LocationConfig.TAG, "geofencer is null");
			return;
		}
		geofencer.onDestroy();
	}

	@Override
	public void startTracking() {
		PWLog.noise(LocationConfig.TAG, "startTracking");
		if (geofencer == null) {
			PWLog.noise(LocationConfig.TAG, "geofencer is null");
			return;
		}
		geofencer.connect();
	}

	@Override
	public void locationUpdated(final Location location) {
		if (!needUpdate) {
			return;
		}

		boolean hasPushGeoZoneInRange = false;
		for (GeoZone geoZone : pushGeoZones) {
			if (geoZone.distanceTo(location) < geoZone.getRange()) {
				hasPushGeoZoneInRange = true;
				break;
			}
		}

		if (hasPushGeoZoneInRange || !geoZonesUpdated) {
			PWLog.noise(SUB_TAG, "Request update geoZones. PushGeoZones: " + pushGeoZones + "; geoZonesUpdated: " + geoZonesUpdated);
			geoZonesUpdater.requestUpdateGeoZones(callback -> needUpdate = !callback.getData());
		}
	}
}
