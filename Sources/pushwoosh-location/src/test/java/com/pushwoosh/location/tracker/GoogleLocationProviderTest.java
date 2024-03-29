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

package com.pushwoosh.location.tracker;

import com.pushwoosh.location.foregroundservice.ForegroundServiceHelper;
import com.pushwoosh.location.internal.checker.GoogleApiChecker;
import com.pushwoosh.location.internal.checker.LocationPermissionChecker;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Implements;


//TODO Disabling these tests until we update robolectric to 4.9+ due to
// https://github.com/robolectric/robolectric/issues/7269

@RunWith(RobolectricTestRunner.class)
//@Config(Shadows={GoogleLocationProviderTest.ShadowGoogleApiClient.class})
public class GoogleLocationProviderTest {
	private GoogleLocationProvider googleLocationProvider;
	@Mock
	private GoogleApiChecker googleApiChecker;
	@Mock
	private LocationPermissionChecker locationPermissionChecker;
	@Mock
	private GoogleLocationProvider.GoogleLocationListener googleLocationListener;
	@Mock
	ForegroundServiceHelper foregroundServiceHelper;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		googleLocationProvider = new GoogleLocationProvider(RuntimeEnvironment.application, googleApiChecker, locationPermissionChecker, foregroundServiceHelper);

	}

	@Implements(className = "GoogleApiClient")
	public class ShadowGoogleApiClient {

	}

	@Ignore
	@Test
	public void setGoogleLocationListener() {
		googleLocationProvider.setGoogleLocationListener(googleLocationListener);

	}

	@Ignore
	@Test
	public void connectGoogleApiClientIsNull() {
		googleLocationProvider = new GoogleLocationProvider(null, googleApiChecker, locationPermissionChecker, foregroundServiceHelper);
		googleLocationProvider.connect();
	}

	@Ignore
	@Test
	public void onConnected() {
	}

	@Ignore
	@Test
	public void getLastLocation() {
	}

	@Ignore
	@Test
	public void onConnectionFailed() {
	}

	@Ignore
	@Test
	public void onConnectionSuspended() {
	}

	@Ignore
	@Test
	public void onLocationChanged() {
	}

	@Ignore
	@Test
	public void updateLocationTracker() {
	}

	@Ignore
	@Test
	public void isConnected() {
	}

	@Ignore
	@Test
	public void cancel() {
	}

	@Ignore
	@Test
	public void isLocationAvailable() {
	}
}