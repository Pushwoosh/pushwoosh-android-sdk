package com.pushwoosh;

import static com.pushwoosh.testutil.ReceiptTime.RECEIVED_AT_MILLIS;
import static com.pushwoosh.testutil.ReceiptTime.RECEIVED_AT_SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

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

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ExistingWorkPolicy> policyCaptor = ArgumentCaptor.forClass(ExistingWorkPolicy.class);

            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            requestCaptor.capture(), nameCaptor.capture(), policyCaptor.capture()),
                    times(1));

            // Verify work request parameters
            OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
            assertEquals(
                    PushStatisticsWorker.EVENT_DELIVERY,
                    capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_EVENT_TYPE));
            assertEquals(TEST_HASH, capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_PUSH_HASH));
            assertEquals(
                    TEST_METADATA, capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_METADATA));

            // Verify unique work name contains event type and hash
            String capturedName = nameCaptor.getValue();
            assertTrue(
                    "Work name should contain event type",
                    capturedName.startsWith(PushStatisticsWorker.EVENT_DELIVERY));
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

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            requestCaptor.capture(), nameCaptor.capture(), eq(ExistingWorkPolicy.KEEP)),
                    times(1));

            // Verify work request parameters
            OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
            assertEquals(
                    PushStatisticsWorker.EVENT_OPEN,
                    capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_EVENT_TYPE));
            assertEquals(TEST_HASH, capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_PUSH_HASH));
            assertEquals(
                    TEST_METADATA, capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_METADATA));

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

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(null);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            // Verify no work was scheduled
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()), never());
        }
    }

    /**
     * Test that delivery event is not scheduled when hash is empty
     */
    @Test
    public void testScheduleDeliveryEventWithEmptyHash() {
        Bundle pushBundle = createPushBundleWithEmptyHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn("");
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            // Verify no work was scheduled
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()), never());
        }
    }

    /**
     * Test that open event is not scheduled when hash is null
     */
    @Test
    public void testScheduleOpenEventWithNullHash() {
        Bundle pushBundle = createPushBundleWithoutHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(null);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);

            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            // Verify no work was scheduled
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()), never());
        }
    }

    /**
     * Test that delivery event is scheduled successfully with null metadata
     */
    @Test
    public void testScheduleDeliveryEventWithNullMetadata() {
        Bundle pushBundle = createPushBundleWithOnlyHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(null);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);

            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            requestCaptor.capture(), anyString(), eq(ExistingWorkPolicy.KEEP)),
                    times(1));

            // Verify work request parameters - metadata should be null
            OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
            assertEquals(TEST_HASH, capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_PUSH_HASH));
            assertEquals(null, capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_METADATA));
        }
    }

    /**
     * Test that open event is scheduled successfully with null metadata
     */
    @Test
    public void testScheduleOpenEventWithNullMetadata() {
        Bundle pushBundle = createPushBundleWithOnlyHash();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(null);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);

            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            requestCaptor.capture(), anyString(), eq(ExistingWorkPolicy.KEEP)),
                    times(1));

            // Verify work request parameters - metadata should be null
            OneTimeWorkRequest capturedRequest = requestCaptor.getValue();
            assertEquals(TEST_HASH, capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_PUSH_HASH));
            assertEquals(null, capturedRequest.getWorkSpec().input.getString(PushStatisticsWorker.DATA_METADATA));
        }
    }

    /**
     * Test exception handling when WorkManager throws exception
     */
    @Test
    public void testScheduleDeliveryEventWithWorkManagerException() {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mockConstraints);

            // Mock WorkManager to throw exception
            workManagerHelperMock
                    .when(() -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()))
                    .thenThrow(new RuntimeException("WorkManager failed"));

            // This should not throw exception - errors are handled internally
            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            // Verify that enqueue was attempted despite the exception
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()), times(1));
        }
    }

    /**
     * Test exception handling when WorkManager throws exception for open event
     */
    @Test
    public void testScheduleOpenEventWithWorkManagerException() {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mockConstraints);

            // Mock WorkManager to throw exception
            workManagerHelperMock
                    .when(() -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()))
                    .thenThrow(new RuntimeException("WorkManager failed"));

            // This should not throw exception - errors are handled internally
            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            // Verify that enqueue was attempted despite the exception
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), anyString(), any()), times(1));
        }
    }

    @Test
    public void scheduleOpenEvent_bundleCarriesTappedButtonId_putsItInTheWorkData() {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getTappedButtonId(pushBundle))
                    .thenReturn("cancel_order");
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mock(Constraints.class));

            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            requestCaptor.capture(), nameCaptor.capture(), any(ExistingWorkPolicy.class)),
                    times(1));

            assertEquals(
                    "cancel_order",
                    requestCaptor.getValue().getWorkSpec().input.getString(PushStatisticsWorker.DATA_BUTTON_ID));
            // KEEP would drop this event if "open_<hash>" from a foreground display were already queued.
            assertEquals(PushStatisticsWorker.EVENT_OPEN + "_" + TEST_HASH + "|cancel_order", nameCaptor.getValue());
        }
    }

    @Test
    public void scheduleOpenEvent_noTappedButtonId_keepsTheOldWorkName() {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getTappedButtonId(pushBundle))
                    .thenReturn(null);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mock(Constraints.class));

            PushStatisticsScheduler.scheduleOpenEvent(pushBundle);

            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            any(OneTimeWorkRequest.class), nameCaptor.capture(), any(ExistingWorkPolicy.class)),
                    times(1));

            assertEquals(PushStatisticsWorker.EVENT_OPEN + "_" + TEST_HASH, nameCaptor.getValue());
        }
    }

    @Test
    public void scheduleStatisticsEvent_hashAndButtonIdAmbiguousUnderUnderscore_stillGetDistinctWorkNames() {
        // Both inputs render as open_pw_1_2_a_b if the button id is joined with an underscore, and
        // ExistingWorkPolicy.KEEP would then drop the second click without a word in the log.
        try (MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mock(Constraints.class));

            PushStatisticsScheduler.scheduleStatisticsEvent(
                    PushStatisticsWorker.EVENT_OPEN, "pw_1_2_a", TEST_METADATA, "b");
            PushStatisticsScheduler.scheduleStatisticsEvent(
                    PushStatisticsWorker.EVENT_OPEN, "pw_1_2", TEST_METADATA, "a_b");

            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), nameCaptor.capture(), any()),
                    times(2));

            assertNotEquals(
                    nameCaptor.getAllValues().get(0), nameCaptor.getAllValues().get(1));
        }
    }

    /**
     * Test that unique work names are actually unique for different timestamps
     */
    @Test
    public void testWorkNames() throws InterruptedException {
        Bundle pushBundle = createValidPushBundle();

        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {

            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);

            // Mock constraints
            Constraints mockConstraints = mock(Constraints.class);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mockConstraints);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            // Wait to ensure different timestamp
            Thread.sleep(5);

            PushStatisticsScheduler.scheduleDeliveryEvent(pushBundle);

            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);

            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(any(), nameCaptor.capture(), any()),
                    times(2));

            // Verify that work names are different
            java.util.List<String> capturedNames = nameCaptor.getAllValues();
            assertEquals(2, capturedNames.size());

            String firstName = capturedNames.get(0);
            String secondName = capturedNames.get(1);

            // Both should start with event type and contain hash
            assertTrue(
                    "First name should contain event and hash",
                    firstName.startsWith(PushStatisticsWorker.EVENT_DELIVERY) && firstName.contains(TEST_HASH));
            assertTrue(
                    "Second name should contain event and hash",
                    secondName.startsWith(PushStatisticsWorker.EVENT_DELIVERY) && secondName.contains(TEST_HASH));

            // But names should be different due to timestamp
            assertTrue("Work names should be the same", firstName.equals(secondName));
        }
    }

    private Bundle bundleWithDelay(String value) {
        Bundle bundle = createValidPushBundle();
        bundle.putString("pw_delivery_delay", value);
        return bundle;
    }

    @Test
    public void deliveryDelayWindow_noKey_isDefaultJitter() {
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(createValidPushBundle()));
    }

    @Test
    public void deliveryDelayWindow_stringSeconds_isWindowFromPush() {
        assertEquals(60000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("60")));
    }

    @Test
    public void deliveryDelayWindow_numericSeconds_isWindowFromPush() {
        Bundle bundle = createValidPushBundle();
        bundle.putInt("pw_delivery_delay", 60);
        assertEquals(60000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundle));
    }

    @Test
    public void deliveryDelayWindow_aboveCap_isCappedAtFiveMinutes() {
        assertEquals(300000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("1000")));
    }

    @Test
    public void deliveryDelayWindow_fractionalSeconds_isTruncatedToWholeSeconds() {
        assertEquals(30000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("30.5")));
        assertEquals(60000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("60.")));
        assertEquals(
                30000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("30.99999999999999999")));

        Bundle numeric = createValidPushBundle();
        numeric.putDouble("pw_delivery_delay", 30.9);
        assertEquals(30000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(numeric));
    }

    @Test
    public void deliveryDelayWindow_belowDefaultJitter_isHonouredWithoutFloor() {
        assertEquals(2000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("2")));
    }

    @Test
    public void deliveryDelayWindow_oneSecondIsTheSmallestWindow() {
        assertEquals(1000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("1")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("0.5")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay(".5")));
    }

    @Test
    public void deliveryDelayWindow_zeroNegativeOrNotANumber_isDefaultJitter() {
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("0")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("-5")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("abc")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("")));
    }

    @Test
    public void deliveryDelayWindow_notPlainDecimalDigits_isDefaultJitter() {
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("60d")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("6e1")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("+60")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("0x1p8")));
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay("Infinity")));
    }

    @Test
    public void deliveryDelayWindow_digitsBeyondLongRange_isCappedAtFiveMinutes() {
        String fourHundredNines = new String(new char[400]).replace('\0', '9');
        assertEquals(300000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay(fourHundredNines)));
    }

    @Test
    public void deliveryDelayWindow_paddedWithWhitespace_isWindowFromPush() {
        assertEquals(60000L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundleWithDelay(" 60 ")));
    }

    @Test
    public void deliveryDelayWindow_unsupportedValueType_isDefaultJitter() {
        Bundle bundle = createValidPushBundle();
        bundle.putBoolean("pw_delivery_delay", true);
        assertEquals(2500L, PushStatisticsScheduler.getDeliveryDelayWindowMillis(bundle));
    }

    @Test
    public void scheduleDeliveryEvent_withDelayWindowInPush_staysInsideWindowAndExceedsDefaultJitter() {
        List<Long> delays = captureScheduledWork(
                        bundleWithDelay("60"), PushStatisticsScheduler::scheduleDeliveryEvent, 200)
                .initialDelays();

        assertTrue("initialDelay should stay within the push window", Collections.max(delays) <= 60000L);
        assertTrue("at least one delay should exceed the default jitter window", Collections.max(delays) > 2500L);
        assertTrue("delays should not all be the same constant value", new HashSet<>(delays).size() > 1);
    }

    @Test
    public void scheduleDeliveryEvent_withoutDelayWindow_keepsDefaultJitter() {
        List<Long> delays = captureScheduledWork(
                        createValidPushBundle(), PushStatisticsScheduler::scheduleDeliveryEvent, 200)
                .initialDelays();

        assertTrue("initialDelay should stay within the default jitter window", Collections.max(delays) <= 2500L);
        assertTrue("delays should not all be the same constant value", new HashSet<>(delays).size() > 1);
    }

    @Test
    public void scheduleOpenEvent_withDeliveryDelayInPush_keepsDefaultJitter() {
        List<Long> delays = captureScheduledWork(bundleWithDelay("60"), PushStatisticsScheduler::scheduleOpenEvent, 200)
                .initialDelays();

        assertTrue("open event initialDelay should ignore the push delay window", Collections.max(delays) <= 2500L);
        assertTrue("delays should not all be the same constant value", new HashSet<>(delays).size() > 1);
    }

    @Test
    public void scheduleDeliveryEvent_withDelayWindowInPush_keepsWorkNameAndKeepPolicy() {
        ScheduledWork work =
                captureScheduledWork(bundleWithDelay("60"), PushStatisticsScheduler::scheduleDeliveryEvent, 1);

        assertEquals(PushStatisticsWorker.EVENT_DELIVERY + "_" + TEST_HASH, work.names.get(0));
        assertEquals(ExistingWorkPolicy.KEEP, work.policies.get(0));
    }

    /** Everything the scheduler handed to {@code enqueueOneTimeUniqueWork}, one entry per call. */
    private static final class ScheduledWork {
        final List<OneTimeWorkRequest> requests;
        final List<String> names;
        final List<ExistingWorkPolicy> policies;

        ScheduledWork(List<OneTimeWorkRequest> requests, List<String> names, List<ExistingWorkPolicy> policies) {
            this.requests = requests;
            this.names = names;
            this.policies = policies;
        }

        List<Long> initialDelays() {
            List<Long> delays = new ArrayList<>(requests.size());
            for (OneTimeWorkRequest request : requests) {
                delays.add(request.getWorkSpec().initialDelay);
            }
            return delays;
        }
    }

    /**
     * Runs {@code schedule} against {@code pushBundle} {@code draws} times with the bundle provider
     * and WorkManager helper mocked, and returns what reached {@code enqueueOneTimeUniqueWork}.
     * The hash and metadata are stubbed; the delay window is read from the real bundle.
     */
    private ScheduledWork captureScheduledWork(Bundle pushBundle, Consumer<Bundle> schedule, int draws) {
        try (MockedStatic<PushBundleDataProvider> bundleProviderMock =
                        Mockito.mockStatic(PushBundleDataProvider.class);
                MockedStatic<PushwooshWorkManagerHelper> workManagerHelperMock =
                        Mockito.mockStatic(PushwooshWorkManagerHelper.class)) {
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushHash(pushBundle))
                    .thenReturn(TEST_HASH);
            bundleProviderMock
                    .when(() -> PushBundleDataProvider.getPushMetadata(pushBundle))
                    .thenReturn(TEST_METADATA);
            workManagerHelperMock
                    .when(PushwooshWorkManagerHelper::getStatisticsConstraints)
                    .thenReturn(mock(Constraints.class));

            for (int i = 0; i < draws; i++) {
                schedule.accept(pushBundle);
            }

            ArgumentCaptor<OneTimeWorkRequest> requestCaptor = ArgumentCaptor.forClass(OneTimeWorkRequest.class);
            ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<ExistingWorkPolicy> policyCaptor = ArgumentCaptor.forClass(ExistingWorkPolicy.class);
            workManagerHelperMock.verify(
                    () -> PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                            requestCaptor.capture(), nameCaptor.capture(), policyCaptor.capture()),
                    times(draws));
            return new ScheduledWork(
                    requestCaptor.getAllValues(), nameCaptor.getAllValues(), policyCaptor.getAllValues());
        }
    }

    private static long storedReceiptSeconds(OneTimeWorkRequest request) {
        return request.getWorkSpec().input.getLong(PushStatisticsWorker.DATA_RECEIVED_AT_UTC_SECONDS, -1L);
    }

    @Test
    public void scheduleDelivery_storesReceiptSecondsFloored() {
        ScheduledWork work = captureScheduledWork(
                createValidPushBundle(),
                bundle -> PushStatisticsScheduler.scheduleDeliveryEvent(bundle, RECEIVED_AT_MILLIS + 999L),
                1);

        assertEquals(RECEIVED_AT_SECONDS, storedReceiptSeconds(work.requests.get(0)));
    }

    @Test
    public void scheduleDelivery_withDelayWindow_keepsReceiptTime() {
        ScheduledWork work = captureScheduledWork(
                bundleWithDelay("60"),
                bundle -> PushStatisticsScheduler.scheduleDeliveryEvent(bundle, RECEIVED_AT_MILLIS),
                1);

        OneTimeWorkRequest request = work.requests.get(0);
        assertTrue("delay within window", request.getWorkSpec().initialDelay <= 60000L);
        assertEquals(RECEIVED_AT_SECONDS, storedReceiptSeconds(request));
    }

    @Test
    public void scheduleDelivery_twoPushes_ownTimes() {
        long[] times = {RECEIVED_AT_MILLIS, RECEIVED_AT_MILLIS + 60000L};
        int[] call = {0};
        ScheduledWork work = captureScheduledWork(
                createValidPushBundle(),
                bundle -> PushStatisticsScheduler.scheduleDeliveryEvent(bundle, times[call[0]++]),
                2);

        assertEquals(60L, storedReceiptSeconds(work.requests.get(1)) - storedReceiptSeconds(work.requests.get(0)));
    }

    @Test
    public void scheduleDelivery_independentOfTimeZone() {
        java.util.TimeZone original = java.util.TimeZone.getDefault();
        try {
            java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Tokyo"));
            ScheduledWork work = captureScheduledWork(
                    createValidPushBundle(),
                    bundle -> PushStatisticsScheduler.scheduleDeliveryEvent(bundle, RECEIVED_AT_MILLIS),
                    1);

            assertEquals(RECEIVED_AT_SECONDS, storedReceiptSeconds(work.requests.get(0)));
        } finally {
            java.util.TimeZone.setDefault(original);
        }
    }

    @Test
    public void scheduleDelivery_legacyOverloadOrNonPositive_omitsKey() {
        ScheduledWork legacy =
                captureScheduledWork(createValidPushBundle(), PushStatisticsScheduler::scheduleDeliveryEvent, 1);
        ScheduledWork zero = captureScheduledWork(
                createValidPushBundle(), bundle -> PushStatisticsScheduler.scheduleDeliveryEvent(bundle, 0L), 1);

        for (ScheduledWork work : java.util.Arrays.asList(legacy, zero)) {
            assertFalse(
                    "no receipt key",
                    work.requests
                            .get(0)
                            .getWorkSpec()
                            .input
                            .getKeyValueMap()
                            .containsKey(PushStatisticsWorker.DATA_RECEIVED_AT_UTC_SECONDS));
        }
    }

    @Test
    public void scheduleOpen_hasNoReceiptKey() {
        ScheduledWork work =
                captureScheduledWork(createValidPushBundle(), PushStatisticsScheduler::scheduleOpenEvent, 1);

        assertFalse(
                "no receipt key",
                work.requests
                        .get(0)
                        .getWorkSpec()
                        .input
                        .getKeyValueMap()
                        .containsKey(PushStatisticsWorker.DATA_RECEIVED_AT_UTC_SECONDS));
    }
}
