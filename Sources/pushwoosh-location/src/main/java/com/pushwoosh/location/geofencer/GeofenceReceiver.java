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

package com.pushwoosh.location.geofencer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.internal.LocationModule;
import com.pushwoosh.location.internal.utils.LocationConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GeofenceReceiver extends BroadcastReceiver {
	private static final String SUB_TAG = "[GeofenceReceiver]";

	private GeofenceStateChangedCallback geofenceStateChangedCallback = LocationModule.geofenceStateChangedCallback();
	public static final String ACTION_PROCESS_UPDATES = "%s.action.PROCESS_UPDATES";

	public static String getGeofenceAction(Context context) {
		return String.format(ACTION_PROCESS_UPDATES, context.getPackageName());
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			handleReceive(context, intent);
		} catch (Exception e) {
			PWLog.error(LocationConfig.TAG, SUB_TAG + "Failed to handle geofence event", e);
		}
	}

	private void handleReceive(Context context, Intent intent) {
		PWLog.noise(LocationConfig.TAG, SUB_TAG + "onReceive");
		if (intent != null) {
			final String action = intent.getAction();
			if (getGeofenceAction(context).equals(action)) {
				AndroidPlatformModule.init(context.getApplicationContext());
				onHandleIntent(intent);
			} else {
				PWLog.noise(LocationConfig.TAG, SUB_TAG + "wrong action:" + action);
			}
		} else {
			PWLog.noise(LocationConfig.TAG, SUB_TAG + "intent is null");
		}
	}

	protected void onHandleIntent(Intent intent) {
		GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

		if (geofencingEvent == null) {
			return;
		}
		if (geofencingEvent.hasError()) {
			PWLog.error(LocationConfig.TAG, SUB_TAG + "GeofencingEvent error occurred with errorCode :" + geofencingEvent.getErrorCode());
			return;
		}

		// Get the transition type.
		int geofenceTransition = geofencingEvent.getGeofenceTransition();

		// Test that the reported transition was of interest.
		if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
			List<String> ids = new ArrayList<>();

			//noinspection Convert2streamapi
			if (geofencingEvent.getTriggeringGeofences() != null){
				for (Geofence geofence : geofencingEvent.getTriggeringGeofences()) {
					ids.add(geofence.getRequestId());
				}
			}

			PWLog.noise(LocationConfig.TAG, SUB_TAG + (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ? "Enter to " : "Exit from ") + " geoZones with ids " + Arrays.toString(ids.toArray()));

			geofenceStateChangedCallback.onGeofenceStateChanged(ids, geofenceTransition);
		}
	}
}