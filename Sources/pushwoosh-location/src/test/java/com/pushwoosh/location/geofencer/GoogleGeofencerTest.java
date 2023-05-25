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

import android.app.PendingIntent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.tasks.Task;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.event.Subscription;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.internal.checker.GoogleApiChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;

import org.junit.Assert;
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
import org.robolectric.shadows.ShadowLooper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//TODO Disabling these tests until we update robolectric to 4.9+ due to
// https://github.com/robolectric/robolectric/issues/7269
@RunWith(RobolectricTestRunner.class)
public class GoogleGeofencerTest {
	public static final String GEO_ZONE_1 = "geo zone 1";
	public static final String GEO_ZONE_2 = "geo zone 2";
	private GoogleGeofencer googleGeofencer;

	@Mock
	private GoogleApiChecker googleApiChecker;
	@Mock
	private GoogleApiClient googleApiClient;
	@Mock
	private GeofencingClient geofencingClient;
	@Mock
	private PendingIntent pendingIntent;
	@Mock
	Task<Void> taskMock;
	@Mock
	private LocationPermissionChecker locationPermissionChecker;
	private GeoZone geoZone1;
	private GeoZone geoZone2;


	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		AndroidPlatformModule.init(RuntimeEnvironment.application, true);
		createGoogleGeofencerWithMockedComponents(geofencingClient, pendingIntent);

		when(googleApiChecker.check()).thenReturn(true);
		when(locationPermissionChecker.check()).thenReturn(true);

		geoZone1 = new GeoZone(GEO_ZONE_1, 1.2d, 2.2d, 1L, 2L);
		geoZone2 = new GeoZone(GEO_ZONE_2, 1.5d, 24.2d, 2L, 20L);

