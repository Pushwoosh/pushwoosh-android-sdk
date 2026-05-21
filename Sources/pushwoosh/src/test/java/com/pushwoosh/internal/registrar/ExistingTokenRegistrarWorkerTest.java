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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.SdkStateProvider;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

        SdkStateProvider.getInstance().resetForTesting();
    }

    @After
    public void tearDown() throws Exception {
        SdkStateProvider.getInstance().resetForTesting();
        mocks.close();
    }

    private void setupWorkerWithToken(String token) {
        Data data = new Data.Builder()
                .putString(ExistingTokenRegistrarWorker.TOKEN, token)
                .build();
        when(workerParametersMock.getInputData()).thenReturn(data);
        worker = new ExistingTokenRegistrarWorker(context, workerParametersMock);
    }

    private void setupWorkerWithEmptyInput() {
        when(workerParametersMock.getInputData()).thenReturn(new Data.Builder().build());
        worker = new ExistingTokenRegistrarWorker(context, workerParametersMock);
    }

    private void ensureSdkReady() throws InterruptedException {
        SdkStateProvider.getInstance().setReady();

        CountDownLatch latch = new CountDownLatch(1);
        SdkStateProvider.getInstance().executeOrQueue(latch::countDown);
        assertTrue("SDK initialization should be ok", latch.await(2, TimeUnit.SECONDS));
    }

    // Verifies that doWork returns success and forwards the token (with null tagsJson) to the notification manager when
    // SDK is ready.
    @Test
    public void testWorkerForwardsTokenAndReturnsSuccessWhenSdkReady() throws Exception {
        setupWorkerWithToken(TEST_TOKEN);
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            verify(notificationManagerMock).onExistingTokenReceived(TEST_TOKEN, null);
        }
    }

    // Verifies that doWork returns success but defers the token forward when SDK is not yet ready (fire-and-forget
    // contract).
    @Test
    public void testWorkerReturnsSuccessAndQueuesTaskWhenSdkNotReady() {
        setupWorkerWithToken(TEST_TOKEN);

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            verify(notificationManagerMock, never()).onExistingTokenReceived(anyString(), any());
        }
    }

    // Verifies that doWork returns failure and skips the facade when TOKEN key is missing from input data.
    @Test
    public void testWorkerFailsWhenTokenIsMissing() throws Exception {
        setupWorkerWithEmptyInput();
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.failure(), result);
            verifyNoInteractions(notificationManagerMock);
        }
    }

    // Verifies that doWork returns failure and skips the facade when TOKEN value is empty string.
    @Test
    public void testWorkerFailsWhenTokenIsEmptyString() throws Exception {
        setupWorkerWithToken("");
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.failure(), result);
            verifyNoInteractions(notificationManagerMock);
        }
    }

    // Verifies that doWork returns failure when the facade chain throws (outer try/catch contract).
    @Test
    public void testWorkerFailsWhenFacadeThrows() throws Exception {
        setupWorkerWithToken(TEST_TOKEN);
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);
            when(pushwooshPlatformMock.notificationManager()).thenThrow(new RuntimeException("boom"));

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.failure(), result);
        }
    }
}
