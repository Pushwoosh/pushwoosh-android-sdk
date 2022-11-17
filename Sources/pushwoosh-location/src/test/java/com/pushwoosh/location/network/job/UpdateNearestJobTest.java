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

package com.pushwoosh.location.network.job;

import android.location.Location;
import androidx.core.util.Pair;

import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.PushRequest;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.location.data.GeoZone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class UpdateNearestJobTest {

    public static final PushwooshException TEST_NETWORK_EXCEPTION = new PushwooshException("Test NetworkException");
    @Mock
    private Location location;
    @Mock
    private RequestManager requestManager;

    private UpdateNearestJob updateNearestJob;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);
    }

    @Test
    public void apply() {
        updateNearestJob = new UpdateNearestJob(null);
        Result<Pair<Location, List<GeoZone>>, PushwooshException> result = updateNearestJob.apply();
        Assert.assertEquals("Location not found", result.getException().getMessage());

        updateNearestJob = new UpdateNearestJob(location);
        Result<Pair<Location, List<GeoZone>>, PushwooshException> result2 = updateNearestJob.apply();
        Assert.assertEquals("Request Manager is null", result2.getException().getMessage());

        when(requestManager.sendRequestSync(any(PushRequest.class))).thenReturn(Result.fromException(TEST_NETWORK_EXCEPTION));
        NetworkModule.setRequestManager(requestManager);
        Result<Pair<Location, List<GeoZone>>, PushwooshException> result3 = updateNearestJob.apply();
        Assert.assertEquals("Test NetworkException", result3.getException().getMessage());

        List<GeoZone> list = new ArrayList<>();
        list.add(Mockito.mock(GeoZone.class));
        when(requestManager.sendRequestSync(any(PushRequest.class))).thenReturn(Result.fromData(list));
        Result<Pair<Location, List<GeoZone>>, PushwooshException> result4 = updateNearestJob.apply();
        Assert.assertNull( result4.getException());
        Assert.assertTrue(result4.isSuccess());
        Assert.assertEquals(location, result4.getData().first);
        Assert.assertEquals(list, result4.getData().second);
    }
}