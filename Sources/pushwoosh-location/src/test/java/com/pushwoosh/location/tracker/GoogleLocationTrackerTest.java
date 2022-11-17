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

package com.pushwoosh.location.tracker;

import android.location.Location;
import androidx.annotation.NonNull;
import androidx.core.util.Pair;

import com.google.android.gms.location.LocationRequest;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.internal.checker.LocationStateChecker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;

import java.util.List;

import static com.pushwoosh.location.internal.utils.LocationConfig.LOCATION_HIGH_INTERVAL;
import static com.pushwoosh.location.internal.utils.LocationConfig.LOCATION_LOW_INTERVAL;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoogleLocationTrackerTest {
	private GoogleLocationTracker googleLocationTracker;

	@Mock
	private LocationTrackerCallback locationTrackerCallback;
	@Mock
	private LocationUpdateListener locationUpdateListener;
	@Mock
	private GoogleLocationProvider locationProvider;
	@Mock
	private LocationStateChecker locationStateChecker;

	private Location location = createLocation();

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		googleLocationTracker = new GoogleLocationTracker(locationTrackerCallback, locationUpdateListener, locationProvider, locationStateChecker);


		//TODO: when(locationProvider.getLastLocation()).thenReturn(location);
	}

	@Test
	@Ignore // TODO
	public void onConnectedDoNotNeedRequestLocation() {
		googleLocationTracker.onConnected();

//		verify(locationProvider).getLastLocation();
//		verify(locationUpdateListener).locationUpdated(location);
//		verify(locationProvider, never()).updateLocationTracker(any());
	}

	@Test
	@Ignore //TODO
	public void onConnectedNeedRequestLocationUpdatesAndHighAccuracyIsTrue(){
		Whitebox.setInternalState(googleLocationTracker, "needRequestLocationUpdates", true);
		Whitebox.setInternalState(googleLocationTracker, "highAccuracy", true);

		googleLocationTracker.onConnected();

//		verify(locationProvider).getLastLocation();
		verify(locationUpdateListener).locationUpdated(location);
		checkHightPriorityRequest();
	}

	@Test
	@Ignore //TODO
	public void onConnectedIsNeedRequestLocationUpdates(){
		Whitebox.setInternalState(googleLocationTracker, "needRequestLocationUpdates", true);

		googleLocationTracker.onConnected();

//		verify(locationProvider).getLastLocation();
//		verify(locationUpdateListener).locationUpdated(location);
		checkLowPriorityRequest();
	}



	@Test
	public void locationUpdated() {
		googleLocationTracker.locationUpdated(location);

		verify(locationUpdateListener).locationUpdated(location);
	}

	@NonNull
	private Location createLocation() {
		Location location = Mockito.mock(Location.class);
		when(location.getLongitude()).thenReturn(1.5d);
		when(location.getLatitude()).thenReturn(1.3d);
		return location;
	}

	@Test
	public void requestLocationUpdatesLocationProviderNotConnected() {
		googleLocationTracker.requestLocationUpdates(true);

		verify(locationProvider).connect();
		Boolean needRequestLocationUpdates =
				(Boolean) Whitebox.getInternalState(googleLocationTracker, "needRequestLocationUpdates");
		Assert.assertEquals(true, needRequestLocationUpdates);
	}

	@Test
	public void requestLocationUpdatesLocationProviderConnectedHighAccuracyIsTrue() {
		when(locationProvider.isConnected()).thenReturn(true);

		googleLocationTracker.requestLocationUpdates(true);

		Boolean highAccuracy =
				(Boolean) Whitebox.getInternalState(googleLocationTracker, "highAccuracy");
		Assert.assertEquals(true, highAccuracy);

		checkHightPriorityRequest();
	}

	private void checkHightPriorityRequest() {
		ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor =
				ArgumentCaptor.forClass(LocationRequest.class);

		verify(locationProvider).updateLocationTracker(locationRequestArgumentCaptor.capture());
		LocationRequest locationRequest = locationRequestArgumentCaptor.getValue();

		Assert.assertEquals(LocationRequest.PRIORITY_HIGH_ACCURACY, locationRequest.getPriority());
		Assert.assertEquals(LOCATION_HIGH_INTERVAL, locationRequest.getInterval());
	}

	@Test
	public void requestLocationUpdatesLocationProviderConnectedHighAccuracyIsFalse() {
		when(locationProvider.isConnected()).thenReturn(true);

		googleLocationTracker.requestLocationUpdates(false);

		Boolean highAccuracy =
				(Boolean) Whitebox.getInternalState(googleLocationTracker, "highAccuracy");
		Assert.assertEquals(false, highAccuracy);

		checkLowPriorityRequest();
	}

	private void checkLowPriorityRequest() {
		ArgumentCaptor<LocationRequest> locationRequestArgumentCaptor =
				ArgumentCaptor.forClass(LocationRequest.class);

		verify(locationProvider).updateLocationTracker(locationRequestArgumentCaptor.capture());
		LocationRequest locationRequest = locationRequestArgumentCaptor.getValue();

		Assert.assertEquals(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, locationRequest.getPriority());
		Assert.assertEquals(LOCATION_LOW_INTERVAL, locationRequest.getInterval());
	}

	@Test
	public void getLocation() {
		googleLocationTracker.getLocation(locationResult -> Assert.assertEquals(location, locationResult));
	}

	@Test
	public void onDestroy() {
		googleLocationTracker.onDestroy();

		Boolean needRequestLocationUpdates =
				(Boolean) Whitebox.getInternalState(googleLocationTracker, "needRequestLocationUpdates");
		Assert.assertEquals(true, needRequestLocationUpdates);
		verify(locationProvider).cancel();
	}

	@Test
	public void isLocationAvailable() {
		when(locationProvider.isLocationAvailable()).thenReturn(false);
		when(locationStateChecker.check()).thenReturn(false);
		boolean result = googleLocationTracker.isLocationAvailable();
		Assert.assertFalse(result);

		when(locationProvider.isLocationAvailable()).thenReturn(true);
		when(locationStateChecker.check()).thenReturn(false);
		boolean result2 = googleLocationTracker.isLocationAvailable();
		Assert.assertTrue(result2);

		when(locationProvider.isLocationAvailable()).thenReturn(false);
		when(locationStateChecker.check()).thenReturn(true);
		boolean result3 = googleLocationTracker.isLocationAvailable();
		Assert.assertTrue(result3);

		when(locationProvider.isLocationAvailable()).thenReturn(true);
		when(locationStateChecker.check()).thenReturn(true);
		boolean result4 = googleLocationTracker.isLocationAvailable();
		Assert.assertTrue(result4);
	}

	@Test
	public void successRequestLocation() {
		googleLocationTracker.successRequestLocation();
		googleLocationTracker.successRequestLocation();

		Boolean notifyCallback =
				(Boolean) Whitebox.getInternalState(googleLocationTracker, "notifyCallback");
		Assert.assertEquals(true, notifyCallback);

		verify(locationTrackerCallback, times(1)).successProvidingLocation();

	}

	@Test
	public void failedRequestLocation() {
		googleLocationTracker.failedRequestLocation();
		googleLocationTracker.failedRequestLocation();

		Boolean notifyCallback =
				(Boolean) Whitebox.getInternalState(googleLocationTracker, "notifyCallback");
		Assert.assertEquals(true, notifyCallback);

		verify(locationTrackerCallback, times(1)).failedProvidingLocation();
	}

	@Test
	public void failedRequestLocationWasFirst() {
		googleLocationTracker.failedRequestLocation();
		googleLocationTracker.successRequestLocation();

		Boolean notifyCallback =
				(Boolean) Whitebox.getInternalState(googleLocationTracker, "notifyCallback");
		Assert.assertEquals(true, notifyCallback);

		verify(locationTrackerCallback).failedProvidingLocation();
		verify(locationTrackerCallback, never()).successProvidingLocation();
	}

	@Test
	public void successRequestLocationWasFirst() {
		googleLocationTracker.successRequestLocation();
		googleLocationTracker.failedRequestLocation();

		Boolean notifyCallback =
				(Boolean) Whitebox.getInternalState(googleLocationTracker, "notifyCallback");
		Assert.assertEquals(true, notifyCallback);

		verify(locationTrackerCallback, never()).failedProvidingLocation();
		verify(locationTrackerCallback).successProvidingLocation();
	}
}