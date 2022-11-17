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

package com.pushwoosh.location.network.repository;

import android.location.Location;
import android.os.AsyncTask;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.network.job.DisableLocationJob;
import com.pushwoosh.location.network.job.EmptyJob;
import com.pushwoosh.location.network.job.Job;
import com.pushwoosh.location.network.job.UpdateNearestJob;
import com.pushwoosh.location.storage.NearestZonesStorage;
import com.pushwoosh.location.tracker.LocationTracker;

import java.util.List;

public class UpdateNearestRepository {

	private final NearestZonesStorage nearestZonesStorage;
	@Nullable
	private final LocationTracker locationTracker;

	public UpdateNearestRepository(NearestZonesStorage nearestZonesStorage, @Nullable LocationTracker locationTracker) {
		this.nearestZonesStorage = nearestZonesStorage;
		this.locationTracker = locationTracker;
	}

	public void applyNearestJob(final boolean forceUpdate,
								Callback<Pair<Location, List<GeoZone>>, PushwooshException> callback,
								Callback<Job<Result<Pair<Location, List<GeoZone>>, PushwooshException>>, PushwooshException> updateNearestJobCallback) {
		if (locationTracker == null) {
			applyNearestJobForLocation(null, forceUpdate, callback, updateNearestJobCallback);
		} else {
			locationTracker.getLocation(AsyncTask.THREAD_POOL_EXECUTOR, location ->
					applyNearestJobForLocation(location,
						forceUpdate,
						callback,
						updateNearestJobCallback));
		}
	}

	private void applyNearestJobForLocation(Location location,
											boolean forceUpdate,
											Callback<Pair<Location, List<GeoZone>>, PushwooshException> callback,
											Callback<Job<Result<Pair<Location, List<GeoZone>>, PushwooshException>>, PushwooshException> updateNearestJobCallback) {
		List<GeoZone> storageGeoZones = nearestZonesStorage.getAll();
		if (storageGeoZones != null && !forceUpdate) {
			callback.process(Result.fromData(new Pair<>(location, storageGeoZones)));
			updateNearestJobCallback.process(Result.fromData(new EmptyJob<>()));
			return;
		}

		UpdateNearestJob updateNearestJob = new UpdateNearestJob(location);
		Result<Pair<Location, List<GeoZone>>, PushwooshException> result = updateNearestJob.apply();
		if (result.isSuccess() && result.getData() != null) {
			nearestZonesStorage.save(result.getData().second);
		}

		callback.process(result);
		updateNearestJobCallback.process(Result.fromData(updateNearestJob));
	}

	public Job<Result<Void, NetworkException>> applyDisableLocationJob(Callback<Void, NetworkException> callback) {
		DisableLocationJob disableLocationJob = new DisableLocationJob();
		Result<Void, NetworkException> result = disableLocationJob.apply();

		if (callback != null) {
			callback.process(result);
		}
		return disableLocationJob;
	}
}
