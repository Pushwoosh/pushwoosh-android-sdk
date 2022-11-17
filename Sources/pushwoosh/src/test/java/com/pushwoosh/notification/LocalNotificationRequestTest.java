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

import android.app.NotificationManager;
import android.content.Context;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.repository.DbLocalNotification;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class LocalNotificationRequestTest {
    public static final int TEST_REQUEST_ID = 99;
    public static final String TAG = "tag";
    public static final int NOTIFICATION_ID = 2;
    private PlatformTestManager platformTestManager;
    private LocalNotificationStorage localNotificationStorageMock;

    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();
        localNotificationStorageMock = Mockito.mock(LocalNotificationStorage.class);
        RepositoryModule.setLocalNotificationStorage(localNotificationStorageMock);
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    public void cancel() {
        LocalNotificationRequest localNotificationRequest = new LocalNotificationRequest(TEST_REQUEST_ID);
        DbLocalNotification dbLocalNotification = new DbLocalNotification(TEST_REQUEST_ID, NOTIFICATION_ID, TAG);
        Mockito.when(localNotificationStorageMock.getLocalNotificationShown(TEST_REQUEST_ID)).thenReturn(dbLocalNotification);
        Context spyContext = AndroidPlatformModule.getApplicationContext();
        NotificationManager systemService = (NotificationManager) spyContext.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationManager spyService = spy(systemService);
        when(spyContext.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(spyService);

        localNotificationRequest.cancel();

        verify(spyService).cancel(TAG, NOTIFICATION_ID);
    }
}