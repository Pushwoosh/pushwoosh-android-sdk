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

package com.pushwoosh.location.network.job;

import android.location.Location;
import androidx.core.util.Pair;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.network.data.GetNearestZoneRequest;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;

import java.util.List;

public class UpdateNearestJob implements Job<Result<Pair<Location, List<GeoZone>>, PushwooshException>> {

	private Location currentLocation;

	public UpdateNearestJob(final Location currentLocation) {
		this.currentLocation = currentLocation;
	}

	private boolean checkParams() {
		boolean networkAvailable = GeneralUtils.isNetworkAvailable();

		PWLog.noise(LocationConfig.TAG, "Try to load nearest geoZones. Network available: " + networkAvailable + "; Current location: " + currentLocation);
		return currentLocation != null && networkAvailable;

	}

	@Override
	public Result<Pair<Location, List<GeoZone>>, PushwooshException> apply() {
		try {
			if (checkParams()) {
				GetNearestZoneRequest request = new GetNearestZoneRequest(currentLocation);
				RequestManager requestManager = NetworkModule.getRequestManager();

				Result<List<GeoZone>, NetworkException> result = requestManager == null ?
						Result.fromException(new NetworkException("Request Manager is null")) :
						requestManager.sendRequestSync(request);

				if (result.isSuccess() && result.getData() != null) {
					return Result.fromData(new Pair<>(currentLocation, result.getData()));
				} else {
					return Result.fromException(result.getException());
				}
			} else {
				return Result.fromException(new LocationNotAvailableException());
			}
		} catch (Exception e) {
			return Result.fromException(new PushwooshException(e));
		}
	}

	@Override
	public void cancel() {
	}
}