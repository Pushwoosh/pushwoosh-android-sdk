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
import android.util.Log;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

/**
 * Created by aevstefeev on 23/03/2018.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class PushTest extends BaseTest {

    public static final int PUSH_TEST_TIME_OUT = TIME_OUT * 20;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        PushwooshProxyController.getPushwooshProxy().clearNotificationCenter();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Log.d("test", "try reg");
        Pushwoosh.getInstance().registerForPushNotifications(result ->{
            if(result.isSuccess()){
                Log.d("test", "reg success:"+result.getData());
            }else {
                Log.d("test", "reg error:"+result.getException());
            }

            countDownLatch.countDown();
        });
        countDownLatch.await();
    }

    @Test(timeout = PUSH_TEST_TIME_OUT)
    public void testPush() throws InterruptedException {
        sendPush("message", null);
        waitNotification();
        StatusBarNotification[] notificationList = getNotificationList();
        Assert.assertEquals(1, notificationList.length);
        assertEqualsTextMessage("message");
        assertSound("content://settings/system/notification_sound");
    }

    @Test(timeout = PUSH_TEST_TIME_OUT)
    public void checkCustomSoundAssets() throws InterruptedException {
        sendPush("message", "bubble.mp3");
        waitNotification();
        assertEqualsTextMessage("message");
        assertSound("content://com.pushwoosh.testingapp.provider/pw_external_files/bubble.mp3");

    }

    @Test(timeout = PUSH_TEST_TIME_OUT)
    public void checkCostomSoundResources() throws InterruptedException {
        sendPush("message", "push_sound");
        waitNotification();
        assertEqualsTextMessage("message");
        assertSound("android.resource://com.pushwoosh.testingapp/2131558400");
    }

    private void assertSound(String soundUri) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            Assert.assertEquals(soundUri, getNotificationList()[0].getNotification().sound.toString());
        } else {
            //todo
        }
    }
}
