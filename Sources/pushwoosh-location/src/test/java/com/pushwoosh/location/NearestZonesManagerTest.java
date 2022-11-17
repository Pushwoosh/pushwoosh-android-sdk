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

package com.pushwoosh.location;

import android.location.Location;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.data.GeoZonesProvider;
import com.pushwoosh.location.geofence.GeofenceTracker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.BackgroundLocationPermissionDeclaredChecker;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;
import com.pushwoosh.location.internal.utils.LocationConfig;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.location.scheduler.ServiceScheduler;
import com.pushwoosh.location.storage.LocationPrefs;
import com.pushwoosh.location.tracker.LocationTracker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.reflection.Whitebox;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

import java.lang.ref.WeakReference;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class NearestZonesManagerTest {
	private NearestZonesManager nearestZonesManager;

	@Mock
	private GeofenceTracker geofenceTracker;
	@Mock
	private LocationPrefs locationPrefs;
	@Mock
	private LocationTracker locationTracker;
	@Mock
	private LocationPermissionChecker locationPermissionChecker;
	@Mock
	private FineLocationPermissionChecker fineLocationPermissionChecker;
	@Mock
	private BackgroundLocationPermissionDeclaredChecker backgroundLocationPermissionDeclaredChecker;
	@Mock
	private BackgroundLocationPermissionChecker backgroundLocationPermissionChecker;
	@Mock
	private ServiceScheduler serviceScheduler;

	@Mock
	PreferenceBooleanValue geolocationStarted;

	@Mock
	Callback<Void, LocationNotAvailableException> callback;

	@Captor
	ArgumentCaptor<Result<Void, LocationNotAvailableException>> callbackCaptor;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		when(locationPrefs.geolocationStarted()).thenReturn(geolocationStarted);

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
	public void requestUpdateGeoZonesAllCheckIsTrueAndLocationNotNull() {
		when(locationPermissionChecker.check()).thenReturn(true);
		when(geolocationStarted.get()).thenReturn(true);
		Location location = GeoZonesProvider.createLocation();
		//when(locationTracker.getLocation()).thenReturn(location);

		nearestZonesManager.requestUpdateGeoZones(cb -> {
			Assert.assertTrue(cb.getData());
			verify(serviceScheduler).requestUpdateNearestGeoZones();
			verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
		});
	}

	@Test
	public void requestUpdateGeoZonesAllCheckIsTrueButLocationIsNull() {
		when(locationPermissionChecker.check()).thenReturn(true);
		when(geolocationStarted.get()).thenReturn(true);

		nearestZonesManager.requestUpdateGeoZones(cb -> {
			Assert.assertFalse(cb.getData());
			verify(serviceScheduler, never()).requestUpdateNearestGeoZones();
			verify(locationTracker).requestLocationUpdates(false);
		});
	}

	@Test
	public void requestUpdateGeoZonesAllCheckIsTrueButLocationTrackerIsNull() {
		nearestZonesManager = new NearestZonesManager(
				geofenceTracker,
				locationPrefs,
				null,
				locationPermissionChecker,
				fineLocationPermissionChecker,
				backgroundLocationPermissionDeclaredChecker,
				backgroundLocationPermissionChecker,
				serviceScheduler);

		when(locationPermissionChecker.check()).thenReturn(true);
		when(geolocationStarted.get()).thenReturn(true);

		nearestZonesManager.requestUpdateGeoZones(cb -> Assert.assertFalse(cb.getData()));

		verify(serviceScheduler, never()).requestUpdateNearestGeoZones();
		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void requestUpdateGeoZonesPermissionCheckTrue() {
		when(locationPermissionChecker.check()).thenReturn(true);
		when(geolocationStarted.get()).thenReturn(false);

		nearestZonesManager.requestUpdateGeoZones(cb -> Assert.assertFalse(cb.getData()));

		verify(serviceScheduler, never()).requestUpdateNearestGeoZones();
		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void requestUpdateGeoZonesGeolocationStartedTrue() {
		when(locationPermissionChecker.check()).thenReturn(false);
		when(geolocationStarted.get()).thenReturn(true);

		nearestZonesManager.requestUpdateGeoZones(cb -> Assert.assertFalse(cb.getData()));

		verify(serviceScheduler, never()).requestUpdateNearestGeoZones();
		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void requestUpdateGeoZonesAllCheckFail() {
		when(locationPermissionChecker.check()).thenReturn(false);
		when(geolocationStarted.get()).thenReturn(false);

		nearestZonesManager.requestUpdateGeoZones(cb -> Assert.assertFalse(cb.getData()));

		verify(serviceScheduler, never()).requestUpdateNearestGeoZones();
		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void failedProvidingLocation() {
		nearestZonesManager.start(callback);

		nearestZonesManager.failedProvidingLocation();
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		checkStop();

		verify(callback, times(1)).process(callbackCaptor.capture());
		Assert.assertEquals("Location not found", callbackCaptor.getValue().getException().getMessage());
	}

	private void checkStop() {
		verify(geolocationStarted).set(false);
		verify(geofenceTracker).onDestroy();
		verify(locationTracker).onDestroy();
		verify(serviceScheduler).cancel();
		verify(serviceScheduler).requestLocationDisabled();
	}

	@Test
	public void successProvidingLocation() {
		nearestZonesManager.start(callback);

		nearestZonesManager.successProvidingLocation();
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		verify(callback, times(1)).process(callbackCaptor.capture());
		Assert.assertTrue(callbackCaptor.getValue().isSuccess());
	}

	@Test
	public void startLocationPermissionCheckerIsFalse() {
		when(locationPermissionChecker.check()).thenReturn(false);

		nearestZonesManager.start(callback);

		checkStart();
		verify(locationPermissionChecker).requestPermissions(LocationConfig.LOCATION_PERMISSIONS);
		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void startLocationPermissionCheckerIsTrue() {
		when(locationPermissionChecker.check()).thenReturn(true);

		nearestZonesManager.start(callback);

		checkStart();
		verify(locationPermissionChecker, never()).requestPermissions(any());
		verify(locationTracker).requestLocationUpdates(false);
	}

	@Test
	public void startLocationPermissionCheckerIsTrueLocationTrackerNull() {
		nearestZonesManager = new NearestZonesManager(geofenceTracker,
				locationPrefs,
				null,
				locationPermissionChecker,
				fineLocationPermissionChecker,
				backgroundLocationPermissionDeclaredChecker,
				backgroundLocationPermissionChecker,
				serviceScheduler);
		when(locationPermissionChecker.check()).thenReturn(true);

		nearestZonesManager.start(callback);

		checkStart();
		verify(locationPermissionChecker, never()).requestPermissions(any());
		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	private void checkStart() {
		Object callback = Whitebox.getInternalState(nearestZonesManager, "callback");
		WeakReference<Callback<Void, LocationNotAvailableException>> weakReference
				= (WeakReference<Callback<Void, LocationNotAvailableException>>) callback;
		Assert.assertEquals(this.callback, weakReference.get());
		verify(geolocationStarted).set(true);
		verify(geofenceTracker).startTracking();
	}

	@Test
	public void stop() {
		nearestZonesManager.stop();

		checkStop();
	}

	@Test
	public void updateZones() {
		Location location = GeoZonesProvider.createLocation();
		List<GeoZone> geoZoneList = GeoZonesProvider.generateGeoZones(3, 3, location);
		nearestZonesManager.updateZones(location, geoZoneList);
	}

	@Test
	public void updateState() {
		nearestZonesManager.updateState(true);
		verify(serviceScheduler).scheduleNearestGeoZones(true);
	}

	@Test
	public void updateStateFalse() {
		nearestZonesManager.updateState(false);
		verify(serviceScheduler).scheduleNearestGeoZones(false);
	}

	@Test
	public void deviceRebooted() {
		nearestZonesManager.deviceRebooted();
		verify(serviceScheduler).deviceRebooted();
	}

	@Test
	public void requestLocationGeolocationStartedFalse() {
		when(geolocationStarted.get()).thenReturn(false);

		nearestZonesManager.requestLocation();

		verify(locationPermissionChecker, never()).requestPermissions(any());
		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void requestLocationGeolocationStartedTruePermissionCheckFalse() {
		when(geolocationStarted.get()).thenReturn(true);
		when(locationPermissionChecker.check()).thenReturn(false);

		nearestZonesManager.requestLocation();

		verify(locationPermissionChecker).requestPermissions(LocationConfig.LOCATION_PERMISSIONS);
		verify(locationTracker, never()).requestLocationUpdates(anyBoolean());
	}

	@Test
	public void requestLocationGeolocationStartedTruePermissionCheckTrue() {
		when(geolocationStarted.get()).thenReturn(true);
		when(locationPermissionChecker.check()).thenReturn(true);

		nearestZonesManager.requestLocation();

		verify(locationPermissionChecker, never()).requestPermissions(any());
		verify(locationTracker).requestLocationUpdates(false);
	}
}