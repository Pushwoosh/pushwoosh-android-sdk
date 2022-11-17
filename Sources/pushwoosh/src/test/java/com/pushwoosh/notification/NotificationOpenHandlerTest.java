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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.pushwoosh.internal.chain.Chain;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.notification.handlers.notification.PushNotificationOpenHandler;
import com.pushwoosh.testutil.PlatformTestManager;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.util.Iterator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class NotificationOpenHandlerTest {

    private NotificationOpenHandler notificationOpenHandler;

    Chain<PushNotificationOpenHandler> notificationOpenHandlerChainMock;
    private PlatformTestManager platformTestManager;

    private PushNotificationOpenHandler pushNotificationOpenHandler1;
    private PushNotificationOpenHandler pushNotificationOpenHandler2;


    @Before
    public void setUp() throws Exception {
        platformTestManager = new PlatformTestManager();
        platformTestManager.setUp();

        notificationOpenHandlerChainMock = Mockito.mock(Chain.class);
        Iterator<PushNotificationOpenHandler> iterator = createIterator();
        when(notificationOpenHandlerChainMock.getIterator()).thenReturn(iterator);
        notificationOpenHandler = new NotificationOpenHandler(notificationOpenHandlerChainMock);
    }

    @NonNull
    private Iterator<PushNotificationOpenHandler> createIterator() {
        pushNotificationOpenHandler1 = mock(PushNotificationOpenHandler.class);
        pushNotificationOpenHandler2 = mock(PushNotificationOpenHandler.class);
        Iterator<PushNotificationOpenHandler> iterator = Mockito.mock(Iterator.class);
        when(iterator.hasNext()).thenReturn(true, true, false);
        when(iterator.next()).thenReturn(pushNotificationOpenHandler1, pushNotificationOpenHandler2);
        return iterator;
    }

    @After
    public void tearDown() throws Exception {
        platformTestManager.tearDown();
    }

    @Test
    @Ignore
    public void preHandleNotification() {
        Bundle bundle = createTestBundle();
        boolean result = notificationOpenHandler.preHandleNotification(bundle);
        Assert.assertTrue(result);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(AndroidPlatformModule.getApplicationContext()).startActivity(intentArgumentCaptor.capture());
        Assert.assertEquals(1, intentArgumentCaptor.getAllValues().size());

        Intent intent = intentArgumentCaptor.getValue();
        Assert.assertEquals("android.intent.action.VIEW", intent.getAction());
        Assert.assertEquals("http:\\\\link", intent.getData().toString());
    }

    private Bundle createTestBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("one", "1");
        bundle.putBoolean("two", true);
        bundle.putInt("three", 3);
        bundle.putString("l", "http:\\\\link");
        return bundle;
    }

    @Test
    public void startPushLauncherActivity() {
        PushMessage pushMessage = new PushMessage(createTestBundle());
        notificationOpenHandler.startPushLauncherActivity(pushMessage);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(AndroidPlatformModule.getApplicationContext()).startActivity(intentArgumentCaptor.capture());
        Assert.assertEquals(1, intentArgumentCaptor.getAllValues().size());
        Intent intent = intentArgumentCaptor.getValue();
        Assert.assertEquals("com.pushwoosh.MESSAGE", intent.getAction());
        Assert.assertEquals("Bundle[{PUSH_RECEIVE_EVENT={\"one\":\"1\",\"l\":\"http:\\\\\\\\link\",\"two\":true,\"three\":3}}]",
                intent.getExtras().toString());
    }

    @Test
    @Ignore("java.lang.NoSuchMethodError")
    public void startPushLauncherActivitySecondMethod() {
        PushMessage pushMessage = new PushMessage(createTestBundle());
        Context applicationContext = AndroidPlatformModule.getApplicationContext();

        Mockito.doThrow(new ActivityNotFoundException("Test Exception")).when(applicationContext).startActivity(any());
        notificationOpenHandler.startPushLauncherActivity(pushMessage);

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(applicationContext).startActivity(intentArgumentCaptor.capture());
        Assert.assertEquals(1, intentArgumentCaptor.getAllValues().size());
        Intent intent = intentArgumentCaptor.getValue();
        Assert.assertEquals("com.pushwoosh.MESSAGE", intent.getAction());
        String expected = "Bundle[{PUSH_RECEIVE_EVENT={\"one\":\"1\",\"l\":\"http:\\\\\\\\link\",\"two\":true,\"three\":3}}]";
        Assert.assertEquals(expected, intent.getExtras().toString());
    }

    @Test
    public void postHandleNotification() {
        Bundle bundle = createTestBundle();
        notificationOpenHandler.postHandleNotification(bundle);

        Mockito.verify(pushNotificationOpenHandler1).postHandleNotification(eq(bundle));
        Mockito.verify(pushNotificationOpenHandler2).postHandleNotification(eq(bundle));
    }
}