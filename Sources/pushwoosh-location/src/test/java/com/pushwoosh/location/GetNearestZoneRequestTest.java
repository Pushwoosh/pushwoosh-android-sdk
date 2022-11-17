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

package com.pushwoosh.location;

import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.network.data.GetNearestZoneRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(CustomRobolectricTestRunner.class)
public class GetNearestZoneRequestTest {



    @Test
    public void testGetMethod() throws Exception {
        GetNearestZoneRequest request = new GetNearestZoneRequest(null);
        assertEquals("getNearestZone", request.getMethod());
    }

    @Test
    public void testParseResponse() throws Exception {
        GetNearestZoneRequest request = new GetNearestZoneRequest(null);

        JSONObject response = new JSONObject("{ \"response\" : { \"geozones\":[{\"name\":\"Technopark\",\"lat\":54.858,\"lng\":83.1103,\"range\":100,\"distance\":18},{\"name\":\"yet another geozone\",\"lat\":54.85824,\"lng\":83.11087,\"range\":132,\"distance\":61}] } }");
        List<GeoZone> locations = parseResponse(request, response);

        assertEquals(2, locations.size());

        GeoZone location1 = locations.get(0);
        assertEquals("Technopark", location1.getName());
        assertEquals(54.858, location1.getLat(), 0.001);
        assertEquals(83.1103, location1.getLng(), 0.001);
        assertEquals(100, location1.getRange());
        assertEquals(18, location1.getDistance());

        GeoZone location2 = locations.get(1);
        assertEquals("yet another geozone", location2.getName());
        assertEquals(54.85824, location2.getLat(), 0.001);
        assertEquals(83.11087, location2.getLng(), 0.001);
        assertEquals(132, location2.getRange());
        assertEquals(61, location2.getDistance());
    }

    private List<GeoZone> parseResponse(GetNearestZoneRequest request, final JSONObject response) throws JSONException {
        return request.parseResponse(response.optJSONObject("response"));
    }

    @Test
    public void testEmptyZones() throws Exception {
        GetNearestZoneRequest request = new GetNearestZoneRequest(null);

        JSONObject response = new JSONObject("{ \"response\" : { \"geozones\":[] } }");
        List<GeoZone> locations = parseResponse(request, response);

        assertEquals(Collections.<GeoZone>emptyList(), locations);
    }

    @Test(expected = JSONException.class)
    public void testNoGeozones() throws Exception {
        GetNearestZoneRequest request = new GetNearestZoneRequest(null);

        JSONObject response = new JSONObject("{ \"response\" : { } }");

        List<GeoZone> locations = parseResponse(request, response);
    }

    @Test(expected = JSONException.class)
    public void testBadResponseType() throws Exception {
        GetNearestZoneRequest request = new GetNearestZoneRequest(null);

        JSONObject response = new JSONObject("{ \"response\" : {  \"geozones\":{} } }");

        List<GeoZone> locations = parseResponse(request, response);
    }

    @Test(expected = JSONException.class)
    public void testBadZoneType() throws Exception {
        GetNearestZoneRequest request = new GetNearestZoneRequest(null);

        JSONObject response = new JSONObject("{ \"response\" : {  \"geozones\":[{\"name\":null,\"lat\":{},\"lng\":83.1103,\"range\":100,\"distance\":18}] }, [], 11 }");

        List<GeoZone> locations = parseResponse(request, response);
    }

    @Test(expected = JSONException.class)
    public void testBadZone() throws Exception {
        GetNearestZoneRequest request = new GetNearestZoneRequest(null);

        JSONObject response = new JSONObject("{ \"response\" : {  \"geozones\":[{\"name\":null,\"lat\":{},\"lng\":83.1103,\"range\":100,\"distance\":18}] }}");

        List<GeoZone> locations = parseResponse(request, response);

        GeoZone location1 = locations.get(0);
        double lat = location1.getLat(); // crash
        double lng = location1.getLng(); // crash
    }
}
