package com.pushwoosh;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import androidx.work.Configuration;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.TestDriver;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.pushwoosh.internal.network.ConnectionException;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.MockConfig;
import com.pushwoosh.repository.MessageDeliveredRequest;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.RequestManagerMock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = "AndroidManifest.xml")
public class MessageDeliveryEventTest {

    // Mocked dependencies
    private PlatformTestManager sdk;
    private Config configMock;
    private RequestManagerMock requestManagerMock;
    private WorkManager workManager;
    private TestDriver testDriver;

    // Test data
    private static final String TEST_HASH = "test_hash_456";
    private static final String TEST_METADATA = "test_metadata";
    private static final String TEST_PUSH_TITLE = "Test Push";
    private static final String TEST_PUSH_MESSAGE = "This is a integration test push message";

    @Before
    public void setUp() throws Exception {
        // Create mock config
        setupConfig();

        // Initialize SDK using PlatformTestManager - this includes WorkManager setup
        sdk = new PlatformTestManager(configMock);
        sdk.onApplicationCreated();

        requestManagerMock = sdk.getRequestManager();

        Context context = AndroidPlatformModule.getApplicationContext();
        if (context == null) {
            throw new RuntimeException("Context is null");
        }

        setupWorkManager(context);
    }

    private void setupConfig() {
        configMock = MockConfig.createMock();
        when(configMock.getLogLevel()).thenReturn("NOISE");
    }

    private void setupWorkManager(Context ctx) {
        Configuration config = new Configuration.Builder()
                .setExecutor(new SynchronousExecutor())
                .setTaskExecutor(new SynchronousExecutor())
                .build();

        WorkManagerTestInitHelper.initializeTestWorkManager(ctx, config);

        workManager = WorkManager.getInstance(ctx);
        testDriver = WorkManagerTestInitHelper.getTestDriver(ctx);
    }

    @After
    public void tearDown() throws Exception {
        if (sdk != null) {
            sdk.tearDown();
        }
    }

    private Bundle createTestPushBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("p", TEST_HASH);
        bundle.putString("md", TEST_METADATA);
        bundle.putString("pw_msg", "1");
        bundle.putString("title", TEST_PUSH_TITLE);
        bundle.putString("message", TEST_PUSH_MESSAGE);
        return bundle;
    }

    private void waitForTasks() {
        try {
            // Let any scheduled tasks execute
            Thread.sleep(500);

            // Force execution of enqueued work using TestDriver
            try {
                // Get all enqueued work and force execution
                java.util.List<androidx.work.WorkInfo> enqueuedWork = workManager.getWorkInfos(
                        androidx.work.WorkQuery.Builder
                                .fromStates(Collections.singletonList(WorkInfo.State.ENQUEUED))
                                .build()
                ).get();

                System.out.println("Forcing execution of " + enqueuedWork.size() + " enqueued tasks");
                for (androidx.work.WorkInfo workInfo : enqueuedWork) {
                    try {
                        // Force execution using TestDriver
                        testDriver.setAllConstraintsMet(workInfo.getId());
                        System.out.println("Forced constraints met for work: " + workInfo.getId());

                        // Additional force for immediate execution
                        testDriver.setInitialDelayMet(workInfo.getId());
                        System.out.println("Forced initial delay met for work: " + workInfo.getId());

                    } catch (Exception e) {
                        System.out.println("Failed to force work " + workInfo.getId() + ": " + e.getMessage());
                    }
                }

                // Give time for execution
                Thread.sleep(1000);

            } catch (Exception e) {
                System.out.println("Error forcing work execution: " + e.getMessage());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Verifies that delivery event flow successfully sends message delivered request.
     * Tests basic message delivery statistics tracking functionality.
     */
    @Test
    public void testDeliveryEventFlowWillCallRequestManager() {

        // Make captor for checking request
        ArgumentCaptor<MessageDeliveredRequest> c = ArgumentCaptor.forClass(MessageDeliveredRequest.class);

        // trigger MessageDeliveryEvent
        Bundle bundle = createTestPushBundle();
        PushwooshMessagingServiceHelper.sendMessageDeliveryEvent(bundle);

        // wait work manager schedule and complete tasks
        waitForTasks();

        // Verify PushwooshRequestManager.sendRequestSync was called
        verify(requestManagerMock, Mockito.times(1)).sendRequestSync(c.capture());

        // Verify arguments = MessageDeliveryRequest with correct hash and metadata
        MessageDeliveredRequest request = c.getValue();
        assertEquals(TEST_HASH, request.getHash());
        assertEquals(TEST_METADATA, request.getMetaData());
    }

    /**
     * Verifies that delivery event retries on server errors (5xx).
     * Tests WorkManager retry mechanism for transient server failures.
     */
    @Test
    public void testDeliveryEventRetryOnServerError() throws Exception {
        // Given: Mock server error (500) - should trigger retry
        ConnectionException exception = new ConnectionException("Internal Server Error ", 500, 0);
        requestManagerMock.setException(exception, MessageDeliveredRequest.class);

        // Make captor for checking request
        ArgumentCaptor<MessageDeliveredRequest> c = ArgumentCaptor.forClass(MessageDeliveredRequest.class);

        // trigger MessageDeliveryEvent
        Bundle bundle = createTestPushBundle();
        PushwooshMessagingServiceHelper.sendMessageDeliveryEvent(bundle);

        // wait work manager schedule and complete tasks
        waitForTasks();

        // Verify PushwooshRequestManager.sendRequestSync was called
        verify(requestManagerMock, Mockito.times(1)).sendRequestSync(Mockito.any());

        // wait work manager schedule and complete tasks
        waitForTasks();

        // Check if more attempts were made (WorkManager should retry on 500 error)
        // Count only sendRequestSync calls, not setException calls
        verify(requestManagerMock, Mockito.times(2)).sendRequestSync(c.capture());

        // Verify we have 2 same requests and the end
        List<MessageDeliveredRequest> requests = c.getAllValues();
        assertEquals("Total requests count mismatch", 2, requests.size());

        for (MessageDeliveredRequest r : requests) {
            assertEquals(TEST_HASH, r.getHash());
            assertEquals(TEST_METADATA, r.getMetaData());
        }
    }

    /**
     * Verifies that delivery event does not retry on client errors (4xx).
     * Tests that permanent client errors are not retried to avoid unnecessary load.
     */
    @Test
    public void testDeliveryEventNoRetryOnClientError() throws Exception {
        // Given: Mock client error (400) - should NOT trigger retry
        ConnectionException clientError = new ConnectionException("Bad Request", 400, 0);
        requestManagerMock.setException(clientError, MessageDeliveredRequest.class);

        // Make captor for checking request
        ArgumentCaptor<MessageDeliveredRequest> c = ArgumentCaptor.forClass(MessageDeliveredRequest.class);

        // trigger MessageDeliveryEvent
        Bundle pushBundle = createTestPushBundle();
        PushwooshMessagingServiceHelper.sendMessageDeliveryEvent(pushBundle);

        // wait work manager schedule and complete tasks
        waitForTasks();

        //Verify first attempt was made
        verify(requestManagerMock, Mockito.times(1)).sendRequestSync(c.capture());

        MessageDeliveredRequest request = c.getValue();
        assertEquals(TEST_HASH, request.getHash());
        assertEquals(TEST_METADATA, request.getMetaData());

        // wait work manager schedule and complete tasks
        waitForTasks();

        // Verify no additional attempts were made (should still be exactly 1)
        verify(requestManagerMock, Mockito.times(1)).sendRequestSync(Mockito.any());
    }
}
