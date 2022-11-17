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

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.location.internal.event.LocationPermissionEvent;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.location.geofence.GeofenceTracker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionDeclaredChecker;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.location.scheduler.ServiceScheduler;
import com.pushwoosh.location.storage.LocationPrefs;
import com.pushwoosh.location.storage.TestLocationPrefs;
import com.pushwoosh.location.tracker.LocationTracker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricTestRunner.class)
public class NearerstZonesManagerTest {

	@Mock
	GeofenceTracker geofenceTracker;
	@Mock
	LocationPrefs locationPrefs;
	@Mock
	LocationTracker locationTracker;
	@Mock
	LocationPermissionChecker locationPermissionChecker;
	@Mock
	FineLocationPermissionChecker fineLocationPermissionChecker;
	@Mock
	BackgroundLocationPermissionDeclaredChecker backgroundLocationPermissionDeclaredChecker;
	@Mock
	BackgroundLocationPermissionChecker backgroundLocationPermissionChecker;
	@Mock
	ServiceScheduler serviceScheduler;
	@Mock
	PreferenceBooleanValue geoLocationPrefs;
	@Mock
	Callback<Void, LocationNotAvailableException> callback;

	private NearestZonesManager nearestZonesManager;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
		TestLocationPrefs.mockLocationPrefs(locationPrefs, geoLocationPrefs);
		nearestZonesManager = new NearestZonesManager(
				geofenceTracker,
				locationPrefs,
				locationTracker,
				locationPermissionChecker,
				fineLocationPermissionChecker,
				backgroundLocationPermissionDeclaredChecker,
				backgroundLocationPermissionChecker,
				serviceScheduler);
	}

	@Test
	public void start_RequestPermissionWithoutPermissionTest() {
		when(locationPermissionChecker.check()).thenReturn(false);

		nearestZonesManager.start(null);

		verify(locationPermissionChecker).requestPermissions(LocationConfig.LOCATION_PERMISSIONS);
	}

	@Test
	public void start_NotRequestLocationWithoutPermissionTest() {
		when(locationPermissionChecker.check()).thenReturn(false);

		nearestZonesManager.start(null);

		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void start_RequestLocationAfterSuccessPermissionTest() throws Exception {
		when(locationPermissionChecker.check()).thenReturn(false);

		Answer<Void> sendEventAnswer = invocation -> {
			when(locationPermissionChecker.check()).thenReturn(true);
			EventBus.sendEvent(new LocationPermissionEvent(Arrays.asList(LocationConfig.LOCATION_PERMISSIONS), null));
			return null;
		};

		doAnswer(sendEventAnswer).when(locationPermissionChecker).requestPermissions(LocationConfig.LOCATION_PERMISSIONS);

		nearestZonesManager.start(callback);

		//check
		verify(locationTracker, timeout(500).times(1)).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void start_StopIfPermissionDeclinedTest() throws Exception {
		when(locationPermissionChecker.check()).thenReturn(false);

		Answer<Void> sendEventAnswer = invocation -> {
			when(locationPermissionChecker.check()).thenReturn(false);
			EventBus.sendEvent(new LocationPermissionEvent(Arrays.asList(LocationConfig.LOCATION_PERMISSIONS), null));
			return null;
		};

		doAnswer(sendEventAnswer).when(locationPermissionChecker).requestPermissions(LocationConfig.LOCATION_PERMISSIONS);

		nearestZonesManager.start(callback);

		//check
		verify(locationTracker, after(500).never()).requestLocationUpdates(anyBoolean());
		verify(callback).process(Result.fromException(any(LocationNotAvailableException.class)));
		verify(geoLocationPrefs).set(false);
	}

	@Test
	public void start_RequestLocationWithPermissionTest() {
		when(locationPermissionChecker.check()).thenReturn(true);

		nearestZonesManager.start(null);

		verify(locationTracker).requestLocationUpdates(false);
	}

	@Test
	public void start_EnableLocationIntoPrefs(){
		nearestZonesManager.start(null);

		verify(geoLocationPrefs).set(true);
	}

	@Test
	public void stop_DisableLocationIntoPrefs(){
		nearestZonesManager.stop();

		verify(geoLocationPrefs).set(false);
	}

	@Test
	public void stop_GeofenceTrackerStop(){
		nearestZonesManager.stop();

		verify(geofenceTracker).onDestroy();
	}

	@Test
	public void stop_LocationTrackerStop(){
		nearestZonesManager.stop();

		verify(locationTracker).onDestroy();
	}

	@Test
	public void stop_ServiceSchedulerStop(){
		nearestZonesManager.stop();

		verify(serviceScheduler).cancel();
	}
}
