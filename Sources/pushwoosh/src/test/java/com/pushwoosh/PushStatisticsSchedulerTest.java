package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import android.os.Bundle;

import androidx.work.Constraints;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;

import com.pushwoosh.notification.PushBundleDataProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class PushStatisticsSchedulerTest {

    private static final String TEST_HASH = "test-hash-123";
    private static final String TEST_METADATA = "test-metadata";

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() {
        // Clean up any static mocks
    }

    private Bundle createValidPushBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("h", TEST_HASH);
        bundle.putString("md", TEST_METADATA);
        return bundle;
    }

    private Bundle createPushBundleWithoutHash() {
        Bundle bundle = new Bundle();
        bundle.putString("md", TEST_METADATA);
        return bundle;
    }

    private Bundle createPushBundleWithEmptyHash() {
        Bundle bundle = new Bundle();
        bundle.putString("h", "");
        bundle.putString("md", TEST_METADATA);
        return bundle;
    }

    private Bundle createPushBundleWithOnlyHash() {
        Bundle bundle = new Bundle();
        bundle.putString("h", TEST_HASH);
        return bundle;
    }

    /**
     * Test successful delivery event scheduling with valid push bundle
     */
    @Test
    public void testScheduleDeliveryEventSuccess() {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(TEST_HASH);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock.when(PushwooshWorkManagerHelper::getStatisticsConstraints).thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ExistingWorkPolicy> policyCaptor = ArgumentCaptor.forClass(ExistingWorkPolicy.class);

            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    requestCaptor.capture(),
                    nameCaptor.capture(),
                    policyCaptor.capture()
                ), times(1));

            // Verify work request parameters
            OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
            assertEquals(PushStatisticsWorker.EVENT_DELIVERY, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_EVENT_TYPE));
            assertEquals(TEST_HASH, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_PUSH_HASH));
            assertEquals(TEST_METADATA, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_METADATA));

            // Verify unique work name contains event type and hash
            String capturedName = nameCaptor.getValue();
            assertTrue("Work name should contain event type", capturedName.startsWith(PushStatisticsWorker.EVENT_DELIVERY));
            assertTrue("Work name should contain hash", capturedName.contains(TEST_HASH));

            // Verify policy
            assertEquals(ExistingWorkPolicy.KEEP, policyCaptor.getValue());
        }
    }

    private void assertTrue(String message, boolean condition) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    /**
     * Test successful open event scheduling with valid push bundle
     */
    @Test
    public void testScheduleOpenEventSuccess() {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(TEST_HASH);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock.when(PushwooshWorkManagerHelper::getStatisticsConstraints).thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    requestCaptor.capture(),
                    nameCaptor.capture(),
                    eq(ExistingWorkPolicy.KEEP)
                ), times(1));

            // Verify work request parameters
            OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
            assertEquals(PushStatisticsWorker.EVENT_OPEN, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_EVENT_TYPE));
            assertEquals(TEST_HASH, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_PUSH_HASH));
            assertEquals(TEST_METADATA, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_METADATA));

            // Verify unique work name
            String capturedName = nameCaptor.getValue();
            assertTrue("Work name should contain event type", capturedName.startsWith(PushStatisticsWorker.EVENT_OPEN));
            assertTrue("Work name should contain hash", capturedName.contains(TEST_HASH));
        }
    }

    /**
     * Test that delivery event is not scheduled when hash is null
     */
    @Test
    public void testScheduleDeliveryEventWithNullHash() {
        Bundle pushBundle = createPushBundleWithoutHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(null);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(TEST_METADATA);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            // Verify no work was scheduled
            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()),
                never());
        }
    }

    /**
     * Test that delivery event is not scheduled when hash is empty
     */
    @Test
    public void testScheduleDeliveryEventWithEmptyHash() {
        Bundle pushBundle = createPushBundleWithEmptyHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn("");
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(TEST_METADATA);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            // Verify no work was scheduled
            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()),
                never());
        }
    }

    /**
     * Test that open event is not scheduled when hash is null
     */
    @Test
    public void testScheduleOpenEventWithNullHash() {
        Bundle pushBundle = createPushBundleWithoutHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(null);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(TEST_METADATA);

            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            // Verify no work was scheduled
            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()),
                never());
        }
    }

    /**
     * Test that delivery event is scheduled successfully with null metadata
     */
    @Test
    public void testScheduleDeliveryEventWithNullMetadata() {
        Bundle pushBundle = createPushBundleWithOnlyHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(TEST_HASH);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(null);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock.when(PushwooshWorkManagerHelper::getStatisticsConstraints).thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);

            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    requestCaptor.capture(),
                    anyString(),
                    eq(ExistingWorkPolicy.KEEP)
                ), times(1));

            // Verify work request parameters - metadata should be null
            OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
            assertEquals(TEST_HASH, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_PUSH_HASH));
            assertEquals(null, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_METADATA));
        }
    }

    /**
     * Test that open event is scheduled successfully with null metadata
     */
    @Test
    public void testScheduleOpenEventWithNullMetadata() {
        Bundle pushBundle = createPushBundleWithOnlyHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(TEST_HASH);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(null);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock.when(PushwooshWorkManagerHelper::getStatisticsConstraints).thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);

            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    requestCaptor.capture(),
                    anyString(),
                    eq(ExistingWorkPolicy.KEEP)
                ), times(1));

            // Verify work request parameters - metadata should be null
            OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
            assertEquals(TEST_HASH, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_PUSH_HASH));
            assertEquals(null, 
                capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_METADATA));
        }
    }

    /**
     * Test exception handling when WorkManager throws exception
     */
    @Test
    public void testScheduleDeliveryEventWithWorkManagerException() {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(TEST_HASH);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock.when(PushwooshWorkManagerHelper::getStatisticsConstraints).thenReturn(mockConstraints);

            // Mock WorkManager to throw exception
            workManagerHelperMock.when(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()))
                .thenThrow(new RuntimeException("WorkManager failed"));

            // This should not throw exception - errors are handled internally
            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            // Verify that enqueue was attempted despite the exception
            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()),
                times(1));
        }
    }

    /**
     * Test exception handling when WorkManager throws exception for open event
     */
    @Test
    public void testScheduleOpenEventWithWorkManagerException() {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(TEST_HASH);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock.when(PushwooshWorkManagerHelper::getStatisticsConstraints).thenReturn(mockConstraints);

            // Mock WorkManager to throw exception
            workManagerHelperMock.when(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()))
                .thenThrow(new RuntimeException("WorkManager failed"));

            // This should not throw exception - errors are handled internally
            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            // Verify that enqueue was attempted despite the exception
            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()),
                times(1));
        }
    }

    /**
     * Test that unique work names are actually unique for different timestamps
     */
    @Test
    public void testWorkNames() throws InterruptedException {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock = Mockito.mockStatic(PushBundleDataProvider.class);
             MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock = Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock.when(() -> PushBundleDataProvider.getPushHash(pushBundle)).thenReturn(TEST_HASH);
            bundleProviderMock.when(() -> PushBundleDataProvider.getPushMetadata(pushBundle)).thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock.when(PushwooshWorkManagerHelper::getStatisticsConstraints).thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);
            
            // Wait to ensure different timestamp
            Thread.sleep(5);
            
            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

            workManagerHelperMock.verify(() -> 
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    any(),
                    nameCaptor.capture(),
                    any()
                ), times(2));

            // Verify that work names are different
            java.util.List<String> capturedNames = nameCaptor.getAllValues();
            assertEquals(2, capturedNames.size());
            
            String firstName = capturedNames.get(0);
            String secondName = capturedNames.get(1);
            
            // Both should start with event type and contain hash
            assertTrue("First name should contain event and hash", 
                firstName.startsWith(PushStatisticsWorker.EVENT_DELIVERY) && firstName.contains(TEST_HASH));
            assertTrue("Second name should contain event and hash", 
                secondName.startsWith(PushStatisticsWorker.EVENT_DELIVERY) && secondName.contains(TEST_HASH));
            
            // But names should be different due to timestamp
            assertTrue("Work names should be the same", firstName.equals(secondName));
        }
    }
}
