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

package com.pushwoosh.location.tracker;

import android.content.Context;
import android.os.Looper;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.location.internal.checker.GoogleApiChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Regression guard for crash candidate #23
 * (crash-requestlocationupdates-securityexception-coarse-only).
 *
 * <p>{@link GoogleLocationProvider#notifyRequestLocationListener(boolean)} gates the high-accuracy
 * {@code requestLocationUpdates} call ({@code GoogleLocationProvider.java:150-151}) with
 * {@link LocationPermissionChecker#check()}, which returns {@code true} when {@code ACCESS_FINE_LOCATION}
 * <b>or</b> {@code ACCESS_COARSE_LOCATION} is granted. But the stored {@code locationRequest} on this
 * path is {@code PRIORITY_HIGH_ACCURACY}, and the Fused Location provider requires
 * {@code ACCESS_FINE_LOCATION} for high accuracy. With only COARSE granted the gate passes but
 * {@code getFusedLocationProviderClient(context).requestLocationUpdates(...)} throws
 * {@link SecurityException} — a {@link RuntimeException} unrelated to {@link ApiException}, so the
 * unwrapped async {@code catch(ApiException)} ({@code updateLocationTracker:252}) did NOT catch it and
 * it crashed the process.
 *
 * <p>The fix wraps the {@code :151} call in a {@code catch(SecurityException)} at the defect point:
 * the runtime denial degrades to {@code failedRequestLocation()} (the same path the gate already takes
 * when permission is absent) instead of escaping the async lambda. These tests assert that graceful
 * behavior — and the negative controls confirm the conditions are still discriminating.
 *
 * <h3>Stand-ins (faithful, documented per reproduce-crash phase 5)</h3>
 * <ul>
 *   <li>The {@link SecurityException} is raised by a Mockito stub on the {@link FusedLocationProviderClient}
 *       — real GMS internals cannot run on the JVM. It is faithful because it is thrown at the exact prod
 *       {@code :150-151} call site, through the real method body.</li>
 *   <li>The async GMS {@link Task} is replaced by a mock whose {@code addOnCompleteListener} fires
 *       {@code onComplete} synchronously — determinism substituted for the GMS Task timing. The path
 *       through the real {@code updateLocationTracker} lambda is faithful.</li>
 * </ul>
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class GoogleLocationProviderSecurityExceptionTest {

    private MockedStatic<LocationServices> locationServicesStatic;

    @After
    public void tearDown() {
        if (locationServicesStatic != null) {
            locationServicesStatic.close();
            locationServicesStatic = null;
        }
        EventBus.clearSubscribersMap();
    }

    // ---------------------------------------------------------------------------------------------
    // Harness plumbing
    // ---------------------------------------------------------------------------------------------

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = GoogleLocationProvider.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static LocationRequest highAccuracyRequest() {
        LocationRequest req = new LocationRequest();
        req.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return req;
    }

    /**
     * Stub {@code LocationServices} so the Fused client throws SecurityException for the high-accuracy
     * request (COARSE-only state). When {@code wireSettingsClient} is true, also wire the SettingsClient
     * so {@code updateLocationTracker}'s async Task onComplete fires synchronously and successfully.
     */
    private FusedLocationProviderClient stubLocationServices(boolean fusedThrows, boolean wireSettingsClient) {
        locationServicesStatic = Mockito.mockStatic(LocationServices.class);

        FusedLocationProviderClient fused = Mockito.mock(FusedLocationProviderClient.class);
        if (fusedThrows) {
            Mockito.when(fused.requestLocationUpdates(
                            Mockito.any(LocationRequest.class),
                            Mockito.any(LocationCallback.class),
                            Mockito.nullable(Looper.class)))
                    .thenThrow(new SecurityException(
                            "requestLocationUpdates HIGH_ACCURACY requires ACCESS_FINE_LOCATION"));
        }
        locationServicesStatic
                .when(() -> LocationServices.getFusedLocationProviderClient(Mockito.any(Context.class)))
                .thenReturn(fused);

        if (wireSettingsClient) {
            SettingsClient settingsClient = Mockito.mock(SettingsClient.class);
            @SuppressWarnings("unchecked")
            Task<LocationSettingsResponse> task = Mockito.mock(Task.class);
            Mockito.when(settingsClient.checkLocationSettings(Mockito.any())).thenReturn(task);
            try {
                Mockito.when(task.getResult(ApiException.class))
                        .thenReturn(Mockito.mock(LocationSettingsResponse.class));
            } catch (ApiException ignored) {
                // mock stub, never actually thrown
            }
            // Fire onComplete synchronously => the real updateLocationTracker lambda runs
            // notifyRequestLocationListener(true). (determinism substituted for GMS Task timing)
            Mockito.when(task.addOnCompleteListener(Mockito.<OnCompleteListener<LocationSettingsResponse>>any()))
                    .thenAnswer(inv -> {
                        OnCompleteListener<LocationSettingsResponse> l = inv.getArgument(0);
                        l.onComplete(task);
                        return task;
                    });
            locationServicesStatic
                    .when(() -> LocationServices.getSettingsClient(Mockito.any(Context.class)))
                    .thenReturn(settingsClient);
        }
        return fused;
    }

    /**
     * Build the real {@link GoogleLocationProvider} and force the COARSE-only dangerous state:
     * connected client, stored high-accuracy request, a request listener set, and the permission
     * checker returning true (FINE-or-COARSE gate passes).
     */
    private GoogleLocationProvider newDangerousProvider(boolean coarsePermissionGranted) throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        GoogleApiChecker apiChecker = Mockito.mock(GoogleApiChecker.class);
        Mockito.when(apiChecker.check()).thenReturn(true);
        LocationPermissionChecker permChecker = Mockito.mock(LocationPermissionChecker.class);
        Mockito.when(permChecker.check()).thenReturn(coarsePermissionGranted);

        GoogleLocationProvider p = new GoogleLocationProvider(ctx, apiChecker, permChecker, null);

        GoogleApiClient connectedClient = Mockito.mock(GoogleApiClient.class);
        Mockito.when(connectedClient.isConnected()).thenReturn(true);
        setField(p, "googleApiClient", connectedClient);
        setField(p, "locationRequest", highAccuracyRequest());

        GoogleLocationProvider.RequestLocationListener listener =
                Mockito.mock(GoogleLocationProvider.RequestLocationListener.class);
        setField(p, "requestLocationListener", listener);
        return p;
    }

    private static void invokeNotify(GoogleLocationProvider p, boolean isSuccess) throws Throwable {
        Method m = GoogleLocationProvider.class.getDeclaredMethod("notifyRequestLocationListener", boolean.class);
        m.setAccessible(true);
        try {
            m.invoke(p, isSuccess);
        } catch (InvocationTargetException ite) {
            throw ite.getCause();
        }
    }

    private static Object readField(Object target, String name) throws Exception {
        Field f = GoogleLocationProvider.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    // ---------------------------------------------------------------------------------------------
    // Regression guard A: the REAL async Task callback (updateLocationTracker) no longer escapes.
    // The SecurityException from the :151 high-accuracy call is caught at the defect point and the
    // path degrades to failedRequestLocation(); nothing propagates out of the lambda.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void reach_asyncTaskCallback_securityException_isHandledGracefully() throws Exception {
        // Verifies that the COARSE-only high-accuracy SecurityException is swallowed at the crash
        // point and reported via failedRequestLocation, not escaped through the unwrapped lambda.
        stubLocationServices(/*fusedThrows=*/ true, /*wireSettingsClient=*/ true);
        GoogleLocationProvider p = newDangerousProvider(/*coarsePermissionGranted=*/ true);
        GoogleLocationProvider.RequestLocationListener listener =
                (GoogleLocationProvider.RequestLocationListener) readField(p, "requestLocationListener");

        // Real method: registers the real async onComplete lambda (fired synchronously by the Task
        // mock), which calls notifyRequestLocationListener(true) -> the :151 call throws & is caught.
        p.updateLocationTracker(highAccuracyRequest());

        Mockito.verify(listener).failedRequestLocation();
        Mockito.verify(listener, Mockito.never()).successRequestLocation();
    }

    // ---------------------------------------------------------------------------------------------
    // Regression guard B: the crash-point body directly. notifyRequestLocationListener(true) catches
    // the SecurityException from :151 and routes to failedRequestLocation() without throwing.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void crashPoint_notifyRequestLocationListener_securityException_isHandledGracefully() throws Throwable {
        // Verifies that the defect-point catch(SecurityException) turns the throwing :151 call into
        // a graceful failedRequestLocation() instead of propagating the exception.
        stubLocationServices(/*fusedThrows=*/ true, /*wireSettingsClient=*/ false);
        GoogleLocationProvider p = newDangerousProvider(/*coarsePermissionGranted=*/ true);
        GoogleLocationProvider.RequestLocationListener listener =
                (GoogleLocationProvider.RequestLocationListener) readField(p, "requestLocationListener");

        invokeNotify(p, true);

        Mockito.verify(listener).failedRequestLocation();
        Mockito.verify(listener, Mockito.never()).successRequestLocation();
    }

    // ---------------------------------------------------------------------------------------------
    // negative control A: no permission at all -> gate :145 fails -> failedRequestLocation, no throw
    // (the FINE-or-COARSE gate passing is a necessary condition for reaching the :151 call)
    // ---------------------------------------------------------------------------------------------

    @Test
    public void negativeControl_noPermission_gateFails_noThrow() throws Throwable {
        stubLocationServices(/*fusedThrows=*/ true, /*wireSettingsClient=*/ false);
        GoogleLocationProvider p = newDangerousProvider(/*coarsePermissionGranted=*/ false);
        GoogleLocationProvider.RequestLocationListener listener =
                (GoogleLocationProvider.RequestLocationListener) readField(p, "requestLocationListener");
        // check()==false -> the gate at :145 short-circuits to failedRequestLocation, never reaching :151.
        invokeNotify(p, true);
        Mockito.verify(listener).failedRequestLocation();
        Mockito.verify(listener, Mockito.never()).successRequestLocation();
    }

    // ---------------------------------------------------------------------------------------------
    // negative control B: Fused does NOT throw (FINE present) -> successRequestLocation, no throw.
    // Discriminator: the catch(SecurityException) must not swallow the success path.
    // ---------------------------------------------------------------------------------------------

    @Test
    public void negativeControl_fusedDoesNotThrow_success_noThrow() throws Throwable {
        FusedLocationProviderClient fused = stubLocationServices(/*fusedThrows=*/ false, /*wireSettingsClient=*/ false);
        Mockito.when(fused.requestLocationUpdates(
                        Mockito.any(LocationRequest.class),
                        Mockito.any(LocationCallback.class),
                        Mockito.nullable(Looper.class)))
                .thenReturn(null);
        GoogleLocationProvider p = newDangerousProvider(/*coarsePermissionGranted=*/ true);
        GoogleLocationProvider.RequestLocationListener listener =
                (GoogleLocationProvider.RequestLocationListener) readField(p, "requestLocationListener");
        invokeNotify(p, true);
        Mockito.verify(listener).successRequestLocation();
        Mockito.verify(listener, Mockito.never()).failedRequestLocation();
    }
}
