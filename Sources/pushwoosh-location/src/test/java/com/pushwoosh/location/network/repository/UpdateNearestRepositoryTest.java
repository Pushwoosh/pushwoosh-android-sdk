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

package com.pushwoosh.location.network.repository;

import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.location.storage.NearestZonesStorage;
import com.pushwoosh.location.tracker.LocationTracker;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class UpdateNearestRepositoryTest {

    private UpdateNearestRepository updateNearestRepository;
    @Mock
    private NearestZonesStorage nearestZonesStorage;
    @Mock
    private LocationTracker locationTracker;
    @Mock
    private RequestManager requestManager;

    @Mock
    private Callback<Void, NetworkException> callback;



    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        AndroidPlatformModule.init(RuntimeEnvironment.application, true);

        updateNearestRepository = new UpdateNearestRepository(nearestZonesStorage, locationTracker);
    }

    @Test
    @Ignore
    public void applyNearestJob() {
        Callback callback = Mockito.mock(Callback.class);
        when(nearestZonesStorage.getAll()).thenReturn(null);
        ArgumentCaptor<Result> callbackArgumentCaptor = ArgumentCaptor.forClass(Result.class);
        updateNearestRepository.applyNearestJob(false, callback, updateJobCallback -> {});
    }

    @Test
    @Ignore
    public void applyDisableLocationJob() {
        NetworkModule.setRequestManager(requestManager);
        updateNearestRepository.applyDisableLocationJob(callback);
        ArgumentCaptor<Result<Void, NetworkException>> resultArgumentCaptor = ArgumentCaptor.forClass(Result.class);
        Mockito.verify(callback).process(resultArgumentCaptor.capture());
        Result<Void, NetworkException> result = resultArgumentCaptor.getValue();
      //  Assert.assertNull(result.getException());
        Assert.assertEquals("", result.getException().getMessage());


    }
}