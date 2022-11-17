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

package com.pushwoosh.location.geofence;

import android.location.Location;

import com.google.android.gms.location.Geofence;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.data.GeoZonesProvider;
import com.pushwoosh.location.geofencer.Geofencer;
import com.pushwoosh.location.internal.checker.FineLocationPermissionChecker;
import com.pushwoosh.location.storage.GeoZoneStorage;
import com.pushwoosh.location.tracker.LocationTracker;
import com.pushwoosh.location.utils.Matchers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.pushwoosh.location.data.GeoZonesProvider.DEFAULT_RANGE;
import static com.pushwoosh.location.geofence.GeofenceTrackerImp.MIN_RADIUS;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GeofenceTrackerTest {

    private GeofenceTrackerImp geofenceTrackerImp;

    @Mock
    Geofencer geofencer;
    @Mock
    GeoZonesUpdater geoZonesUpdater;
    @Mock
    GeoZoneStorage geoZoneStorage;
    @Mock
    LocationTracker locationTracker;
    @Mock
    FineLocationPermissionChecker fineLocationPermissionChecker;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(fineLocationPermissionChecker.check()).thenReturn(true);
        geofenceTrackerImp = new GeofenceTrackerImp(geofencer, geoZonesUpdater, geoZoneStorage, locationTracker, fineLocationPermissionChecker);
    }

    @Test
    public void updateZones_saveZones() {
        //init
        Location location = mock(Location.class);
        List<GeoZone> geoZoneList = new ArrayList<>(GeoZonesProvider.generateGeoZones(10, 0));

        geofenceTrackerImp.updateZones(geoZoneList, location);

        //check
        verify(geoZoneStorage).saveGeoZones(argThat(Matchers.sameAsSet(geoZoneList)));
    }

    @Test
    public void updateZones_removeExpiredZones() {
        //init
        List<GeoZone> storageZones = GeoZonesProvider.generateGeoZones(5, 0);
        List<GeoZone> newZones = GeoZonesProvider.generateGeoZones(5, 5);
        Location location = mock(Location.class);

        //prepare
        when(geoZoneStorage.getGeoZones()).thenReturn(storageZones);
        geofenceTrackerImp = new GeofenceTrackerImp(geofencer, geoZonesUpdater, geoZoneStorage, locationTracker, fineLocationPermissionChecker);

        geofenceTrackerImp.updateZones(newZones, location);

        //check
        verify(geofencer).removeZones(argThat(Matchers.sameAsSet(storageZones)));
    }

    @Test
    public void updateZones_addNewZones() {
        //init
        Location location = mock(Location.class);
        List<GeoZone> storageZones = GeoZonesProvider.generateGeoZones(5, 0);
        List<GeoZone> newZones = GeoZonesProvider.generateGeoZones(5, 5);

        //prepare
        when(geoZoneStorage.getGeoZones()).thenReturn(storageZones);
        geofenceTrackerImp = new GeofenceTrackerImp(geofencer, geoZonesUpdater, geoZoneStorage, locationTracker, fineLocationPermissionChecker);

        geofenceTrackerImp.updateZones(newZones, location);

        newZones.add(GeoZone.createRadiusZone(newZones, location, MIN_RADIUS));
        //check
        verify(geofencer).addZones(argThat(Matchers.sameAsSet(newZones)));
    }

    @Test
    public void updateZones_updatePushGeoZonesAllZonesInsideRange() {
        //init
        Location location = mock(Location.class);
        when(location.distanceTo(any(Location.class))).thenReturn(DEFAULT_RANGE - 1f);
        List<GeoZone> storageZones = GeoZonesProvider.generateGeoZones(5, 0, location);
        List<GeoZone> newZones = GeoZonesProvider.generateGeoZones(5, 0, location);

        //prepare
        when(geoZoneStorage.getGeoZones()).thenReturn(storageZones);
        geofenceTrackerImp = new GeofenceTrackerImp(geofencer, geoZonesUpdater, geoZoneStorage, locationTracker, fineLocationPermissionChecker);
        geofenceTrackerImp.pushGeoZones = newZones;

        geofenceTrackerImp.updateZones(newZones, location);

        newZones.add(GeoZone.createRadiusZone(newZones, location, MIN_RADIUS));
        //check
        verify(locationTracker).requestLocationUpdates(false);
    }

    @Test
    public void updateZones_updatePushGeoZonesSomeZonesNotInsideRange() {
        //init
        Location location = mock(Location.class);
        when(location.distanceTo(any(Location.class))).thenReturn(DEFAULT_RANGE + 1f);
        List<GeoZone> storageZones = GeoZonesProvider.generateGeoZones(5, 0, location);
        List<GeoZone> newZones = GeoZonesProvider.generateGeoZones(5, 0, location);

        //prepare
        when(geoZoneStorage.getGeoZones()).thenReturn(storageZones);
        geofenceTrackerImp = new GeofenceTrackerImp(geofencer, geoZonesUpdater, geoZoneStorage, locationTracker, fineLocationPermissionChecker);
        geofenceTrackerImp.pushGeoZones = newZones;

        geofenceTrackerImp.updateZones(newZones, location);

        newZones.add(GeoZone.createRadiusZone(newZones, location, MIN_RADIUS));
        //check
        assertEquals(geofenceTrackerImp.pushGeoZones, newZones);
        verify(locationTracker, never()).requestLocationUpdates(false);
    }

    @Test
    @Ignore // TODO
    public void onGeofenceStateChanged_enterWithLargeDistance_PushGeoZonesMatches() {
        List<String> ids = prepareGeofenceStateChangedEnvironment(DEFAULT_RANGE + 1f);

        geofenceTrackerImp.onGeofenceStateChanged(ids, Geofence.GEOFENCE_TRANSITION_ENTER);

        //check
        Assert.assertTrue("Push geo zones incorrect", Matchers.sameAsSet(geofenceTrackerImp.pushGeoZones).matches(GeoZonesProvider.generateGeoZones(5, 0)));
        verify(locationTracker).requestLocationUpdates(true);
    }

    @Test
    public void onGeofenceStateChanged_enterWithLargeDistance_NotUpdateGeoZones() {
        List<String> ids = prepareGeofenceStateChangedEnvironment(DEFAULT_RANGE + 1f);

        geofenceTrackerImp.onGeofenceStateChanged(ids, Geofence.GEOFENCE_TRANSITION_ENTER);

        //check
        verify(geoZonesUpdater, never()).requestUpdateGeoZones(cb -> {});
    }

    @Test
    public void onGeofenceStateChanged_enterWithNormalDistance_PushGeoZonesMatches() {
        //init
        List<String> ids = prepareGeofenceStateChangedEnvironment(DEFAULT_RANGE - 1f);

        geofenceTrackerImp.onGeofenceStateChanged(ids, Geofence.GEOFENCE_TRANSITION_ENTER);

        //check
        Assert.assertTrue("Push geo zones not empty;", geofenceTrackerImp.pushGeoZones.isEmpty());
    }

    @Test
    @Ignore // TODO
    public void onGeofenceStateChanged_enterWithNormalDistance_UpdateGeoZones() {
        //init
        List<String> ids = prepareGeofenceStateChangedEnvironment(DEFAULT_RANGE - 1f);

        geofenceTrackerImp.onGeofenceStateChanged(ids, Geofence.GEOFENCE_TRANSITION_ENTER);

        //check
        verify(geoZonesUpdater).requestUpdateGeoZones(cb -> {});
    }

    @Test
    @Ignore // TODO
    public void onGeofenceStateChanged_exitFromAllEnteredZones() {
        //init
        List<String> ids = prepareGeofenceStateChangedEnvironment(DEFAULT_RANGE + 1f);

        geofenceTrackerImp.onGeofenceStateChanged(ids, Geofence.GEOFENCE_TRANSITION_ENTER);
        geofenceTrackerImp.onGeofenceStateChanged(ids, Geofence.GEOFENCE_TRANSITION_EXIT);

        //check
        verify(locationTracker).requestLocationUpdates(false);
    }

    @Test
    @Ignore // TODO
    public void onGeofenceStateChanged_exitFromRadiusZone() {
        //init
        List<GeoZone> newGeoZones = GeoZonesProvider.generateGeoZones(4, 0);
        Location location = mock(Location.class);
        List<String> ids = Collections.singletonList(GeoZone.createRadiusZone(newGeoZones, location, MIN_RADIUS).getName());

        geofenceTrackerImp.updateZones(newGeoZones, location);

        geofenceTrackerImp.onGeofenceStateChanged(ids, Geofence.GEOFENCE_TRANSITION_EXIT);

        //check
        verify(geoZonesUpdater).requestUpdateGeoZones(cb -> {});
    }

    private List<String> prepareGeofenceStateChangedEnvironment(float distanceTo) {
        //init
        Location location = mock(Location.class);
        when(location.distanceTo(any(Location.class))).thenReturn(distanceTo);
        List<GeoZone> storageZones = GeoZonesProvider.generateGeoZones(10, 0, location);
        List<String> ids = GeoZonesProvider.generateGeoZonesIds(5, 0);

        //prepare
        when(geoZoneStorage.getGeoZones()).thenReturn(storageZones);
        //TODO when(locationTracker.getLocation()).thenReturn(location);
        geofenceTrackerImp = new GeofenceTrackerImp(geofencer, geoZonesUpdater, geoZoneStorage, locationTracker, fineLocationPermissionChecker);
        return ids;
    }

    @Test
    @Ignore
    public void locationUpdate_SeveralTimeCall() {
        Location location = mock(Location.class);
        when(location.distanceTo(any())).thenReturn(DEFAULT_RANGE - 1f);
        //TODO when(geoZonesUpdater.requestUpdateGeoZones(cb -> {})).thenReturn(true);

        geofenceTrackerImp.locationUpdated(location);
        geofenceTrackerImp.locationUpdated(location);

//TODO        verify(geoZonesUpdater, times(1)).requestUpdateGeoZones(cb->{});
    }

    @Test
    public void locationUpdate_noPushGeoZonesAndGetNearestCalled() {
        Location location = mock(Location.class);
        when(location.distanceTo(any())).thenReturn(DEFAULT_RANGE - 1f);
        //TODO when(geoZonesUpdater.requestUpdateGeoZones(cb -> {})).thenReturn(true);

        geofenceTrackerImp.updateZones(Collections.emptyList(), location);
        geofenceTrackerImp.locationUpdated(location);

        verify(geoZonesUpdater, never()).requestUpdateGeoZones(cb -> {});
    }


    @Test
    public void locationUpdate_GetNearestCalled() {
        Location location = mock(Location.class);
        //TODO when(geoZonesUpdater.requestUpdateGeoZones()).thenReturn(true);

        geofenceTrackerImp.updateZones(Collections.emptyList(), location);
        geofenceTrackerImp.locationUpdated(location);

        verify(geoZonesUpdater, never()).requestUpdateGeoZones(cb -> {});
    }

    @Test
    @Ignore // TODO
    public void locationUpdate_withPushGeoZonesAndGetNearestCalled() {
        Location location = mock(Location.class);
        when(location.distanceTo(any())).thenReturn(DEFAULT_RANGE - 1f);
//TODO        when(geoZonesUpdater.requestUpdateGeoZones()).thenReturn(true);
        geofenceTrackerImp.pushGeoZones = GeoZonesProvider.generateGeoZones(3, 0, location);

        geofenceTrackerImp.locationUpdated(location);

        verify(geoZonesUpdater).requestUpdateGeoZones(cb->{});
    }

    @Test
    public void onDestroy_GeofencerDestroy() {
        geofenceTrackerImp.onDestroy();

        verify(geofencer).onDestroy();
    }
}
