/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.List;

import static com.pushwoosh.location.data.GeoZonesProvider.createLocation;
import static org.mockito.Mockito.when;

public class GeoZoneTest {

	@Test
	public void createRadiusZone() {
		List<GeoZone> geoZoneList = GeoZonesProvider.generateGeoZones(5, 5);
		Location location = createLocation();


		GeoZone radiusZone = GeoZone.createRadiusZone(geoZoneList, location, 50);

		Assert.assertEquals(1.5d, radiusZone.getLng(), 0);
		Assert.assertEquals(1.3d, radiusZone.getLat(), 0);
		Assert.assertEquals(0, radiusZone.getDistance());
		Assert.assertEquals(50, radiusZone.getRange());
		Assert.assertEquals("RADIUS_ZONE501.31.5", radiusZone.getName());
	}

	@Test
	public void createRadiusZoneWhitMaxDistanceFromGeoZone(){
		List<GeoZone> geoZoneList = GeoZonesProvider.generateGeoZones(5, 5);
		Location location = createLocation();

		GeoZone geoZone = geoZoneList.get(0);
		Location locationGeoZone = (Location) Whitebox.getInternalState(geoZone, "location");
		when(locationGeoZone.distanceTo(location)).thenReturn(100f);

		GeoZone radiusZone2 = GeoZone.createRadiusZone(geoZoneList, location, 50);

		Assert.assertEquals(1.5d, radiusZone2.getLng(), 0);
		Assert.assertEquals(1.3d, radiusZone2.getLat(), 0);
		Assert.assertEquals(0, radiusZone2.getDistance());
		Assert.assertEquals(100, radiusZone2.getRange());
		Assert.assertEquals("RADIUS_ZONE1001.31.5", radiusZone2.getName());
	}

	@Test
	public void distanceTo() {
		List<GeoZone> geoZoneList = GeoZonesProvider.generateGeoZones(5, 5);
		Location location = new Location("");
		GeoZone geoZone = geoZoneList.get(0);
		Location locationGeoZone = (Location) Whitebox.getInternalState(geoZone, "location");
		float distance = 33.0F;
		when(locationGeoZone.distanceTo(location)).thenReturn(distance);

		int result = geoZone.distanceTo(location);

		Assert.assertEquals(distance, result, 0);

	}

}