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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.location.internal.LocationModule;
import com.pushwoosh.location.storage.LocationPrefs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
@LooperMode(LooperMode.Mode.LEGACY)
public class GeofenceReceiverTest {

    private static GeofencingEvent eventWithZones(int transition, List<String> zoneIds) {
        GeofencingEvent event = mock(GeofencingEvent.class);
        when(event.hasError()).thenReturn(false);
        when(event.getGeofenceTransition()).thenReturn(transition);
        List<Geofence> geofences = new ArrayList<>();
        for (String zoneId : zoneIds) {
            Geofence geofence = mock(Geofence.class);
            when(geofence.getRequestId()).thenReturn(zoneId);
            geofences.add(geofence);
        }
        when(event.getTriggeringGeofences()).thenReturn(geofences);
        return event;
    }

    private static void stubTracking(MockedStatic<LocationModule> locationModule, boolean geolocationStarted) {
        LocationPrefs prefs = mock(LocationPrefs.class);
        PreferenceBooleanValue started = mock(PreferenceBooleanValue.class);
        when(started.get()).thenReturn(geolocationStarted);
        when(prefs.geolocationStarted()).thenReturn(started);
        locationModule.when(LocationModule::locationPrefs).thenReturn(prefs);
    }

    @Test
    public void testCallbackReceivesEnterTransitionWhenTrackingStarted() {
        Context context = RuntimeEnvironment.getApplication();
        Intent intent = new Intent(GeofenceReceiver.getGeofenceAction(context));
        GeofencingEvent event = eventWithZones(Geofence.GEOFENCE_TRANSITION_ENTER, Arrays.asList("zone-a", "zone-b"));

        try (MockedStatic<LocationModule> locationModule = mockStatic(LocationModule.class);
                MockedStatic<GeofencingEvent> geofencingEventStatic = mockStatic(GeofencingEvent.class)) {
            GeofenceStateChangedCallback callback = mock(GeofenceStateChangedCallback.class);
            locationModule.when(LocationModule::geofenceStateChangedCallback).thenReturn(callback);
            stubTracking(locationModule, true);
            geofencingEventStatic
                    .when(() -> GeofencingEvent.fromIntent(any(Intent.class)))
                    .thenReturn(event);

            GeofenceReceiver receiver = new GeofenceReceiver();
            receiver.onReceive(context, intent);

            verify(callback)
                    .onGeofenceStateChanged(Arrays.asList("zone-a", "zone-b"), Geofence.GEOFENCE_TRANSITION_ENTER);
        }
    }

    @Test
    public void testCallbackReceivesExitTransitionWhenTrackingStarted() {
        Context context = RuntimeEnvironment.getApplication();
        Intent intent = new Intent(GeofenceReceiver.getGeofenceAction(context));
        GeofencingEvent event = eventWithZones(Geofence.GEOFENCE_TRANSITION_EXIT, Collections.singletonList("zone-a"));

        try (MockedStatic<LocationModule> locationModule = mockStatic(LocationModule.class);
                MockedStatic<GeofencingEvent> geofencingEventStatic = mockStatic(GeofencingEvent.class)) {
            GeofenceStateChangedCallback callback = mock(GeofenceStateChangedCallback.class);
            locationModule.when(LocationModule::geofenceStateChangedCallback).thenReturn(callback);
            stubTracking(locationModule, true);
            geofencingEventStatic
                    .when(() -> GeofencingEvent.fromIntent(any(Intent.class)))
                    .thenReturn(event);

            GeofenceReceiver receiver = new GeofenceReceiver();
            receiver.onReceive(context, intent);

            verify(callback)
                    .onGeofenceStateChanged(Collections.singletonList("zone-a"), Geofence.GEOFENCE_TRANSITION_EXIT);
        }
    }

    @Test
    public void testCallbackIgnoresDwellTransitionWhenTrackingStarted() {
        Context context = RuntimeEnvironment.getApplication();
        Intent intent = new Intent(GeofenceReceiver.getGeofenceAction(context));
        GeofencingEvent event = eventWithZones(Geofence.GEOFENCE_TRANSITION_DWELL, Collections.singletonList("zone-a"));

        try (MockedStatic<LocationModule> locationModule = mockStatic(LocationModule.class);
                MockedStatic<GeofencingEvent> geofencingEventStatic = mockStatic(GeofencingEvent.class)) {
            GeofenceStateChangedCallback callback = mock(GeofenceStateChangedCallback.class);
            locationModule.when(LocationModule::geofenceStateChangedCallback).thenReturn(callback);
            stubTracking(locationModule, true);
            geofencingEventStatic
                    .when(() -> GeofencingEvent.fromIntent(any(Intent.class)))
                    .thenReturn(event);

            GeofenceReceiver receiver = new GeofenceReceiver();
            receiver.onReceive(context, intent);

            verify(callback, never()).onGeofenceStateChanged(anyList(), anyInt());
        }
    }

    @Test
    public void testEventIsNotParsedWhenTrackingStopped() {
        Context context = RuntimeEnvironment.getApplication();
        Intent intent = new Intent(GeofenceReceiver.getGeofenceAction(context));
        GeofencingEvent event = eventWithZones(Geofence.GEOFENCE_TRANSITION_ENTER, Collections.singletonList("zone-a"));

        try (MockedStatic<LocationModule> locationModule = mockStatic(LocationModule.class);
                MockedStatic<GeofencingEvent> geofencingEventStatic = mockStatic(GeofencingEvent.class)) {
            GeofenceStateChangedCallback callback = mock(GeofenceStateChangedCallback.class);
            locationModule.when(LocationModule::geofenceStateChangedCallback).thenReturn(callback);
            stubTracking(locationModule, false);
            geofencingEventStatic
                    .when(() -> GeofencingEvent.fromIntent(any(Intent.class)))
                    .thenReturn(event);

            GeofenceReceiver receiver = new GeofenceReceiver();
            receiver.onReceive(context, intent);

            verify(callback, never()).onGeofenceStateChanged(anyList(), anyInt());
            geofencingEventStatic.verify(() -> GeofencingEvent.fromIntent(any(Intent.class)), never());
        }
    }

    @Test
    public void testReceiverSurvivesPrefsFailureWhenGateIsEvaluated() {
        Context context = RuntimeEnvironment.getApplication();
        Intent intent = new Intent(GeofenceReceiver.getGeofenceAction(context));

        try (MockedStatic<LocationModule> locationModule = mockStatic(LocationModule.class);
                MockedStatic<GeofencingEvent> geofencingEventStatic = mockStatic(GeofencingEvent.class)) {
            GeofenceStateChangedCallback callback = mock(GeofenceStateChangedCallback.class);
            locationModule.when(LocationModule::geofenceStateChangedCallback).thenReturn(callback);
            locationModule
                    .when(LocationModule::locationPrefs)
                    .thenThrow(new IllegalStateException("prefs provider unavailable"));

            GeofenceReceiver receiver = new GeofenceReceiver();
            receiver.onReceive(context, intent);

            verify(callback, never()).onGeofenceStateChanged(anyList(), anyInt());
        }
    }
}
