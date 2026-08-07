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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pushwoosh.function.Result;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.network.RequestManager;
import com.pushwoosh.location.network.data.DisableLocationRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 30)
@LooperMode(LooperMode.Mode.LEGACY)
public class DisableLocationJobTest {

    private RequestManager requestManager;

    @Before
    public void setUp() {
        requestManager = mock(RequestManager.class);
        NetworkModule.setRequestManager(requestManager);
    }

    @After
    public void tearDown() {
        NetworkModule.setRequestManager(null);
    }

    // Verifies that the job sends DisableLocationRequest synchronously and returns the server result as is.
    @Test
    public void testApplySendsDisableLocationRequestAndReturnsItsResult() {
        Result<Void, NetworkException> serverResult = Result.fromData(null);
        when(requestManager.sendRequestSync(any(DisableLocationRequest.class))).thenReturn(serverResult);

        Result<Void, NetworkException> result = new DisableLocationJob().apply();

        assertSame(serverResult, result);
        verify(requestManager).sendRequestSync(any(DisableLocationRequest.class));
    }

    // Verifies that the job returns the not-initialized network error instead of dereferencing a missing
    // request manager when the SDK never finished initialization.
    @Test
    public void testApplyFailsWithNetworkExceptionWhenSdkNotInitialized() {
        NetworkModule.setRequestManager(null);

        Result<Void, NetworkException> result = new DisableLocationJob().apply();

        assertFalse(result.isSuccess());
        assertEquals("SDK is not initialized", result.getException().getMessage());
    }
}
