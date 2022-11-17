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

package com.pushwoosh.location.storage;

import androidx.annotation.Nullable;

import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.internal.utils.LocationConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Used for networking geoZones cache {@link com.pushwoosh.location.network.GetNearestZonesTask.RequestUpdatesRunnable}
 */
public class NearestZonesStorage {
	private List<GeoZone> geoZones = new ArrayList<>();
	private long cacheTime;

	public void save(List<GeoZone> geoZones) {
		this.geoZones.clear();
		this.geoZones.addAll(geoZones);
		cacheTime = System.currentTimeMillis();
	}

	@Nullable
	public List<GeoZone> getAll() {
		long diff = System.currentTimeMillis() - cacheTime;
		if (diff < LocationConfig.MAX_GEO_ZONES_CACHE_TIME && !geoZones.isEmpty()) {
			return geoZones;
		}

		return null;
	}
}
