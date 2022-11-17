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

package com.pushwoosh.location.network.data;

import android.location.Location;

import com.pushwoosh.location.data.GeoZone;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class GetNearestZoneRequestTest {

    public static final double LONGITUDE = 1.2;
    public static final double LATITUDE = 1.3;
    private GetNearestZoneRequest getNearestZoneRequest;
    @Mock
    private Location location;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        getNearestZoneRequest = new GetNearestZoneRequest(location);

        when(location.getLongitude()).thenReturn(LONGITUDE);
        when(location.getLatitude()).thenReturn(LATITUDE);
    }

    @Test
    public void buildParams() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        getNearestZoneRequest.buildParams(jsonObject);

        Assert.assertEquals(LATITUDE, jsonObject.getDouble("lat"), 0);
        Assert.assertEquals(LONGITUDE, jsonObject.getDouble("lng"), 0);
        Assert.assertEquals(1, jsonObject.getInt("more"));
    }

    @Test
    public void parseResponse() throws JSONException {
        String json = "{geozones:[{\"name\": \"geo zone name\", \"lat\":1.2, \"lng\": 1.3, \"range\":10, \"distance\":100},{\"name\": \"geo zone name 2\", \"lat\":1.4, \"lng\": 1.5, \"range\":104, \"distance\":1040}]}";
        JSONObject response = new JSONObject(json);
        List<GeoZone> geoZoneList = getNearestZoneRequest.parseResponse(response);

        Assert.assertEquals(2, geoZoneList.size());
        GeoZone geoZone = geoZoneList.get(0);
        Assert.assertEquals("geo zone name", geoZone.getName());
        Assert.assertEquals(1.2d, geoZone.getLat(), 0);
        Assert.assertEquals(1.3d, geoZone.getLng(),0 );
        Assert.assertEquals(10L, geoZone.getRange());
        Assert.assertEquals(100L, geoZone.getDistance());

        GeoZone geoZone2 = geoZoneList.get(1);
        Assert.assertEquals("geo zone name 2", geoZone2.getName());
        Assert.assertEquals(1.4d, geoZone2.getLat(), 0);
        Assert.assertEquals(1.5d, geoZone2.getLng(),0 );
        Assert.assertEquals(104L, geoZone2.getRange());
        Assert.assertEquals(1040L, geoZone2.getDistance());
    }
}