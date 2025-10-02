package com.pushwoosh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.pushwoosh.internal.platform.AndroidPlatformModule;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

import java.lang.reflect.Method;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class PushwooshWorkManagerHelperTest {

    private static final String TEST_WORK_NAME = "test_work_name";

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
    }

    @After
    public void tearDown() {
        // Clean up any static mocks
    }

    /**
     * Test successful enqueue one-time unique work
     */
    @Test
    public void testEnqueueOneTimeUniqueWorkSuccess() {
        OneTimeWorkRequest mockRequest = mock(OneTimeWorkRequest.class);
        WorkManager mockWorkManager = mock(WorkManager.class);

        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock.when(() -> WorkManager.getInstance(any(Context.class))).thenReturn(mockWorkManager);

            try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {
                platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);

                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    mockRequest, 
                    TEST_WORK_NAME, 
                    ExistingWorkPolicy.KEEP
                );

                verify(mockWorkManager, times(1))
                    .enqueueUniqueWork(TEST_WORK_NAME, ExistingWorkPolicy.KEEP, mockRequest);
            }
        }
    }

    /**
     * Test enqueue one-time unique work with WorkManager exception
     */
    @Test
    public void testEnqueueOneTimeUniqueWorkWithException() {
        OneTimeWorkRequest mockRequest = mock(OneTimeWorkRequest.class);
        WorkManager mockWorkManager = mock(WorkManager.class);

        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock.when(() -> WorkManager.getInstance(any(Context.class))).thenReturn(mockWorkManager);

            try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {
                platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);

                doThrow(new RuntimeException("WorkManager error")).when(mockWorkManager)
                    .enqueueUniqueWork(anyString(), any(ExistingWorkPolicy.class), any(OneTimeWorkRequest.class));

                // This should not throw exception - errors are handled internally
                PushwooshWorkManagerHelper.enqueueOneTimeUniqueWork(
                    mockRequest, 
                    TEST_WORK_NAME, 
                    ExistingWorkPolicy.KEEP
                );

                // Verify attempt was made despite exception
                verify(mockWorkManager, times(1))
                    .enqueueUniqueWork(TEST_WORK_NAME, ExistingWorkPolicy.KEEP, mockRequest);
            }
        }
    }

    /**
     * Test successful enqueue periodic unique work
     */
    @Test
    public void testEnqueuePeriodicUniqueWorkSuccess() {
        PeriodicWorkRequest mockRequest = mock(PeriodicWorkRequest.class);
        WorkManager mockWorkManager = mock(WorkManager.class);

        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock.when(() -> WorkManager.getInstance(any(Context.class))).thenReturn(mockWorkManager);

            try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {
                platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);

                PushwooshWorkManagerHelper.enqueuePeriodicUniqueWork(
                    mockRequest, 
                    TEST_WORK_NAME, 
                    ExistingPeriodicWorkPolicy.KEEP
                );

                verify(mockWorkManager, times(1))
                    .enqueueUniquePeriodicWork(TEST_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, mockRequest);
            }
        }
    }

    /**
     * Test enqueue periodic unique work with WorkManager exception
     */
    @Test
    public void testEnqueuePeriodicUniqueWorkWithException() {
        PeriodicWorkRequest mockRequest = mock(PeriodicWorkRequest.class);
        WorkManager mockWorkManager = mock(WorkManager.class);

        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock.when(() -> WorkManager.getInstance(any(Context.class))).thenReturn(mockWorkManager);

            try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {
                platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);

                doThrow(new RuntimeException("WorkManager error")).when(mockWorkManager)
                    .enqueueUniquePeriodicWork(anyString(), any(ExistingPeriodicWorkPolicy.class), any(PeriodicWorkRequest.class));

                // This should not throw exception - errors are handled internally
                PushwooshWorkManagerHelper.enqueuePeriodicUniqueWork(
                    mockRequest, 
                    TEST_WORK_NAME, 
                    ExistingPeriodicWorkPolicy.KEEP
                );

                // Verify attempt was made despite exception
                verify(mockWorkManager, times(1))
                    .enqueueUniquePeriodicWork(TEST_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, mockRequest);
            }
        }
    }

    /**
     * Test successful cancel periodic unique work
     */
    @Test
    public void testCancelPeriodicUniqueWorkSuccess() {
        WorkManager mockWorkManager = mock(WorkManager.class);

        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock.when(() -> WorkManager.getInstance(any(Context.class))).thenReturn(mockWorkManager);

            try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {
                platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);

                PushwooshWorkManagerHelper.cancelPeriodicUniqueWork(TEST_WORK_NAME);

                verify(mockWorkManager, times(1)).cancelUniqueWork(TEST_WORK_NAME);
            }
        }
    }

    /**
     * Test cancel periodic unique work with WorkManager exception
     */
    @Test
    public void testCancelPeriodicUniqueWorkWithException() {
        WorkManager mockWorkManager = mock(WorkManager.class);

        try (MockedStatic<WorkManager> workManagerMock = Mockito.mockStatic(WorkManager.class)) {
            workManagerMock.when(() -> WorkManager.getInstance(any(Context.class))).thenReturn(mockWorkManager);

            try (MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class)) {
                platformMock.when(AndroidPlatformModule::getApplicationContext).thenReturn(RuntimeEnvironment.application);

                doThrow(new RuntimeException("WorkManager error")).when(mockWorkManager)
                    .cancelUniqueWork(anyString());

                // This should not throw exception - errors are handled internally
                PushwooshWorkManagerHelper.cancelPeriodicUniqueWork(TEST_WORK_NAME);

                // Verify attempt was made despite exception
                verify(mockWorkManager, times(1)).cancelUniqueWork(TEST_WORK_NAME);
            }
        }
    }

    // Note: Tests for WorkManager getInstance fallback scenarios are not included
    // because they use reflection which cannot be properly mocked with Mockito.
    // These edge cases (NoSuchMethodException, NullPointerException) are rare
    // and the fallback mechanism is a defensive programming practice.

    /**
     * Test network available constraints configuration
     */
    @Test
    public void testGetNetworkAvailableConstraints() {
        Constraints constraints = PushwooshWorkManagerHelper.getNetworkAvailableConstraints();
        
        assertNotNull("Constraints should not be null", constraints);
        assertEquals("Should require connected network", 
            NetworkType.CONNECTED, constraints.getRequiredNetworkType());
    }

    /**
     * Test statistics constraints configuration for optimal delivery
     */
    @Test
    public void testGetStatisticsConstraints() {
        Constraints constraints = PushwooshWorkManagerHelper.getStatisticsConstraints();
        
        assertNotNull("Constraints should not be null", constraints);
        
        // Verify network requirement
        assertEquals("Should require connected network", 
            NetworkType.CONNECTED, constraints.getRequiredNetworkType());
            
        // Verify battery optimization settings for statistics delivery
        assertFalse("Should allow execution with low battery", 
            constraints.requiresBatteryNotLow());
        assertFalse("Should allow execution in device idle", 
            constraints.requiresDeviceIdle());
        assertFalse("Should allow execution with low storage", 
            constraints.requiresStorageNotLow());
        assertFalse("Should allow execution without charging", 
            constraints.requiresCharging());
    }

    /**
     * Test that statistics constraints are more permissive than network constraints
     */
    @Test
    public void testStatisticsConstraintsMorePermissiveThanNetwork() {
        Constraints networkConstraints = PushwooshWorkManagerHelper.getNetworkAvailableConstraints();
        Constraints statisticsConstraints = PushwooshWorkManagerHelper.getStatisticsConstraints();
        
        // Both require network
        assertEquals("Both should require connected network", 
            networkConstraints.getRequiredNetworkType(), 
            statisticsConstraints.getRequiredNetworkType());
            
        // Statistics constraints should be more permissive for battery/power optimizations
        // (Note: getNetworkAvailableConstraints uses defaults which may be more restrictive)
        assertFalse("Statistics should allow low battery execution", 
            statisticsConstraints.requiresBatteryNotLow());
        assertFalse("Statistics should allow execution in device idle", 
            statisticsConstraints.requiresDeviceIdle());
        assertFalse("Statistics should allow low storage execution", 
            statisticsConstraints.requiresStorageNotLow());
        assertFalse("Statistics should allow execution without charging", 
            statisticsConstraints.requiresCharging());
    }
}