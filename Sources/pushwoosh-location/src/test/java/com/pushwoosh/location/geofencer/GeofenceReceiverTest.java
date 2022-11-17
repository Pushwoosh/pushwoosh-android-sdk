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

package com.pushwoosh.location.geofencer;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.android.gms.location.Geofence;
import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class GeofenceReceiverTest {
	public static final String ZONE_1 = "zone 1";
	public static final String ZONE_2 = "zone 2";
	@Mock
	private GeofenceStateChangedCallback geofenceStateChangedCallback;

	private GeofenceReceiver geofenceReceiver;
	private Application application;

	@Before
	public void SetUp() {
		MockitoAnnotations.initMocks(this);
		application = RuntimeEnvironment.application;
		AndroidPlatformModule.init(application, true);

		geofenceReceiver = new GeofenceReceiver();
		Whitebox.setInternalState(geofenceReceiver, "geofenceStateChangedCallback", geofenceStateChangedCallback);
	}

	@Test
	@Ignore("TODO: broken after AndroidX migration")
	public void onReceive() {
		Intent intent = getItent("org.robolectric.default.action.PROCESS_UPDATES");

		geofenceReceiver.onReceive(application, intent);

		ArgumentCaptor<List<String>> listArgumentCaptor = ArgumentCaptor.forClass(List.class);

		verify(geofenceStateChangedCallback).onGeofenceStateChanged(listArgumentCaptor.capture(), eq(Geofence.GEOFENCE_TRANSITION_ENTER));
		List<String> idList = listArgumentCaptor.getValue();
	//	Assert.assertEquals(ZONE_1, idList.get(0));
	//	Assert.assertEquals(ZONE_2, idList.get(1));
	}

	@NonNull
	private Intent getItent(String action) {
		Bundle bundle = new Bundle();
		ArrayList<Geofence> geoZoneList = getGeofenceList();
		bundle.putSerializable("com.google.android.location.intent.extra.geofence_list", geoZoneList);
		bundle.putInt("com.google.android.location.intent.extra.transition", 1);
		Intent intent = new Intent();
		intent.setAction(action);
		intent.putExtras(bundle);
		return intent;
	}

	@NonNull
	private ArrayList<Geofence> getGeofenceList() {
		ArrayList<Geofence> geoZoneList = new ArrayList<>();
		//geoZoneList.add(buildGeofence(ZONE_1));
		//geoZoneList.add(buildGeofence(ZONE_2));
		return geoZoneList;
	}

	@NonNull
	private Geofence buildGeofence(String zoneName) {
		return new Geofence.Builder()
				.setRequestId(zoneName)
				.setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
				.setExpirationDuration(1L)
				.setCircularRegion(2.0,2.0, 3.0F)
				.setLoiteringDelay(1)
				.build();
	}

	@Test
	public void onReceiveIntentIsNull() {
		geofenceReceiver.onReceive(application, null);

		verify(geofenceStateChangedCallback, never())
				.onGeofenceStateChanged(anyList(), eq(Geofence.GEOFENCE_TRANSITION_ENTER));
	}

	@Test
	public void onReceiveIntentWithWrongAction(){
		Intent intent = getItent("123");

		geofenceReceiver.onReceive(application, intent);

		verify(geofenceStateChangedCallback, never())
				.onGeofenceStateChanged(anyList(), eq(Geofence.GEOFENCE_TRANSITION_ENTER));
	}
}