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

package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.pushwoosh.function.Result;
import com.pushwoosh.internal.SdkStateProvider;
import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.network.NetworkException;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.repository.PushwooshRepository;

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
public class PushStatisticsWorkerTest {

    private PushStatisticsWorker pushStatisticsWorker;
    private Context context;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private Config configMock;

    @Mock
    private PushwooshRepository pushwooshRepositoryMock;

    @Mock
    private PushwooshPlatform pushwooshPlatformMock;

    @Mock
    private WorkerParameters workerParametersMock;

    private static final String TEST_HASH = "test-hash-123";
    private static final String TEST_METADATA = "test-metadata";

    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        mocks = MockitoAnnotations.openMocks(this);

        context = spy(RuntimeEnvironment.application);
        when(context.getApplicationContext()).thenReturn(context);

        when(configMock.getLogLevel()).thenReturn("NOISE");

        when(pushwooshPlatformMock.pushwooshRepository()).thenReturn(pushwooshRepositoryMock);

        SdkStateProvider.getInstance().resetForTesting();
    }

    @After
    public void tearDown() throws Exception {
        SdkStateProvider.getInstance().resetForTesting();
        mocks.close();
    }

    private Data createDeliveryEventData() {
        return new Data.Builder()
                .putString(PushStatisticsWorker.DATA_EVENT_TYPE, PushStatisticsWorker.EVENT_DELIVERY)
                .putString(PushStatisticsWorker.DATA_PUSH_HASH, TEST_HASH)
                .putString(PushStatisticsWorker.DATA_METADATA, TEST_METADATA)
                .build();
    }

    private Data createOpenEventData() {
        return new Data.Builder()
                .putString(PushStatisticsWorker.DATA_EVENT_TYPE, PushStatisticsWorker.EVENT_OPEN)
                .putString(PushStatisticsWorker.DATA_PUSH_HASH, TEST_HASH)
                .putString(PushStatisticsWorker.DATA_METADATA, TEST_METADATA)
                .build();
    }

    private Data createInvalidData() {
        return new Data.Builder()
                .putString(PushStatisticsWorker.DATA_EVENT_TYPE, "")
                .putString(PushStatisticsWorker.DATA_PUSH_HASH, TEST_HASH)
                .build();
    }

    private void setupWorkerWithData(Data data) {
        when(workerParametersMock.getInputData()).thenReturn(data);
        pushStatisticsWorker = new PushStatisticsWorker(context, workerParametersMock);
    }

    private void ensureSdkReady() throws InterruptedException {
        SdkStateProvider.getInstance().setReady();

        CountDownLatch latch = new CountDownLatch(1);
        SdkStateProvider.getInstance().executeOrQueue(latch::countDown);
        assertTrue("SDK initialization should be ok", latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void testDeliveryEventSuccessfullyProcessedWhenSdkReady() throws Exception {
        setupWorkerWithData(createDeliveryEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromData(null));

            ListenableWorker.Result result = pushStatisticsWorker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            verify(pushwooshRepositoryMock, times(1)).sendPushDeliveredSync(TEST_HASH, TEST_METADATA);
        }
    }

    @Test
    public void testOpenEventSuccessfullyProcessedWhenSdkReady() throws Exception {
        setupWorkerWithData(createOpenEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            when(pushwooshRepositoryMock.sendPushOpenedSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromData(null));

            ListenableWorker.Result result = pushStatisticsWorker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            verify(pushwooshRepositoryMock, times(1)).sendPushOpenedSync(TEST_HASH, TEST_METADATA);
        }
    }

    @Test
    public void testWorkerFailsWhenEventTypeInvalidOrMissing() {
        setupWorkerWithData(createInvalidData());

        ListenableWorker.Result result = pushStatisticsWorker.doWork();

        assertEquals(ListenableWorker.Result.failure(), result);
        verify(pushwooshRepositoryMock, never()).sendPushDeliveredSync(anyString(), anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpenedSync(anyString(), anyString());
    }

    @Test
    public void testWorkerFailsWhenEventTypeUnknown() throws Exception {
        Data unknownEventData = new Data.Builder()
                .putString(PushStatisticsWorker.DATA_EVENT_TYPE, "unknown_event")
                .putString(PushStatisticsWorker.DATA_PUSH_HASH, TEST_HASH)
                .build();
        setupWorkerWithData(unknownEventData);
        ensureSdkReady();

        ListenableWorker.Result result = pushStatisticsWorker.doWork();

        assertEquals(ListenableWorker.Result.failure(), result);
        verify(pushwooshRepositoryMock, never()).sendPushDeliveredSync(anyString(), anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpenedSync(anyString(), anyString());
    }

    @Test
    public void testWorkerRetriesWhenSdkNotReady() throws Exception {
        setupWorkerWithData(createDeliveryEventData());

        ListenableWorker.Result result = pushStatisticsWorker.doWork();

        assertEquals(ListenableWorker.Result.retry(), result);
        verify(pushwooshRepositoryMock, never()).sendPushDeliveredSync(anyString(), anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpenedSync(anyString(), anyString());
    }

    @Test
    public void testWorkerRetriesWhenServerErrorOccurs() throws Exception {
        setupWorkerWithData(createDeliveryEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            ConnectionException serverError500 = new ConnectionException("Server error", 500, 0);
            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromException(serverError500));

            ListenableWorker.Result result = pushStatisticsWorker.doWork();
            assertEquals("Server error 500 should retry", ListenableWorker.Result.retry(), result);
        }
    }

    // Symmetric to testWorkerRetriesWhenServerErrorOccurs: pins down that doWork() actually wires
    // shouldRetryException==false back to Result.failure() (helper-only tests would survive a mutation
    // that defaults the integration path to retry).
    @Test
    public void testWorkerFailsWhenNonRetriableClientErrorOccurs() throws Exception {
        setupWorkerWithData(createDeliveryEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            ConnectionException clientError404 = new ConnectionException("Not found", 404, 0);
            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromException(clientError404));

            ListenableWorker.Result result = pushStatisticsWorker.doWork();
            assertEquals("Client error 404 should not retry", ListenableWorker.Result.failure(), result);
        }
    }

    @Test
    public void testShouldRetryExceptionReturnsFalseForNonConnectionException() {
        NetworkException genericException = new NetworkException("Generic network error");

        boolean shouldRetry = PushStatisticsWorker.shouldRetryException(genericException);

        assertFalse("Generic NetworkException should not be retried", shouldRetry);
    }

    @Test
    public void testShouldRetryExceptionReturnsTrueForConnectionError() {
        ConnectionException connectionException = new ConnectionException("No network", 0, 0);

        boolean shouldRetry = PushStatisticsWorker.shouldRetryException(connectionException);

        assertTrue("Connection error (0,0) should be retried", shouldRetry);
    }

    @Test
    public void testShouldRetryExceptionReturnsTrueForServerErrors() {
        int[] serverErrorCodes = {500, 502, 503, 504};

        for (int errorCode : serverErrorCodes) {
            ConnectionException serverException = new ConnectionException("Server error", errorCode, 0);
            boolean shouldRetry = PushStatisticsWorker.shouldRetryException(serverException);
            assertTrue("Server error " + errorCode + " should be retried", shouldRetry);
        }
    }

    // Pins down that retry decision uses HTTP status — non-zero pushwoosh status with a retriable HTTP code
    // still retries. Without this, a mutation like `if (pushwooshStatus != 0) return false` would survive.
    @Test
    public void testShouldRetryExceptionReturnsTrueForServerErrorWithNonZeroPushwooshStatus() {
        ConnectionException serverException = new ConnectionException("Server error", 503, 42);

        boolean shouldRetry = PushStatisticsWorker.shouldRetryException(serverException);

        assertTrue("503 with pushwoosh status 42 should still be retried", shouldRetry);
    }

    @Test
    public void testShouldRetryExceptionReturnsTrueForRetriableClientErrors() {
        int[] retriableClientCodes = {408, 429};

        for (int errorCode : retriableClientCodes) {
            ConnectionException clientException = new ConnectionException("Client error", errorCode, 0);
            boolean shouldRetry = PushStatisticsWorker.shouldRetryException(clientException);
            assertTrue("Client error " + errorCode + " should be retried", shouldRetry);
        }
    }

    @Test
    public void testShouldRetryExceptionReturnsFalseForNonRetriableClientErrors() {
        int[] nonRetriableClientCodes = {400, 401, 403, 404};

        for (int errorCode : nonRetriableClientCodes) {
            ConnectionException clientException = new ConnectionException("Client error", errorCode, 0);
            boolean shouldRetry = PushStatisticsWorker.shouldRetryException(clientException);
            assertFalse("Client error " + errorCode + " should not be retried", shouldRetry);
        }
    }

    @Test
    public void testWorkerFailsWhenRetryLimitReached() throws Exception {
        setupWorkerWithData(createDeliveryEventData());
        when(workerParametersMock.getRunAttemptCount()).thenReturn(5);
        ensureSdkReady();

        ListenableWorker.Result result = pushStatisticsWorker.doWork();

        assertEquals(ListenableWorker.Result.failure(), result);
        verify(pushwooshRepositoryMock, never()).sendPushDeliveredSync(anyString(), anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpenedSync(anyString(), anyString());
    }
}
