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

package com.pushwoosh.location.network;

import android.location.Location;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.NearestZonesManager;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.location.network.job.Job;
import com.pushwoosh.location.network.repository.UpdateNearestRepository;
import com.pushwoosh.location.tracker.LocationTracker;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

class GetNearestZoneJobApplier {
	private static final String SUB_TAG = "[GetNearestZoneJobApplier]";

	private final UpdateNearestRepository updateNearestRepository;
	private final NearestZonesManager nearestZonesManager;
	@Nullable
	private final LocationTracker locationTracker;

	private Job<Result<Pair<Location, List<GeoZone>>, PushwooshException>> nearestZonesJob;
	private Job<Result<Void, NetworkException>> locationJob;

	GetNearestZoneJobApplier(NearestZonesManager nearestZonesManager, UpdateNearestRepository updateNearestRepository, @Nullable LocationTracker locationTracker) {
		this.nearestZonesManager = nearestZonesManager;
		this.updateNearestRepository = updateNearestRepository;
		this.locationTracker = locationTracker;
	}


	void loadNearestGeoZones(final boolean forceUpdate) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + " try to update nearest geoZones");

		if (locationTracker == null || !locationTracker.isLocationAvailable()) {
			locationDisable();
			return;
		}

		updateNearestRepository.applyNearestJob(forceUpdate,
				result -> {
					if (result.isSuccess()) {
						PWLog.noise(LocationConfig.TAG, SUB_TAG + " success update nearest geoZones");


						Pair<Location, List<GeoZone>> data = result.getData();
						if (data != null) {
							PWLog.noise(LocationConfig.TAG, SUB_TAG + " geoZones list:" + data);
							nearestZonesManager.updateZones(data.first, data.second);
						}
					} else {
						PWLog.error(LocationConfig.TAG, SUB_TAG + " failed update nearest geoZones " + (result.getException() != null ? result.getException().getLocalizedMessage() : ""));
						if (result.getException() instanceof LocationNotAvailableException) {
							nearestZonesManager.requestLocation();
						}
					}

					nearestZonesManager.updateState(!result.isSuccess());
				},
				updateNearestRepositoryCallback -> nearestZonesJob = updateNearestRepositoryCallback.getData());
	}

	void locationDisable() {
		PWLog.noise("Try to remove location form service");
		locationJob = updateNearestRepository.applyDisableLocationJob(result -> PWLog.noise("Remove location from service state: " + result.isSuccess()));
	}

	void cancel() {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + "stop nearestZonesJob nearest geoZones");

		if (nearestZonesJob != null) {
			nearestZonesJob.cancel();
		}

		if (locationJob != null) {
			locationJob.cancel();
		}

		nearestZonesJob = null;
	}
}
