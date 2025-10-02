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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
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

    // Mocked dependencies
    @Mock(answer = RETURNS_DEEP_STUBS) private Config configMock;
    @Mock private PushwooshRepository pushwooshRepositoryMock;
    @Mock private PushwooshPlatform pushwooshPlatformMock;
    @Mock private WorkerParameters workerParametersMock;

    private static final String TEST_HASH = "test-hash-123";
    private static final String TEST_METADATA = "test-metadata";

    @Before
    public void setUp() throws Exception {
        // Enable logging
        ShadowLog.stream = System.out;
        MockitoAnnotations.openMocks(this);

        // Setup context
        context = spy(RuntimeEnvironment.application);
        when(context.getApplicationContext()).thenReturn(context);

        // Setup config
        when(configMock.getLogLevel()).thenReturn("NOISE");

        // Skip RepositoryModule.init - it's too complex for testing
        // We'll test the functionality through direct method calls

        // Setup platform mock
        when(pushwooshPlatformMock.pushwooshRepository()).thenReturn(pushwooshRepositoryMock);
        
        // Mock static PushwooshPlatform.getInstance()
        // Note: In real testing, you might need PowerMock or Mockito-inline for static mocking
        
        // Reset SDK state
        SdkStateProvider.getInstance().resetForTesting();
    }

    @After
    public void tearDown() throws Exception {
        SdkStateProvider.getInstance().resetForTesting();
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
                .putString(PushStatisticsWorker.DATA_EVENT_TYPE, "") // Invalid empty event type
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

    /**
     * Verifies that delivery events are successfully processed when SDK is ready.
     * Tests the complete flow: data extraction, repository call, and success result.
     */
    @Test
    public void testDeliveryEventSuccessfullyProcessedWhenSdkReady() throws Exception {
        // Given: Worker with delivery event data and SDK ready
        setupWorkerWithData(createDeliveryEventData());
        ensureSdkReady();

        // Mock PushwooshPlatform.getInstance()
        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            // Mock successful sync method result
            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromData(null));

            // When: Worker executes
            ListenableWorker.Result result = pushStatisticsWorker.doWork();

            // Then: Result should be success
            assertEquals(ListenableWorker.Result.success(), result);

            // And: Repository sync method should be called
            verify(pushwooshRepositoryMock, times(1))
                    .sendPushDeliveredSync(TEST_HASH, TEST_METADATA);
        }
    }

    /**
     * Verifies that open events are successfully processed when SDK is ready.
     * Tests the complete flow: data extraction, repository call, and success result.
     */
    @Test
    public void testOpenEventSuccessfullyProcessedWhenSdkReady() throws Exception {
        // Given: Worker with open event data and SDK ready
        setupWorkerWithData(createOpenEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            // Mock successful sync method result
            when(pushwooshRepositoryMock.sendPushOpenedSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromData(null));

            // When: Worker executes
            ListenableWorker.Result result = pushStatisticsWorker.doWork();

            // Then: Result should be success
            assertEquals(ListenableWorker.Result.success(), result);

            // And: Repository sync method should be called
            verify(pushwooshRepositoryMock, times(1))
                    .sendPushOpenedSync(TEST_HASH, TEST_METADATA);
        }
    }

    /**
     * Verifies that worker fails when event type is invalid or missing.
     * Tests input validation and ensures no repository calls are made.
     */
    @Test
    public void testWorkerFailsWhenEventTypeInvalidOrMissing() {
        // Given: Worker with invalid data
        setupWorkerWithData(createInvalidData());

        // When: Worker executes
        ListenableWorker.Result result = pushStatisticsWorker.doWork();

        // Then: Result should be failure
        assertEquals(ListenableWorker.Result.failure(), result);

        // And: No repository methods should be called
        verify(pushwooshRepositoryMock, never()).sendPushDeliveredSync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpenedSync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Verifies that worker fails when push hash is missing from input data.
     * Tests required field validation and ensures no repository calls are made.
     */
    @Test
    public void testWorkerFailsWhenPushHashMissing() {
        // Given: Worker with missing hash
        Data dataWithoutHash = new Data.Builder()
                .putString(PushStatisticsWorker.DATA_EVENT_TYPE, PushStatisticsWorker.EVENT_DELIVERY)
                .build();
        setupWorkerWithData(dataWithoutHash);

        // When: Worker executes
        ListenableWorker.Result result = pushStatisticsWorker.doWork();

        // Then: Result should be failure
        assertEquals(ListenableWorker.Result.failure(), result);

        // And: No repository methods should be called
        verify(pushwooshRepositoryMock, never()).sendPushDeliveredSync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpenedSync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Verifies that worker fails when event type is unknown.
     * Tests that only supported event types (delivery/open) are processed.
     */
    @Test
    public void testWorkerFailsWhenEventTypeUnknown() throws Exception {
        // Given: Worker with unknown event type and SDK ready
        Data unknownEventData = new Data.Builder()
                .putString(PushStatisticsWorker.DATA_EVENT_TYPE, "unknown_event")
                .putString(PushStatisticsWorker.DATA_PUSH_HASH, TEST_HASH)
                .build();
        setupWorkerWithData(unknownEventData);
        ensureSdkReady();

        // When: Worker executes
        ListenableWorker.Result result = pushStatisticsWorker.doWork();

        // Then: Result should be failure (unknown event type - don't retry)
        assertEquals(ListenableWorker.Result.failure(), result);

        // And: No repository methods should be called
        verify(pushwooshRepositoryMock, never()).sendPushDeliveredSync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpenedSync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Verifies that worker retries when SDK is not ready.
     * Tests that events are postponed until SDK initialization completes.
     */
    @Test
    public void testWorkerRetriesWhenSdkNotReady() throws Exception {
        // Given: Worker with valid data but SDK not ready
        setupWorkerWithData(createDeliveryEventData());
        // Note: Don't call ensureSdkReady() - SDK stays in INITIALIZING state

        // When: Worker executes
        ListenableWorker.Result result = pushStatisticsWorker.doWork();

        // Then: Result should be retry (SDK not ready)
        assertEquals(ListenableWorker.Result.retry(), result);

        // And: Repository method should not be called
        verify(pushwooshRepositoryMock, never()).sendPushDeliveredSync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
        verify(pushwooshRepositoryMock, never()).sendPushOpenedSync(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    /**
     * Verifies that worker succeeds when metadata is null.
     * Tests that metadata is optional and null values are handled gracefully.
     */
    @Test
    public void testWorkerSucceedsWhenMetadataIsNull() throws Exception {
        // Given: Worker with null metadata
        Data dataWithNullMetadata = new Data.Builder()
                .putString(PushStatisticsWorker.DATA_EVENT_TYPE, PushStatisticsWorker.EVENT_DELIVERY)
                .putString(PushStatisticsWorker.DATA_PUSH_HASH, TEST_HASH)
                // Note: No metadata key-value pair added, so it will be null
                .build();
        setupWorkerWithData(dataWithNullMetadata);
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            // Mock successful sync method result
            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, null))
                    .thenReturn(Result.fromData(null));

            // When: Worker executes
            ListenableWorker.Result result = pushStatisticsWorker.doWork();

            // Then: Result should be success
            assertEquals(ListenableWorker.Result.success(), result);

            // And: Repository method should be called with null metadata
            verify(pushwooshRepositoryMock, times(1))
                    .sendPushDeliveredSync(TEST_HASH, null);
        }
    }

    /**
     * Verifies that worker retries when connection error occurs (0,0 codes).
     * Tests shouldRetryException logic for network connectivity issues.
     */
    @Test
    public void testWorkerRetriesWhenConnectionErrorOccurs() throws Exception {
        // Given: Worker with valid data and SDK ready
        setupWorkerWithData(createDeliveryEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            // Test Case 1: Connection error (0,0) - should retry
            ConnectionException connectionError = new ConnectionException("No network", 0, 0);
            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromException(connectionError));

            ListenableWorker.Result result = pushStatisticsWorker.doWork();
            assertEquals("Connection error (0,0) should retry", ListenableWorker.Result.retry(), result);
        }
    }

    /**
     * Verifies that worker retries when server error occurs (5xx codes).
     * Tests shouldRetryException logic for temporary server failures.
     */
    @Test
    public void testWorkerRetriesWhenServerErrorOccurs() throws Exception {
        // Given: Worker with valid data and SDK ready
        setupWorkerWithData(createDeliveryEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            // Test Case 2: Server error 500 - should retry
            ConnectionException serverError500 = new ConnectionException("Server error", 500, 0);
            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromException(serverError500));

            ListenableWorker.Result result = pushStatisticsWorker.doWork();
            assertEquals("Server error 500 should retry", ListenableWorker.Result.retry(), result);
        }
    }

    /**
     * Verifies that worker fails when client error occurs (4xx codes).
     * Tests shouldRetryException logic for permanent client errors.
     */
    @Test
    public void testWorkerFailsWhenClientErrorOccurs() throws Exception {
        // Given: Worker with valid data and SDK ready
        setupWorkerWithData(createDeliveryEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            // Test Case 3: Client error 400 - should not retry
            ConnectionException clientError400 = new ConnectionException("Bad request", 400, 0);
            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromException(clientError400));

            ListenableWorker.Result result = pushStatisticsWorker.doWork();
            assertEquals("Client error 400 should not retry", ListenableWorker.Result.failure(), result);
        }
    }

    /**
     * Verifies that worker fails when generic NetworkException occurs.
     * Tests shouldRetryException logic for non-ConnectionException errors.
     */
    @Test
    public void testWorkerFailsWhenGenericNetworkExceptionOccurs() throws Exception {
        // Given: Worker with valid data and SDK ready
        setupWorkerWithData(createDeliveryEventData());
        ensureSdkReady();

        try (MockedStatic<PushwooshPlatform> platformMock = Mockito.mockStatic(PushwooshPlatform.class)) {
            platformMock.when(PushwooshPlatform::getInstance).thenReturn(pushwooshPlatformMock);

            // Test Case 4: Generic NetworkException (not ConnectionException) - should not retry
            NetworkException genericError = new NetworkException("Generic network error");
            when(pushwooshRepositoryMock.sendPushDeliveredSync(TEST_HASH, TEST_METADATA))
                    .thenReturn(Result.fromException(genericError));

            ListenableWorker.Result result = pushStatisticsWorker.doWork();
            assertEquals("Generic NetworkException should not retry", ListenableWorker.Result.failure(), result);
        }
    }

    // ========== Direct tests for shouldRetryException method ==========

    /**
     * Verifies that shouldRetryException returns false for non-ConnectionException.
     * Tests that only ConnectionException instances are considered for retry.
     */
    @Test
    public void testShouldRetryExceptionReturnsFalseForNonConnectionException() {
        // Given: Generic NetworkException (not ConnectionException)
        NetworkException genericException = new NetworkException("Generic network error");

        // When: Checking if should retry
        boolean shouldRetry = PushStatisticsWorker.shouldRetryException(genericException);

        // Then: Should not retry
        assertFalse("Generic NetworkException should not be retried", shouldRetry);
    }

    /**
     * Verifies that shouldRetryException returns true for connection errors (0,0).
     * Tests retry logic for network connectivity issues.
     */
    @Test
    public void testShouldRetryExceptionReturnsTrueForConnectionError() {
        // Given: ConnectionException with (0,0) codes indicating network issue
        ConnectionException connectionException = new ConnectionException("No network", 0, 0);

        // When: Checking if should retry
        boolean shouldRetry = PushStatisticsWorker.shouldRetryException(connectionException);

        // Then: Should retry
        assertTrue("Connection error (0,0) should be retried", shouldRetry);
    }

    /**
     * Verifies that shouldRetryException returns true for 5xx server errors.
     * Tests retry logic for temporary server failures.
     */
    @Test
    public void testShouldRetryExceptionReturnsTrueForServerErrors() {
        // Test different 5xx server errors
        int[] serverErrorCodes = {500, 502, 503, 504};

        for (int errorCode : serverErrorCodes) {
            // Given: ConnectionException with server error code
            ConnectionException serverException = new ConnectionException("Server error", errorCode, 0);

            // When: Checking if should retry
            boolean shouldRetry = PushStatisticsWorker.shouldRetryException(serverException);

            // Then: Should retry
            assertTrue("Server error " + errorCode + " should be retried", shouldRetry);
        }
    }

    /**
     * Verifies that shouldRetryException returns true for retriable client errors.
     * Tests retry logic for specific client errors that should be retried.
     */
    @Test
    public void testShouldRetryExceptionReturnsTrueForRetriableClientErrors() {
        // Test retriable client errors
        int[] retriableClientCodes = {408, 429};

        for (int errorCode : retriableClientCodes) {
            // Given: ConnectionException with retriable client error code
            ConnectionException clientException = new ConnectionException("Client error", errorCode, 0);

            // When: Checking if should retry
            boolean shouldRetry = PushStatisticsWorker.shouldRetryException(clientException);

            // Then: Should retry
            assertTrue("Client error " + errorCode + " should be retried", shouldRetry);
        }
    }

    /**
     * Verifies that shouldRetryException returns false for non-retriable client errors.
     * Tests that permanent client errors (4xx) are not retried.
     */
    @Test
    public void testShouldRetryExceptionReturnsFalseForNonRetriableClientErrors() {
        // Test non-retriable client errors
        int[] nonRetriableClientCodes = {400, 401, 403, 404};

        for (int errorCode : nonRetriableClientCodes) {
            // Given: ConnectionException with non-retriable client error code
            ConnectionException clientException = new ConnectionException("Client error", errorCode, 0);

            // When: Checking if should retry
            boolean shouldRetry = PushStatisticsWorker.shouldRetryException(clientException);

            // Then: Should not retry
            assertFalse("Client error " + errorCode + " should not be retried", shouldRetry);
        }
    }

    /**
     * Verifies that shouldRetryException returns false for unknown HTTP codes.
     * Tests that non-standard HTTP codes are not retried.
     */
    @Test
    public void testShouldRetryExceptionReturnsFalseForUnknownHttpCodes() {
        // Test unknown/non-standard HTTP codes
        int[] unknownCodes = {418, 299, 199, 600};

        for (int errorCode : unknownCodes) {
            // Given: ConnectionException with unknown HTTP code
            ConnectionException unknownException = new ConnectionException("Unknown error", errorCode, 0);

            // When: Checking if should retry
            boolean shouldRetry = PushStatisticsWorker.shouldRetryException(unknownException);

            // Then: Should not retry
            assertFalse("Unknown HTTP code " + errorCode + " should not be retried", shouldRetry);
        }
    }

    /**
     * Verifies that InputData helper creates correct data structure.
     * Tests the createInputData utility method for WorkManager integration.
     */
    @Test
    public void testInputDataHelperCreatesCorrectDataStructure() {
        // When: Creating input data for delivery event
        Data deliveryData = PushStatisticsWorker.createInputData(
                PushStatisticsWorker.EVENT_DELIVERY, TEST_HASH, TEST_METADATA);

        // Then: Data should contain correct values
        assertEquals(PushStatisticsWorker.EVENT_DELIVERY,
                deliveryData.getString(PushStatisticsWorker.DATA_EVENT_TYPE));
        assertEquals(TEST_HASH,
                deliveryData.getString(PushStatisticsWorker.DATA_PUSH_HASH));
        assertEquals(TEST_METADATA,
                deliveryData.getString(PushStatisticsWorker.DATA_METADATA));

        // When: Creating input data for open event
        Data openData = PushStatisticsWorker.createInputData(
                PushStatisticsWorker.EVENT_OPEN, TEST_HASH, null);

        // Then: Data should contain correct values including null metadata
        assertEquals(PushStatisticsWorker.EVENT_OPEN,
                openData.getString(PushStatisticsWorker.DATA_EVENT_TYPE));
        assertEquals(TEST_HASH,
                openData.getString(PushStatisticsWorker.DATA_PUSH_HASH));
        assertNull(openData.getString(PushStatisticsWorker.DATA_METADATA));
    }
}