		when(geofencingClient.addGeofences(any(), eq(pendingIntent))).thenReturn(taskMock);
		when(geofencingClient.removeGeofences(any(List.class))).thenReturn(taskMock);
	}

	private void createGoogleGeofencerWithMockedComponents(GeofencingClient geofencingClient, PendingIntent pendingIntent) {
		googleGeofencer = new GoogleGeofencer(
				RuntimeEnvironment.application,
				locationPermissionChecker,
				geofencingClient,
				pendingIntent);
		Whitebox.setInternalState(googleGeofencer, "googleApiChecker", googleApiChecker);
		Whitebox.setInternalState(googleGeofencer, "googleApiClient", googleApiClient);
	}

	@Ignore
	@Test
	public void addZones() {
		List<GeoZone> geoZoneList = getGeoZoneList();

		googleGeofencer.addZones(geoZoneList);

		ArgumentCaptor<GeofencingRequest> geofencingRequestArgumentCaptor = ArgumentCaptor.forClass(GeofencingRequest.class);
		verify(geofencingClient).addGeofences(geofencingRequestArgumentCaptor.capture(), eq(pendingIntent));
		GeofencingRequest geofencingRequest = geofencingRequestArgumentCaptor.getValue();
		Assert.assertEquals(1, geofencingRequestArgumentCaptor.getAllValues().size());
		List<Geofence> geofences = geofencingRequest.getGeofences();
		Assert.assertEquals(GEO_ZONE_1, geofences.get(0).getRequestId());
		Assert.assertEquals(GEO_ZONE_2, geofences.get(1).getRequestId());
		Assert.assertEquals(2, geofences.size());
		verify(taskMock).addOnCompleteListener(any());
	}

	@NonNull
	private List<GeoZone> getGeoZoneList() {
		List<GeoZone> geoZoneList = new ArrayList<>();
		geoZoneList.add(geoZone1);
		geoZoneList.add(geoZone2);
		return geoZoneList;
	}

	@Ignore
	@Test
	public void addZonesEmptyListZone() {
		List<GeoZone> geoZoneList = new ArrayList<>();
		googleGeofencer.addZones(geoZoneList);

		checkNeverCallAddZones();
	}

	private void checkNeverCallAddZones() {
		verify(geofencingClient, never()).addGeofences(any(), eq(pendingIntent));
		verify(taskMock, never()).addOnCompleteListener(any());
	}

	@Ignore
	@Test
	public void addZonesCheckFailLocationPermissionChecker() {
		List<GeoZone> geoZoneList = getGeoZoneList();
		when(locationPermissionChecker.check()).thenReturn(false);
		googleGeofencer.addZones(geoZoneList);

		checkNeverCallAddZones();
	}

	@Ignore
	@Test
	public void addZonesCheckFailGoogleApiChecker() {
		List<GeoZone> geoZoneList = getGeoZoneList();
		when(googleApiChecker.check()).thenReturn(false);
		googleGeofencer.addZones(geoZoneList);

		checkNeverCallAddZones();
	}

	@Ignore
	@Test
	public void addZonedPendingIntentIsNull() {
		createGoogleGeofencerWithMockedComponents(geofencingClient, null);

		List<GeoZone> geoZoneList = getGeoZoneList();
		googleGeofencer.addZones(geoZoneList);

		checkNeverCallAddZones();
	}

	@Ignore
	@Test
	public void addZonedGeofencingClientIsNull() {
		createGoogleGeofencerWithMockedComponents(null, pendingIntent);

		List<GeoZone> geoZoneList = getGeoZoneList();
		googleGeofencer.addZones(geoZoneList);

		checkNeverCallAddZones();
	}

	@Ignore
	@Test
	public void removeZones() {
		List<GeoZone> geoZoneList = getGeoZoneList();
		List<String> idList = new ArrayList<>();
		idList.add(GEO_ZONE_1);
		idList.add(GEO_ZONE_2);

		googleGeofencer.removeZones(geoZoneList);

		verify(geofencingClient).removeGeofences(idList);
		verify(taskMock).addOnCompleteListener(any());
	}

	@Ignore
	@Test
	public void removeZonesEmptyList() {
		List<GeoZone> geoZoneList = new ArrayList<>();

		googleGeofencer.removeZones(geoZoneList);

		checkNeverCallRemoveZone();
	}

	private void checkNeverCallRemoveZone() {
		verify(geofencingClient, never()).removeGeofences(any(List.class));
		verify(taskMock, never()).addOnCompleteListener(any());
	}

	@Ignore
	@Test
	public void removeZonesCheckFailGoogleApiChecker() {
		List<GeoZone> geoZoneList = getGeoZoneList();
		when(googleApiChecker.check()).thenReturn(false);
		googleGeofencer.removeZones(geoZoneList);

		checkNeverCallRemoveZone();
	}

	@Ignore
	@Test
	public void removeZonedGeofencingClientIsNull() {
		createGoogleGeofencerWithMockedComponents(null, pendingIntent);

		List<GeoZone> geoZoneList = getGeoZoneList();
		googleGeofencer.removeZones(geoZoneList);

		checkNeverCallRemoveZone();
	}

	@Ignore
	@Test
	public void onDestroy() {
		googleGeofencer.onDestroy();
		verify(googleApiClient, never()).disconnect();

		when(googleApiClient.isConnected()).thenReturn(true);
		googleGeofencer.onDestroy();
		verify(googleApiClient).disconnect();
	}

	@Ignore
	@Test
	public void connect() {
		when(googleApiClient.isConnected()).thenReturn(true);
		when(googleApiClient.isConnecting()).thenReturn(true);
		googleGeofencer.connect();
		verify(googleApiClient, never()).connect();

		when(googleApiClient.isConnected()).thenReturn(false);
		when(googleApiClient.isConnecting()).thenReturn(true);
		googleGeofencer.connect();
		verify(googleApiClient, never()).connect();

		when(googleApiClient.isConnected()).thenReturn(true);
		when(googleApiClient.isConnecting()).thenReturn(false);
		googleGeofencer.connect();
		verify(googleApiClient, never()).connect();

		when(googleApiClient.isConnected()).thenReturn(false);
		when(googleApiClient.isConnecting()).thenReturn(false);
		googleGeofencer.connect();
		verify(googleApiClient).connect();
	}

	@Ignore
	@Test
	public void onConnectionSuspendedMustReconnect() {
		googleGeofencer.onConnectionSuspended(0);
		verify(googleApiClient).reconnect();
	}

	@Ignore
	@Test(timeout = 4000)
	public void onConnectedMustSendEvent() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Subscription<GoogleGeofencer.GoogleGeofencerConnectedEvent> subsriber =
				EventBus.subscribe(GoogleGeofencer.GoogleGeofencerConnectedEvent.class, event -> countDownLatch.countDown());

		googleGeofencer.onConnected(new Bundle());
		ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

		countDownLatch.await();
		subsriber.unsubscribe();
	}
}