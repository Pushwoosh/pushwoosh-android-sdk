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

import android.location.Location;
import androidx.annotation.NonNull;

import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GeoZonesProvider {

	public static final int DEFAULT_RANGE = 100;

	public static List<GeoZone> generateGeoZones(int count, int offset) {
		return generateGeoZones(count, offset, createMockLocation());
	}

	public static List<GeoZone> generateGeoZones(int count, int offset, Location location) {
		List<GeoZone> geoZones = new ArrayList<>();

		for (int i = offset; i < count + offset; i++) {
			geoZones.add(new GeoZone(getName(i), location, DEFAULT_RANGE, new Random().nextInt(100000)));
		}

		return geoZones;
	}

	@NonNull
	private static String getName(final int i) {
		return "test" + i;
	}

	public static List<String> generateGeoZonesIds(int count, int offset) {
		List<String> ids = new ArrayList<>();

		for (int i = offset; i < count + offset; i++) {
			ids.add(getName(i));
		}

		return ids;
	}

	private static Location createMockLocation() {
		Location location = mock(Location.class);
		when(location.getLongitude()).thenReturn(0d);
		when(location.getLatitude()).thenReturn(0d);
		return location;
	}

	@NonNull
	public static Location createLocation() {
		Location location = Mockito.mock(Location.class);
		when(location.getLongitude()).thenReturn(1.5d);
		when(location.getLatitude()).thenReturn(1.3d);
		return location;
	}
}
