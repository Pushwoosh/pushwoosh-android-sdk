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

package com.pushwoosh.notification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Bundle;

import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.TimeProvider;
import com.pushwoosh.repository.DbLocalNotification;
import com.pushwoosh.repository.DbLocalNotificationHelper;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.RepositoryModule;

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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.LooperMode;
import org.robolectric.shadows.ShadowLog;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.LEGACY)
@org.robolectric.annotation.Config(manifest = org.robolectric.annotation.Config.NONE)
public class RescheduleNotificationsWorkerTest {

    private RescheduleNotificationsWorker worker;
    private Context context;
    private AutoCloseable mocks;

    @Mock
    private WorkerParameters workerParametersMock;

    @Mock
    private LocalNotificationStorage storageMock;

    @Mock
    private TimeProvider timeProviderMock;

    @Mock
    private AndroidPlatformModule androidPlatformModuleMock;

    @Before
    public void setUp() {
        ShadowLog.stream = System.out;
        mocks = MockitoAnnotations.openMocks(this);

        context = spy(RuntimeEnvironment.application);
        when(context.getApplicationContext()).thenReturn(context);

        worker = new RescheduleNotificationsWorker(context, workerParametersMock);
    }

    @After
    public void tearDown() throws Exception {
        mocks.close();
    }

    private static DbLocalNotification dbNotification(int requestId) {
        return new DbLocalNotification(requestId, 0, "", 0L, new Bundle());
    }

    // Verifies that doWork reschedules every persisted local notification with the TimeProvider time and returns
    // success.
    @Test
    public void testWorkerReschedulesEveryStoredNotificationAndReturnsSuccess() {
        DbLocalNotification dbNotif1 = dbNotification(1);
        DbLocalNotification dbNotif2 = dbNotification(2);

        doAnswer(invocation -> {
                    DbLocalNotificationHelper.EnumeratorLocalNotification enumerator = invocation.getArgument(0);
                    enumerator.enumerate(dbNotif1);
                    enumerator.enumerate(dbNotif2);
                    return null;
                })
                .when(storageMock)
                .enumerateDbLocalNotificationList(any());

        when(timeProviderMock.getCurrentTime()).thenReturn(12345L);
        when(androidPlatformModuleMock.getTimeProvider()).thenReturn(timeProviderMock);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<LocalNotificationReceiver> receiverMock =
                        Mockito.mockStatic(LocalNotificationReceiver.class)) {
            repoMock.when(RepositoryModule::getLocalNotificationStorage).thenReturn(storageMock);
            platformMock.when(AndroidPlatformModule::getInstance).thenReturn(androidPlatformModuleMock);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            receiverMock.verify(
                    () -> LocalNotificationReceiver.rescheduleNotification(eq(dbNotif1), eq(12345L)), times(1));
            receiverMock.verify(
                    () -> LocalNotificationReceiver.rescheduleNotification(eq(dbNotif2), eq(12345L)), times(1));
        }
    }

    // Verifies that doWork returns failure and skips any reschedule call when storage is null.
    @Test
    public void testWorkerReturnsFailureWhenStorageIsNull() {
        when(androidPlatformModuleMock.getTimeProvider()).thenReturn(timeProviderMock);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<LocalNotificationReceiver> receiverMock =
                        Mockito.mockStatic(LocalNotificationReceiver.class)) {
            repoMock.when(RepositoryModule::getLocalNotificationStorage).thenReturn(null);
            platformMock.when(AndroidPlatformModule::getInstance).thenReturn(androidPlatformModuleMock);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.failure(), result);
            receiverMock.verify(() -> LocalNotificationReceiver.rescheduleNotification(any(), anyLong()), never());
        }
    }

    // Verifies that doWork falls back to System.currentTimeMillis when TimeProvider is null and still reschedules
    // entries.
    @Test
    public void testWorkerFallsBackToSystemTimeWhenTimeProviderIsNull() {
        DbLocalNotification dbNotif = dbNotification(7);

        doAnswer(invocation -> {
                    DbLocalNotificationHelper.EnumeratorLocalNotification enumerator = invocation.getArgument(0);
                    enumerator.enumerate(dbNotif);
                    return null;
                })
                .when(storageMock)
                .enumerateDbLocalNotificationList(any());

        when(androidPlatformModuleMock.getTimeProvider()).thenReturn(null);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<LocalNotificationReceiver> receiverMock =
                        Mockito.mockStatic(LocalNotificationReceiver.class)) {
            repoMock.when(RepositoryModule::getLocalNotificationStorage).thenReturn(storageMock);
            platformMock.when(AndroidPlatformModule::getInstance).thenReturn(androidPlatformModuleMock);

            long before = System.currentTimeMillis();
            ListenableWorker.Result result = worker.doWork();
            long after = System.currentTimeMillis();

            assertEquals(ListenableWorker.Result.success(), result);

            ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
            receiverMock.verify(
                    () -> LocalNotificationReceiver.rescheduleNotification(eq(dbNotif), timeCaptor.capture()),
                    times(1));
            long captured = timeCaptor.getValue();
            assertTrue(
                    "currentTime " + captured + " should fall inside [" + before + ", " + after + "]",
                    captured >= before && captured <= after);
        }
    }

    // Verifies that doWork returns success and never invokes the reschedule receiver when storage enumerates zero
    // entries.
    @Test
    public void testWorkerReturnsSuccessAndSkipsRescheduleWhenStorageIsEmpty() {
        when(androidPlatformModuleMock.getTimeProvider()).thenReturn(timeProviderMock);
        when(timeProviderMock.getCurrentTime()).thenReturn(999L);

        try (MockedStatic<RepositoryModule> repoMock = Mockito.mockStatic(RepositoryModule.class);
                MockedStatic<AndroidPlatformModule> platformMock = Mockito.mockStatic(AndroidPlatformModule.class);
                MockedStatic<LocalNotificationReceiver> receiverMock =
                        Mockito.mockStatic(LocalNotificationReceiver.class)) {
            repoMock.when(RepositoryModule::getLocalNotificationStorage).thenReturn(storageMock);
            platformMock.when(AndroidPlatformModule::getInstance).thenReturn(androidPlatformModuleMock);

            ListenableWorker.Result result = worker.doWork();

            assertEquals(ListenableWorker.Result.success(), result);
            receiverMock.verify(() -> LocalNotificationReceiver.rescheduleNotification(any(), anyLong()), never());
        }
    }
}
