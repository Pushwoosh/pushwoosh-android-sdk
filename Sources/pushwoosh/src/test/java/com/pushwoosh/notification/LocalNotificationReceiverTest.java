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

package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.manager.ManagerProvider;
import com.pushwoosh.internal.utils.BackgroundExecutor;
import com.pushwoosh.repository.DbLocalNotification;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by aevstefeev on 13/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@LooperMode(LooperMode.Mode.LEGACY)
public class LocalNotificationReceiverTest {

    private LocalNotificationReceiver localNotificationReceiver;
    private LocalNotificationStorage localNotificationStorageMock;

    @Mock
    private NotificationServiceExtension notificationServiceExtensionMock;

    private PlatformTestManager platformTestManager;
    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        WhiteboxHelper.setInternalState(
                PushwooshPlatform.getInstance(), "notificationServiceExtension", notificationServiceExtensionMock);
        localNotificationReceiver = new LocalNotificationReceiver();

        localNotificationStorageMock = mock(LocalNotificationStorage.class);
        RepositoryModule.setLocalNotificationStorage(localNotificationStorageMock);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
        mocks.close();
    }

    @Test
    public void scheduleNotification() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("anyKey", "any");
        Mockito.when(localNotificationStorageMock.nextRequestId()).thenReturn(1234);

        int result = LocalNotificationReceiver.scheduleNotification(bundle, 10);

        verify(localNotificationStorageMock)
                .saveLocalNotification(eq(1234), Mockito.any(Bundle.class), Mockito.anyLong());
        assertEquals(1234, result);
    }

    @Test
    public void cancelAll() throws Exception {
        Set<Integer> requestIds = new HashSet<>();
        requestIds.add(1);
        requestIds.add(2);
        requestIds.add(3);

        Mockito.when(localNotificationStorageMock.getRequestIds()).thenReturn(requestIds);
        LocalNotificationReceiver.cancelAll();
        verify(localNotificationStorageMock, times(3)).removeLocalNotification(Mockito.anyInt());
    }

    @Test
    public void cancelNotification() throws Exception {
        LocalNotificationReceiver.cancelNotification(123);
        verify(localNotificationStorageMock).removeLocalNotification(123);
    }

    @Test
    public void onReceive_validIntent_dispatchesToNotificationService() {
        Bundle extras = new Bundle();
        extras.putString(LocalNotificationReceiver.EXTRA_NOTIFICATION_ID, "42");
        extras.putString("data", "x");
        Intent intent = new Intent();
        intent.putExtras(extras);

        try (MockedStatic<BackgroundExecutor> bg =
                Mockito.mockStatic(BackgroundExecutor.class, Mockito.CALLS_REAL_METHODS)) {
            bg.when(() -> BackgroundExecutor.executeOnPool(any())).thenAnswer(invocation -> {
                ((Runnable) invocation.getArgument(0)).run();
                return null;
            });

            localNotificationReceiver.onReceive(AndroidPlatformModule.getApplicationContext(), intent);
        }

        verify(localNotificationStorageMock).removeLocalNotification(42);
        ArgumentCaptor<Bundle> bundleCaptor = ArgumentCaptor.forClass(Bundle.class);
        verify(notificationServiceExtensionMock).handleMessage(bundleCaptor.capture());
        assertEquals("42", bundleCaptor.getValue().getString(LocalNotificationReceiver.EXTRA_NOTIFICATION_ID));
        assertEquals("x", bundleCaptor.getValue().getString("data"));
    }

    @Test
    public void onReceive_nullIntent_doesNothing() {
        localNotificationReceiver.onReceive(AndroidPlatformModule.getApplicationContext(), null);

        verifyNoInteractions(localNotificationStorageMock);
        verifyNoInteractions(notificationServiceExtensionMock);
    }

    @Test
    public void onReceive_intentWithoutExtras_doesNothing() {
        Intent intent = new Intent();

        localNotificationReceiver.onReceive(AndroidPlatformModule.getApplicationContext(), intent);

        verifyNoInteractions(localNotificationStorageMock);
        verifyNoInteractions(notificationServiceExtensionMock);
    }

    @Test
    public void onReceive_extrasWithoutLocalPushId_skipsRemovalAndDispatch() {
        Bundle extras = new Bundle();
        extras.putString("unrelated", "value");
        Intent intent = new Intent();
        intent.putExtras(extras);

        localNotificationReceiver.onReceive(AndroidPlatformModule.getApplicationContext(), intent);

        verify(localNotificationStorageMock, never()).removeLocalNotification(anyInt());
        verifyNoInteractions(notificationServiceExtensionMock);
    }

    @Test
    public void scheduleNotification_alarmManagerThrowsSecurity_returnsMinusOneButStillPersists() {
        Bundle bundle = new Bundle();
        bundle.putString("anyKey", "any");
        when(localNotificationStorageMock.nextRequestId()).thenReturn(55);

        AlarmManager alarmManagerMock = mock(AlarmManager.class);
        Mockito.doThrow(new SecurityException("quota"))
                .when(alarmManagerMock)
                .set(anyInt(), anyLong(), any(PendingIntent.class));
        ManagerProvider managerProviderMock = mock(ManagerProvider.class);
        when(managerProviderMock.getAlarmManager()).thenReturn(alarmManagerMock);

        int result;
        try (MockedStatic<AndroidPlatformModule> platformMock =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProviderMock);

            result = LocalNotificationReceiver.scheduleNotification(bundle, 10);
        }

        verify(localNotificationStorageMock).saveLocalNotification(eq(55), any(Bundle.class), anyLong());
        assertEquals(-1, result);
    }

    @Test
    public void scheduleNotification_alarmManagerNull_returnsMinusOneButStillPersists() {
        Bundle bundle = new Bundle();
        bundle.putString("anyKey", "any");
        when(localNotificationStorageMock.nextRequestId()).thenReturn(66);

        ManagerProvider managerProviderMock = mock(ManagerProvider.class);
        when(managerProviderMock.getAlarmManager()).thenReturn(null);

        int result;
        try (MockedStatic<AndroidPlatformModule> platformMock =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProviderMock);

            result = LocalNotificationReceiver.scheduleNotification(bundle, 10);
        }

        verify(localNotificationStorageMock).saveLocalNotification(eq(66), any(Bundle.class), anyLong());
        assertEquals(-1, result);
    }

    @Test
    public void scheduleNotification_secondsZero_triggerAtMillisIsNow() {
        Bundle bundle = new Bundle();
        bundle.putString("anyKey", "any");
        when(localNotificationStorageMock.nextRequestId()).thenReturn(77);

        long before = System.currentTimeMillis();
        int result = LocalNotificationReceiver.scheduleNotification(bundle, 0);
        long after = System.currentTimeMillis();

        assertEquals(77, result);
        ArgumentCaptor<Long> triggerCaptor = ArgumentCaptor.forClass(Long.class);
        verify(localNotificationStorageMock).saveLocalNotification(eq(77), any(Bundle.class), triggerCaptor.capture());
        long captured = triggerCaptor.getValue();
        // Generous upper bound: tolerate GC pauses / loaded CI runners while still catching mutations
        // like +seconds*1000 or +large constants (which would push captured well past now+1s).
        long upperBound = after + 1000;
        assertTrue("triggerAtMillis " + captured + " should be >= " + before, captured >= before);
        assertTrue("triggerAtMillis " + captured + " should be <= " + upperBound, captured <= upperBound);
    }

    @Test
    public void rescheduleNotification_freshNotification_schedulesAlarmAndKeepsEntry() {
        long currentTime = System.currentTimeMillis();
        Bundle bundle = new Bundle();
        bundle.putString("k", "v");
        DbLocalNotification dbLocalNotification = new DbLocalNotification(5, currentTime + 60_000L, bundle);

        AlarmManager alarmManagerMock = mock(AlarmManager.class);
        ManagerProvider managerProviderMock = mock(ManagerProvider.class);
        when(managerProviderMock.getAlarmManager()).thenReturn(alarmManagerMock);

        try (MockedStatic<AndroidPlatformModule> platformMock =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProviderMock);

            LocalNotificationReceiver.rescheduleNotification(dbLocalNotification, currentTime);
        }

        verify(alarmManagerMock).set(eq(AlarmManager.RTC_WAKEUP), anyLong(), any(PendingIntent.class));
        verify(localNotificationStorageMock, never()).removeLocalNotification(anyInt());
    }

    @Test
    public void rescheduleNotification_oldNotification_removesFromStorage() {
        long currentTime = System.currentTimeMillis();
        Bundle bundle = new Bundle();
        bundle.putString("k", "v");
        long oldTrigger = currentTime - LocalNotificationReceiver.WEEK - 1L;
        DbLocalNotification dbLocalNotification = new DbLocalNotification(9, oldTrigger, bundle);

        AlarmManager alarmManagerMock = mock(AlarmManager.class);
        ManagerProvider managerProviderMock = mock(ManagerProvider.class);
        when(managerProviderMock.getAlarmManager()).thenReturn(alarmManagerMock);

        try (MockedStatic<AndroidPlatformModule> platformMock =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProviderMock);

            LocalNotificationReceiver.rescheduleNotification(dbLocalNotification, currentTime);
        }

        verify(localNotificationStorageMock).removeLocalNotification(9);
        verify(alarmManagerMock, never()).set(anyInt(), anyLong(), any(PendingIntent.class));
    }

    @Test
    public void rescheduleNotification_pastTriggerWithinWeek_clampsDelayTo5Seconds() {
        long currentTime = System.currentTimeMillis();
        Bundle bundle = new Bundle();
        bundle.putString("k", "v");
        DbLocalNotification dbLocalNotification = new DbLocalNotification(11, currentTime - 2_000L, bundle);

        AlarmManager alarmManagerMock = mock(AlarmManager.class);
        ManagerProvider managerProviderMock = mock(ManagerProvider.class);
        when(managerProviderMock.getAlarmManager()).thenReturn(alarmManagerMock);

        ArgumentCaptor<Long> triggerCaptor = ArgumentCaptor.forClass(Long.class);
        try (MockedStatic<AndroidPlatformModule> platformMock =
                Mockito.mockStatic(AndroidPlatformModule.class, Mockito.CALLS_REAL_METHODS)) {
            platformMock.when(AndroidPlatformModule::getManagerProvider).thenReturn(managerProviderMock);

            LocalNotificationReceiver.rescheduleNotification(dbLocalNotification, currentTime);
        }

        verify(alarmManagerMock).set(eq(AlarmManager.RTC_WAKEUP), triggerCaptor.capture(), any(PendingIntent.class));
        long captured = triggerCaptor.getValue();
        long expected = currentTime + 5000L;
        assertEquals("triggerAtMillis", expected, captured);
        verify(localNotificationStorageMock, never()).removeLocalNotification(anyInt());
    }

    @Test
    public void cancelAll_singleRemoveThrows_continuesRemainingCancellations() {
        Set<Integer> requestIds = new LinkedHashSet<>();
        requestIds.add(1);
        requestIds.add(2);
        requestIds.add(3);
        when(localNotificationStorageMock.getRequestIds()).thenReturn(requestIds);
        Mockito.doNothing().when(localNotificationStorageMock).removeLocalNotification(1);
        Mockito.doThrow(new RuntimeException("boom"))
                .when(localNotificationStorageMock)
                .removeLocalNotification(2);
        Mockito.doNothing().when(localNotificationStorageMock).removeLocalNotification(3);

        LocalNotificationReceiver.cancelAll();

        verify(localNotificationStorageMock, times(3)).removeLocalNotification(anyInt());

        // PrefsHelper tearDown iterates getRequestIds() again — reset stubs so it sees an empty set.
        Mockito.reset(localNotificationStorageMock);
        when(localNotificationStorageMock.getRequestIds()).thenReturn(new HashSet<>());
    }
}
