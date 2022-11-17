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

package com.pushwoosh.location.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.internal.LocationModule;
import com.pushwoosh.location.internal.utils.LocationConfig;

/**
 * Service for updating by schedule nearest zones. If forceUpdate is true than it request nearest zones
 * from service otherwise it get nearest zones from storage
 */
public class GeoLocationServiceApi16 extends IntentService {
	private static final String SUB_TAG = "[GeoLocationServiceApi]";
	public static final String KEY_FORCE_UPDATE = SUB_TAG + "KEY_FORCE_UPDATE";
	public static final String KEY_TYPE = SUB_TAG + ".key_TYPE";

	private static final int GET_NEAREST_TYPE = 0;
	private static final int DISABLE_LOCATION_TYPE = 1;

	private GetNearestZoneJobApplier jobApplayer;

	public GeoLocationServiceApi16() {
		super("GeoLocationServiceApi16");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		AndroidPlatformModule.init(getApplicationContext());
		jobApplayer = new GetNearestZoneJobApplier(
				LocationModule.nearestZonesManager(),
				LocationModule.updateNearestRepository(),
				LocationModule.locationTracker());
	}

	public static Intent createGetNearestIntent(@NonNull Context context, boolean forceUpdate) {
		Intent intent = new Intent(context, GeoLocationServiceApi16.class);
		intent.putExtra(KEY_FORCE_UPDATE, forceUpdate);
		intent.putExtra(KEY_TYPE, GET_NEAREST_TYPE);
		return intent;
	}

	public static Intent createLocationDisableIntent(@NonNull Context context) {
		Intent intent = new Intent(context, GeoLocationServiceApi16.class);
		intent.putExtra(KEY_TYPE, DISABLE_LOCATION_TYPE);
		return intent;
	}

	@Override
	public void onDestroy() {
		jobApplayer.cancel();
		PWLog.noise(LocationConfig.TAG, SUB_TAG + " destroy nearest geoZones service");
		super.onDestroy();
	}

	@Override
	protected void onHandleIntent(@Nullable Intent intent) {
		if (intent == null) {
			return;
		}
		switch (intent.getIntExtra(KEY_TYPE, -1)){
			case GET_NEAREST_TYPE:
				jobApplayer.loadNearestGeoZones(intent.getBooleanExtra(KEY_FORCE_UPDATE, false));
				break;
			case DISABLE_LOCATION_TYPE:
				jobApplayer.locationDisable();
				break;
		}
	}
}
