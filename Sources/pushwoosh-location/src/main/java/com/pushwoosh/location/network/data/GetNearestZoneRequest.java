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

package com.pushwoosh.location.network.data;

import android.location.Location;
import androidx.annotation.NonNull;

import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.location.data.GeoZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GetNearestZoneRequest extends PushRequest<List<GeoZone>> {

	private Location location;

	public GetNearestZoneRequest(Location location) {
		this.location = location;
	}

	public String getMethod() {
		return "getNearestZone";
	}

	@Override
	protected void buildParams(JSONObject params) throws JSONException {
		params.put("lat", location.getLatitude());
		params.put("lng", location.getLongitude());
		params.put("more", 1);
	}

	@Override
	public List<GeoZone> parseResponse(@NonNull JSONObject response) throws JSONException {
		List<GeoZone> result = new ArrayList<>();
		JSONArray zones = response.getJSONArray("geozones");

		if (zones == null) {
			return result;
		}

		for (int i = 0; i < zones.length(); i++) {
			JSONObject zone = zones.getJSONObject(i);

			GeoZone zoneLocation = new GeoZone(zone);
			result.add(zoneLocation);
		}

		return result;
	}
}
