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

package com.pushwoosh.location.internal.checker;

import android.content.Context;
import android.location.LocationManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;


public class LocationStateCheckerTest {
    private LocationStateChecker locationStateChecker;

    @Mock
    private Context context;
    @Mock
    private LocationManager locationManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(context.getSystemService(anyString())).thenReturn(locationManager);
        locationStateChecker = new LocationStateChecker(context);
    }

    @Test
    public void emptyLocationManager() {
        when(context.getSystemService(anyString())).thenReturn(null);
        Assert.assertEquals(false, locationStateChecker.check());
    }

    @Test
    public void checkDisbleAll() {
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false);

        Assert.assertEquals(false, locationStateChecker.check());
    }

    @Test
    public void checkEnableAll() {
        when(locationManager.isProviderEnabled(eq(LocationManager.GPS_PROVIDER))).thenReturn(true);
        when(locationManager.isProviderEnabled(eq(LocationManager.NETWORK_PROVIDER))).thenReturn(true);

        Assert.assertEquals(true, locationStateChecker.check());
    }

    @Test
    public void checkEnableNetwork() {
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(false);
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(true);

        Assert.assertEquals(true, locationStateChecker.check());
    }

    @Test
    public void checkEnableGPS() {
        when(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)).thenReturn(true);
        when(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)).thenReturn(false);

        Assert.assertEquals(true, locationStateChecker.check());
    }
}