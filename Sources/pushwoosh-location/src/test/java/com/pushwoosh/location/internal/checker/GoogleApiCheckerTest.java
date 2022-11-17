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

import com.google.android.gms.common.api.GoogleApiClient;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.MockHelper;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoogleApiCheckerTest {
    private GoogleApiChecker googleApiChecker;

    @Mock
    private GoogleApiClient googleApiClient;



    @Before
    public void setUp() {
        MockHelper.changeLogLevel(PWLog.Level.NONE);
        MockitoAnnotations.initMocks(this);
        googleApiChecker = new GoogleApiChecker();
        googleApiChecker.setGoogleApiClient(googleApiClient);
    }

    @Test
    public void GoogleApiClientIsNull() {
        googleApiChecker.setGoogleApiClient(null);
        Assert.assertFalse(googleApiChecker.check());
    }

    @Test
    public void checkNotConnecting() {
        when(googleApiClient.isConnected()).thenReturn(false);
        when(googleApiClient.isConnecting()).thenReturn(false);

        boolean check = googleApiChecker.check();

        Assert.assertEquals(false, check);
        verify(googleApiClient).connect();
    }

    @Test
    public void checkNotConnected(){
        when(googleApiClient.isConnected()).thenReturn(false);
        when(googleApiClient.isConnecting()).thenReturn(true);

        boolean check = googleApiChecker.check();
        Assert.assertEquals(false, check);
        verify(googleApiClient, never()).connect();
    }

    @Test
    public void checkConnected(){
        when(googleApiClient.isConnected()).thenReturn(true);

        Assert.assertEquals(true, googleApiChecker.check());

        verify(googleApiClient, never()).connect();
    }
}