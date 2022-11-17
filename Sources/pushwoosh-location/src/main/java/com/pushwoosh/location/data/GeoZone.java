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

package com.pushwoosh.location.data;

import java.io.Serializable;
import java.util.List;

import android.location.Location;
import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Date: 03-Sep-15
 * Time: 17:53
 *
 * @author Alexander Blinov
 */
public class GeoZone implements Comparable<GeoZone>, Serializable {
	private static final long serialVersionUID = -1621161233854684600L;

	private static final String EXTRA_ZONE_TAG = "_extra";
	private static final String RADIUS_ZONE_NAME = "RADIUS_ZONE";

	private static final String NAME = "name";
	private static final String LAT = "lat";
	private static final String LNG = "lng";
	private static final String RANGE = "range";
	private static final String DISTANCE = "distance";

	private String id;
	private String mName;
	private double lat;
	private double lng;
	private long mRange;
	private long mDistance;

	private transient Location location;

	public GeoZone(JSONObject zone) throws JSONException {
		this(zone.getString(NAME), zone.getDouble(LAT), zone.getDouble(LNG), zone.getLong(RANGE), zone.getLong(DISTANCE));
	}

	public GeoZone(GeoZone zone, long extraRange) {
		this(zone.getName() + EXTRA_ZONE_TAG, zone.getLat(), zone.getLng(), zone.getRange() + extraRange, 0);
	}

	public GeoZone(String name, double lat, double lng, long range, long distance) {
		this(name, locationOf(lat, lng), range, distance);
		//bug with mocking location need for tests
		this.lat = lat;
		this.lng = lng;
	}

	GeoZone(String name, Location location, long range, long distance) {
		mName = name;
		this.lat = location.getLatitude();
		this.lng = location.getLongitude();
		mRange = range;
		mDistance = distance;
		this.location = location;
		generateId();
	}

	private void generateId() {
		id = getName() + "_" + String.valueOf(getLat()) + "_" + String.valueOf(getLng());
	}

	private static Location locationOf(double lat, double lng) {
		Location location = new Location("");
		location.setLatitude(lat);
		location.setLongitude(lng);
		return location;
	}

	@NonNull
	private Location getLocation(){
		if (location == null){
			location = locationOf(lat, lng);
		}

		return location;
	}

	public String getName() {
		return mName;
	}

	public double getLat() {
		return lat;
	}

	public double getLng() {
		return lng;
	}

	public long getRange() {
		return mRange;
	}

	public long getDistance() {
		return mDistance;
	}

	public static GeoZone createRadiusZone(List<GeoZone> geoZones, Location currentLocation, int minRadius) {
		int max = minRadius;

		for (GeoZone trackingZone : geoZones) {
			int distance = trackingZone.distanceTo(currentLocation);

			if (max < distance) {
				max = distance;
			}
		}

		return GeoZone.getRadiusZone(currentLocation, max);
	}

	private static GeoZone getRadiusZone(Location location, long range) {
		if (location != null) {
			return new GeoZone(RADIUS_ZONE_NAME + range + location.getLatitude() + location.getLongitude(), location, range, 0);
		}
		return null;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final GeoZone geoZone = (GeoZone) o;

		return id != null ? id.equals(geoZone.id) : geoZone.id == null;

	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}

	public boolean contains(Location location) {
		//noinspection SimplifiableIfStatement
		if (location == null) {
			return true;
		}

		return mRange > getLocation().distanceTo(location);
	}

	public int distanceTo(Location location) {
		if (location == null) {
			return 0;
		}
		return (int) getLocation().distanceTo(location);
	}

	@Override
	public String toString() {
		return mName;
	}

	@Override
	public int compareTo(@NonNull final GeoZone geoZone) {
		return geoZone.equals(this) ? 0 : Long.valueOf(mDistance).compareTo(geoZone.getDistance());
	}
}
