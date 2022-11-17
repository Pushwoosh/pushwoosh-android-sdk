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

package com.pushwoosh.location.network;

import android.location.Location;
import androidx.core.util.Pair;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.MockHelper;
import com.pushwoosh.location.NearestZonesManager;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.location.network.job.Job;
import com.pushwoosh.location.network.repository.UpdateNearestRepository;
import com.pushwoosh.location.tracker.LocationTracker;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GetNearestZoneJobApplierTest {
    public static final PushwooshException TEST_EXCEPTION = new PushwooshException("test exception");
    public static final LocationNotAvailableException LOCATION_NOT_AVAILABLE_EXCEPTION = new LocationNotAvailableException();
    private GetNearestZoneJobApplier getNearestZoneJobApplier;

    @Mock
    private NearestZonesManager nearestZonesManager;
    @Mock
    private UpdateNearestRepository updateNearestRepository;
    @Mock
    private LocationTracker locationTracker;
    @Mock
    private Job<Result<Pair<Location, List<GeoZone>>, PushwooshException>> nearestZonesJob;
    @Mock
    private Job<Result<Void, NetworkException>> locationJob;
    @Captor
    ArgumentCaptor<Callback<Pair<Location, List<GeoZone>>, PushwooshException>> callbackArgumentCaptor;
    @Captor
    ArgumentCaptor<Callback<Job<Result<Pair<Location, List<GeoZone>>, PushwooshException>>, PushwooshException>> jobCallbackArgumentCaptor;


    @Before
    public void setUp() throws Exception {
        MockHelper.changeLogLevel(PWLog.Level.NONE);
        MockitoAnnotations.initMocks(this);
        getNearestZoneJobApplier = new GetNearestZoneJobApplier(nearestZonesManager, updateNearestRepository, locationTracker);

        when(updateNearestRepository.applyDisableLocationJob(any())).thenReturn(locationJob);

    }

    @Test
    public void  loadNearestGeoZonesDisableLocation(){
        getNearestZoneJobApplier.loadNearestGeoZones(true);

        verify(updateNearestRepository).applyDisableLocationJob(any());
    }

    @Test
    public void loadNearestGeoZones() {
        when(locationTracker.isLocationAvailable()).thenReturn(true);

        getNearestZoneJobApplier.loadNearestGeoZones(true);

        verify(updateNearestRepository).applyNearestJob(eq(true),callbackArgumentCaptor.capture(), jobCallbackArgumentCaptor.capture());
        Pair<Location, List<GeoZone>> data = createData();
        callbackArgumentCaptor.getValue().process(Result.fromData(data));

        verify(nearestZonesManager).updateZones(data.first, data.second);
        verify(nearestZonesManager).updateState(false);
    }

    private Pair<Location, List<GeoZone>> createData() {
        Location location = Mockito.mock(Location.class);
        GeoZone geoZone = Mockito.mock(GeoZone.class);
        GeoZone geoZone2 = Mockito.mock(GeoZone.class);
        List<GeoZone> list = new ArrayList<>();
        list.add(geoZone);
        list.add(geoZone2);
        return new Pair<Location, List<GeoZone>>(location, list);
    }

    @Test
    public void loadNearestGeoZonesSimpleException(){
        loadNearestGeoZonesException(TEST_EXCEPTION);
    }

    @Test
    public void loadNearestGeoZonesLocationNotAvailableException(){
        loadNearestGeoZonesException(LOCATION_NOT_AVAILABLE_EXCEPTION);
        verify(nearestZonesManager).requestLocation();
    }

    public void loadNearestGeoZonesException(PushwooshException e) {
        when(locationTracker.isLocationAvailable()).thenReturn(true);

        getNearestZoneJobApplier.loadNearestGeoZones(true);

        verify(updateNearestRepository).applyNearestJob(eq(true),callbackArgumentCaptor.capture(), jobCallbackArgumentCaptor.capture());
        callbackArgumentCaptor.getValue().process(Result.fromException(e));

        verify(nearestZonesManager).updateState(true);
    }

    @Test
    public void locationDisable() {
        getNearestZoneJobApplier.locationDisable();
        verify(updateNearestRepository).applyDisableLocationJob(any());
    }

    @Test
    @Ignore // TODO
    public void cancel() {
        when(locationTracker.isLocationAvailable()).thenReturn(true);
        getNearestZoneJobApplier.loadNearestGeoZones(true);
        getNearestZoneJobApplier.locationDisable();

        getNearestZoneJobApplier.cancel();

        verify(nearestZonesJob).cancel();
        verify(locationJob).cancel();
    }
}