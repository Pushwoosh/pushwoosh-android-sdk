package com.arellomobile.android.push.request;

import android.content.Context;
import android.location.Location;

import com.arellomobile.android.push.data.PushZoneLocation;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class GetNearestZoneRequest extends PushRequest {
	
	private Location location;
	private PushZoneLocation zoneLocation;
	
	public GetNearestZoneRequest(Location location) {
		this.location = location;
	}

	public String getMethod() {
		return "getNearestZone";
	}

	@Override
	protected void buildParams(Context context, Map<String, Object> params)
	{
		params.put("lat", location.getLatitude());
		params.put("lng", location.getLongitude());
	}
	
	@Override
	public void parseResponse(JSONObject response) throws JSONException {
		JSONObject jsonResp = response.getJSONObject("response");

		zoneLocation = new PushZoneLocation();

		zoneLocation.setName(jsonResp.getString("name"));
		zoneLocation.setLat(jsonResp.getDouble("lat"));
		zoneLocation.setLng(jsonResp.getDouble("lng"));
		zoneLocation.setDistanceTo(jsonResp.getLong("distance"));
	}	
	
	public PushZoneLocation getNearestLocation() {
		return zoneLocation;
	}
}
