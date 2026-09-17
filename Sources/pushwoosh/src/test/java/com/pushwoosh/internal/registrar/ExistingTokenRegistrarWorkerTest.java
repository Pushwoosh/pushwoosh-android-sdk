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

package com.pushwoosh.internal.registrar;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.notification.PushwooshNotificationManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class ExistingTokenRegistrarWorkerTest {

    private ExistingTokenRegistrarWorker worker;
    private Context context;
    private AutoCloseable mocks;

    @Mock
    private PushwooshPlatform pushwooshPlatformMock;

    @Mock
    private PushwooshNotificationManager notificationManagerMock;

    @Mock
    private WorkerParameters workerParametersMock;

    private static final String TEST_TOKEN = "fcm-token-abc";

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        mocks = MockitoAnnotations.openMocks(this);

        context = spy(RuntimeEnvironment.application);
        when(context.getApplicationContext()).thenReturn(context);

        when(pushwooshPlatformMock.notificationManager()).thenReturn(notificationManagerMock);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    private void setupWorkerWithToken(String token) {
        Data data = new Data.Builder()
                .putString(ExistingTokenRegistrarWorker.TOKEN, token)
                .build();
        when(workerParametersMock.getInputData()).thenReturn(data);
        worker = new ExistingTokenRegistrarWorker(context, workerParametersMock);
    }

    // Verifies that doWork forwards the token to the single intake with retries enabled and returns success.
    @Test
    public void testWorkerForwardsTokenWithRetriesAndReturnsSuccess() {
        setupWorkerWithToken(TEST_TOKEN);

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            verify(notificationManagerMock).onTokenReceived(TEST_TOKEN, null, true);
        }
    }
}
