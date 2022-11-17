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

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.pushwoosh.GDPRManager;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.inapp.PushwooshInApp;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.tags.TagsBundle;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;

/**
 * Created by aevstefeev on 27/03/2018.
 */

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GDPRTest extends BaseTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        GDPRClear();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        GDPRClear();
    }

    private void GDPRClear() throws Exception {
        clearPrefs("com.pushwoosh.registration");
        clearPrefs("MyPrefs");

        CountDownLatch doneSignal = new CountDownLatch(2);
        TagsBundle tagsBundle = new TagsBundle.Builder()
                .putBoolean("channel", true)
                .build();

        PushwooshInApp.getInstance().postEvent("GDPRConsent", tagsBundle,
                result -> doneSignal.countDown());
        PushwooshInApp.getInstance().postEvent("");

        TagsBundle tagsBundle2 = new TagsBundle.Builder()
                .putBoolean("status", false)
                .build();

        PushwooshInApp.getInstance().postEvent("GDPRDelete", tagsBundle2,
                result -> doneSignal.countDown());
       // wait(5);
        doneSignal.await();
    }

    private void clearPrefs(String name) {
        Context applicationContext = AndroidPlatformModule.getApplicationContext();
        SharedPreferences prefs =
                applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.commit();
    }

    @Test(timeout = TIME_OUT)
    public void communicationDisable() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        GDPRManager.getInstance().setCommunicationEnabled(false, result -> {
            Assert.assertTrue(result.isSuccess());
            Assert.assertEquals(false, GDPRManager.getInstance().isCommunicationEnabled());
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }

    @Test(timeout = TIME_OUT)
    public void communicationDisableNotAvailable() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        GDPRManager.getInstance().setCommunicationEnabled(false, result -> {
            if (!result.isSuccess()) {
                Assert.assertEquals("The GDPR solution isn’t available for this account", result.getException().getMessage());
            }
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }

    @Test(timeout = TIME_OUT)
    public void communicationEnable() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        GDPRManager.getInstance().setCommunicationEnabled(false, result -> {
            Assert.assertTrue(result.isSuccess());
            countDownLatch.countDown();
        });
        countDownLatch.await();
        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        GDPRManager.getInstance().setCommunicationEnabled(true, result -> {
            Assert.assertTrue(result.isSuccess());
            countDownLatch2.countDown();
        });
        countDownLatch2.await();
        checkRegister();
    }

    private void checkRegister() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Pushwoosh.getInstance().registerForPushNotifications(result -> {
            if (!result.isSuccess()) {
                Assert.assertEquals("", result.getException().getMessage());
            }
            Assert.assertTrue(result.isSuccess());
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }

 /*   @Test(timeout = TIME_OUT)
    public void removeAllDeviceData() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        GDPRManager.getInstance().removeAllDeviceData(result -> {
            if (!result.isSuccess()) {
                Assert.assertEquals("", result.getException().getMessage());
            }
            countDownLatch.countDown();
        });
        countDownLatch.await();
        checkRegister();
    }*/

}
