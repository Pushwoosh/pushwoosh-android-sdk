//
// DeviceFeature2_5.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.arellomobile.android.push;

import android.content.Context;
import android.location.Location;

import com.arellomobile.android.push.data.PushZoneLocation;
import com.arellomobile.android.push.request.AppOpenRequest;
import com.arellomobile.android.push.request.AppRemovedRequest;
import com.arellomobile.android.push.request.ApplicationEventRequest;
import com.arellomobile.android.push.request.GetNearestZoneRequest;
import com.arellomobile.android.push.request.GetTagsRequest;
import com.arellomobile.android.push.request.MessageDeliveredRequest;
import com.arellomobile.android.push.request.PushStatRequest;
import com.arellomobile.android.push.request.RequestManager;
import com.arellomobile.android.push.request.SetTagsRequest;

import org.json.JSONArray;

import java.util.Map;

public class DeviceFeature2_5
{
	public static void sendPushStat(Context context, String hash) throws Exception
	{
		PushStatRequest request = new PushStatRequest(hash);
		RequestManager.sendRequest(context, request);
	}

	public static void sendGoalAchieved(Context context, String goal, Integer count) throws Exception
	{
		ApplicationEventRequest request = new ApplicationEventRequest(goal, count);
		RequestManager.sendRequest(context, request);
	}

	public static void sendAppOpen(Context context) throws Exception
	{
		AppOpenRequest request = new AppOpenRequest();
		RequestManager.sendRequest(context, request);
	}

	public static JSONArray sendTags(Context context, Map<String, Object> tags) throws Exception
	{
		SetTagsRequest request = new SetTagsRequest(tags);
		RequestManager.sendRequest(context, request);
		
		return request.getSkippedTags();
	}

	public static PushZoneLocation getNearestZone(Context context, Location location) throws Exception
	{
		GetNearestZoneRequest request = new GetNearestZoneRequest(location);
		RequestManager.sendRequest(context, request);
		
		return request.getNearestLocation();
	}

	public static void sendMessageDeliveryEvent(Context context, String hash) throws Exception
	{
		MessageDeliveredRequest request = new MessageDeliveredRequest(hash);
		RequestManager.sendRequest(context, request);
	}

	public static void sendAppRemovedData(Context context, String packageName) throws Exception
	{
		AppRemovedRequest request = new AppRemovedRequest(packageName);
		RequestManager.sendRequest(context, request);
	}

	public static Map<String, Object> getTags(Context context) throws Exception
	{
		GetTagsRequest request = new GetTagsRequest();
		RequestManager.sendRequest(context, request);

		return request.getTags();
	}
}
