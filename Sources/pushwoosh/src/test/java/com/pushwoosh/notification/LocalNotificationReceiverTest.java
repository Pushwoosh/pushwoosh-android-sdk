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

import android.os.Bundle;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.PlatformTestManager;
import com.pushwoosh.testutil.WhiteboxHelper;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by aevstefeev on 13/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class LocalNotificationReceiverTest {

    private LocalNotificationReceiver localNotificationReceiver;
    private LocalNotificationStorage localNotificationStorageMock;
    @Mock
    private NotificationServiceExtension notificationServiceExtensionMock;

    private PlatformTestManager platformTestManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        WhiteboxHelper.setInternalState(PushwooshPlatform.getInstance(), "notificationServiceExtension", notificationServiceExtensionMock);
        localNotificationReceiver = new LocalNotificationReceiver();


        localNotificationStorageMock = mock(LocalNotificationStorage.class);
        RepositoryModule.setLocalNotificationStorage(localNotificationStorageMock);
    }

    @After
    public void tearDown() {
        platformTestManager.tearDown();
    }

    @Test
    public void scheduleNotification() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("anyKey", "any");
        Mockito.when(localNotificationStorageMock.nextRequestId()).thenReturn(1234);

        int result = LocalNotificationReceiver.scheduleNotification(bundle, 10);

        verify(localNotificationStorageMock).saveLocalNotification(eq(1234), Mockito.any(Bundle.class), Mockito.anyLong());
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
        verify(localNotificationStorageMock, times(3))
                .removeLocalNotification(Mockito.anyInt());
    }

    @Test
    public void cancelNotification() throws Exception {
        LocalNotificationReceiver.cancelNotification(123);
        verify(localNotificationStorageMock).removeLocalNotification(123);
    }


}