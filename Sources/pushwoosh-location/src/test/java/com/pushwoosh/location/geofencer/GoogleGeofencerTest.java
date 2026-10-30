package com.pushwoosh.location.geofencer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.internal.checker.GoogleApiChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class GoogleGeofencerTest {

    private static final List<GeoZone> ZONES = Collections.singletonList(new GeoZone("zone", 55.0, 37.0, 100, 10));

    private GoogleGeofencer newGeofencer(GeofencingClient client, boolean permissionGateOpen) throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        LocationPermissionChecker permissionChecker = mock(LocationPermissionChecker.class);
        when(permissionChecker.check()).thenReturn(permissionGateOpen);
        GoogleGeofencer geofencer = new GoogleGeofencer(context, permissionChecker, client, mock(PendingIntent.class));

        GoogleApiChecker apiChecker = mock(GoogleApiChecker.class);
        when(apiChecker.check()).thenReturn(true);
        Field field = GoogleGeofencer.class.getDeclaredField("googleApiChecker");
        field.setAccessible(true);
        field.set(geofencer, apiChecker);
        return geofencer;
    }

    @Test
    public void addZones_permissionGateClosed_doesNotCallAddGeofences() throws Exception {
        GeofencingClient client = mock(GeofencingClient.class);

        newGeofencer(client, false).addZones(ZONES);

        verify(client, never()).addGeofences(any(GeofencingRequest.class), any(PendingIntent.class));
    }

    @Test
    public void testFailureDescriptionCarriesStatusCodeForApiException() {
        ApiException exception = new ApiException(new Status(GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE));

        String description = GoogleGeofencer.describeAddZonesFailure(exception);

        assertTrue(description.startsWith("status code " + GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE));
        assertTrue(description.contains("GEOFENCE_NOT_AVAILABLE"));
    }

    @Test
    public void testFailureDescriptionCarriesMessageForPlainException() {
        String description = GoogleGeofencer.describeAddZonesFailure(new IllegalStateException("client disconnected"));

        assertTrue(description.contains("client disconnected"));
    }

    @Test
    public void testFailureDescriptionHandlesMissingException() {
        assertNotNull(GoogleGeofencer.describeAddZonesFailure(null));
    }
}
