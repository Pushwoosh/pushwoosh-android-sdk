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

package com.pushwoosh.location.network.job;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.core.util.Pair;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.location.data.GeoZone;
import com.pushwoosh.location.network.data.GetNearestZoneRequest;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
@LooperMode(LooperMode.Mode.LEGACY)
public class UpdateNearestJobTest {

    private static final class ParamsCase {
        final String name;
        final Location location;
        final boolean networkAvailable;

        ParamsCase(String name, Location location, boolean networkAvailable) {
            this.name = name;
            this.location = location;
            this.networkAvailable = networkAvailable;
        }
    }

    private RequestManager requestManager;
    private Location currentLocation;

    @Before
    public void setUp() {
        AndroidPlatformModule.init(RuntimeEnvironment.getApplication(), true);
        requestManager = mock(RequestManager.class);
        NetworkModule.setRequestManager(requestManager);

        currentLocation = new Location("test");
        currentLocation.setLatitude(10.0);
        currentLocation.setLongitude(20.0);
    }

    @After
    public void tearDown() {
        NetworkModule.setRequestManager(null);
    }

    // Verifies that a successful getNearestZone response is returned paired with the location the job was built for.
    @Test
    public void testApplyPairsLocationWithZonesWhenServerRespondsWithData() {
        List<GeoZone> zones = Collections.singletonList(new GeoZone("zone", 10.0, 20.0, 100L, 5L));
        Result<List<GeoZone>, NetworkException> serverResult = Result.fromData(zones);
        when(requestManager.sendRequestSync(any(GetNearestZoneRequest.class))).thenReturn(serverResult);

        Result<Pair<Location, List<GeoZone>>, PushwooshException> result =
                new UpdateNearestJob(currentLocation).apply();

        assertTrue(result.isSuccess());
        assertSame(currentLocation, result.getData().first);
        assertSame(zones, result.getData().second);
    }

    // Verifies that a failed getNearestZone response surfaces the server exception unchanged.
    @Test
    public void testApplyPropagatesServerExceptionWhenRequestFails() {
        NetworkException serverError = new NetworkException("server is down");
        Result<List<GeoZone>, NetworkException> serverResult = Result.fromException(serverError);
        when(requestManager.sendRequestSync(any(GetNearestZoneRequest.class))).thenReturn(serverResult);

        Result<Pair<Location, List<GeoZone>>, PushwooshException> result =
                new UpdateNearestJob(currentLocation).apply();

        assertFalse(result.isSuccess());
        assertSame(serverError, result.getException());
    }

    // Verifies that the job fails with LocationNotAvailableException and sends nothing when either the
    // location or the network is missing.
    @Test
    public void testApplyFailsWithoutRequestWhenLocationOrNetworkMissing() {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                RuntimeEnvironment.getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo connectedNetwork = connectivityManager.getActiveNetworkInfo();
        assertTrue(
                "network must be available by default, otherwise the cases do not isolate conditions",
                GeneralUtils.isNetworkAvailable());

        ParamsCase[] cases = {
            new ParamsCase("no location", null, true), new ParamsCase("no network", currentLocation, false),
        };

        for (ParamsCase c : cases) {
            shadowOf(connectivityManager).setActiveNetworkInfo(c.networkAvailable ? connectedNetwork : null);

            Result<Pair<Location, List<GeoZone>>, PushwooshException> result = new UpdateNearestJob(c.location).apply();

            assertFalse("case " + c.name, result.isSuccess());
            assertTrue("case " + c.name, result.getException() instanceof LocationNotAvailableException);
        }

        verify(requestManager, never()).sendRequestSync(any());
    }

    // Verifies that the job reports the not-initialized network error instead of dereferencing a missing
    // request manager when the SDK never finished initialization.
    @Test
    public void testApplyFailsWithNetworkExceptionWhenSdkNotInitialized() {
        NetworkModule.setRequestManager(null);

        Result<Pair<Location, List<GeoZone>>, PushwooshException> result =
                new UpdateNearestJob(currentLocation).apply();

        assertFalse(result.isSuccess());
        assertTrue(result.getException() instanceof NetworkException);
        assertEquals("SDK is not initialized", result.getException().getMessage());
    }
}
