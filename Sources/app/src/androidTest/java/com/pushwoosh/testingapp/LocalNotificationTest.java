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

package com.pushwoosh.testingapp;

import android.service.notification.StatusBarNotification;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

/**
 * Created by aevstefeev on 23/03/2018.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class LocalNotificationTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        PushwooshProxyController.getPushwooshProxy().clearNotificationCenter();
    }

    @After
    public void tearDown() throws Exception {
        super.setUp();
        PushwooshProxyController.getPushwooshProxy().clearNotificationCenter();
    }

    @Test(timeout = TIME_OUT)
    public void sendNotificationTest() throws Exception {
        PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message", 1);
        TimeUnit.SECONDS.sleep(TIME_OUT_WAIT_NOTIFICATION);
        StatusBarNotification[] notifications = getNotificationList();
        Assert.assertEquals(1, notifications.length);

        StatusBarNotification notification = notifications[0];
        Assert.assertEquals("message", notification.getNotification().tickerText);
    }

    @Test(timeout = TIME_OUT)
    public void sendNotificationTestTwo() throws Exception {
        TimeUnit.SECONDS.sleep(1);
        PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message1", 1);
        PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message2", 10);
        TimeUnit.SECONDS.sleep(TIME_OUT_WAIT_NOTIFICATION);
        assertEqualsTextMessage("message1");
        TimeUnit.SECONDS.sleep(TIME_OUT_WAIT_NOTIFICATION);
        assertEqualsTextMessage("message2");
    }

    @Test(timeout = TIME_OUT)
    public void cancelNotification() throws Exception {
        int id = PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message", 1);
        PushwooshProxyController.getPushwooshProxy().clearLocalNotification(id);
        TimeUnit.SECONDS.sleep(TIME_OUT_WAIT_NOTIFICATION);
        StatusBarNotification[] notifications = getNotificationList();
        Assert.assertEquals(0, notifications.length);
    }


    @Test(timeout = TIME_OUT)
    public void cancelNotificationOneOfTwo() throws Exception {
        int id = PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message1", 1);
        PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message2", 1);
        PushwooshProxyController.getPushwooshProxy().clearLocalNotification(id);
        TimeUnit.SECONDS.sleep(TIME_OUT_WAIT_NOTIFICATION);
        StatusBarNotification[] notifications = getNotificationList();
        Assert.assertEquals(1,notifications.length);
        assertEqualsTextMessage("message2");
    }

    @Test(timeout = TIME_OUT)
    public void cancelAllNotification() throws Exception {
        PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message1", 1);
        PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message2", 2);
        PushwooshProxyController.getPushwooshProxy().clearLocalNotifications();
        TimeUnit.SECONDS.sleep(TIME_OUT_WAIT_NOTIFICATION);

        StatusBarNotification[] notifications = getNotificationList();
        Assert.assertEquals(0, notifications.length);
    }

    @Test(timeout = TIME_OUT)
    public void cleanToolbar() throws Exception {
        PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message1", 1);
        PushwooshProxyController.getPushwooshProxy().scheduleLocalNotification("message2", 2);
        TimeUnit.SECONDS.sleep(TIME_OUT_WAIT_NOTIFICATION);
        StatusBarNotification[] notifications = getNotificationList();
        Assert.assertEquals(1, notifications.length );
        assertEqualsTextMessage("message2");
        PushwooshProxyController.getPushwooshProxy().clearNotificationCenter();
        notifications = getNotificationList();
        Assert.assertEquals(0, notifications.length);
    }
}