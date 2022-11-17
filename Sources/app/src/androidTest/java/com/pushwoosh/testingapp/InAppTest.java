package com.pushwoosh.testingapp;

import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.webkit.JavascriptInterface;

import com.pushwoosh.inapp.PushwooshInApp;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static com.pushwoosh.testingapp.BaseTest.TIME_OUT;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class InAppTest  {

    public static final String TEST_USER_ID = "test_user_id";
    public static final String TEST_USER_ID_2 = "test_user_id2";

    @After
    public void tearDown(){
        CustomBridgeStatic.customFunctionWasCall = false;
        PushwooshInApp.getInstance().removeJavascriptInterface("customBridge");
    }

    @Test(timeout = TIME_OUT)
    public void postEventAndClose() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        PushwooshInApp.getInstance().postEvent("test", null, result -> {
            assert false;
            if (result.isSuccess()) {
                countDownLatch.countDown();
            } else {
                assert false;
            }
        });
        countDownLatch.await(5, TimeUnit.SECONDS);
        TimeUnit.SECONDS.sleep(3);
        onWebView()
                .withElement(findElement(Locator.CLASS_NAME, "white-button"))
                .perform(webClick());
    }

    @Test(timeout = TIME_OUT)
    public void setUserId() {
        PushwooshInApp.getInstance().setUserId(TEST_USER_ID);
        String userId = PushwooshInApp.getInstance().getUserId();
        Assert.assertEquals(TEST_USER_ID, userId);
    }

    @Test(timeout = TIME_OUT)
    public void mergeUserId() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        PushwooshInApp.getInstance().mergeUserId(TEST_USER_ID, TEST_USER_ID_2, true, result -> {
            if (result.isSuccess()) {
                countDownLatch.countDown();
            } else {
                assert false;
            }
        });
        countDownLatch.await();
    }

    @Test(timeout = TIME_OUT)
    public void addJavascriptInterface() throws Exception {
        CustomBridge customBridge = new CustomBridge();
        PushwooshInApp.getInstance().addJavascriptInterface(customBridge, "customBridge");

        openInAppWithCustomBridge();
        TimeUnit.SECONDS.sleep(3);
        onWebView()
                .withElement(findElement(Locator.CLASS_NAME, "white-button"))
                .perform(webClick());

        Assert.assertTrue(CustomBridgeStatic.customFunctionWasCall);

    }

    @Test(timeout = TIME_OUT)
    public void registerJavascriptInterface() throws Exception {
        String name = CustomBridgeStatic.class.getName();
        PushwooshInApp.getInstance().registerJavascriptInterface(name, "customBridge");

        openInAppWithCustomBridge();
        TimeUnit.SECONDS.sleep(3);
        onWebView()
                .withElement(findElement(Locator.CLASS_NAME, "white-button"))
                .perform(webClick());

        Assert.assertTrue(CustomBridgeStatic.customFunctionWasCall);
    }

    private void openInAppWithCustomBridge() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        PushwooshInApp.getInstance().postEvent("custom", null, result -> {
            if (result.isSuccess()) {
                countDownLatch.countDown();
            } else {
                assert false;
            }
        });
        countDownLatch.await(5, TimeUnit.SECONDS);
    }

    public class CustomBridge {
        private boolean customFunctionWasCall = false;

        @JavascriptInterface
        public void customFunction() {
            this.customFunctionWasCall = true;
        }
    }

    public static class CustomBridgeStatic {
        private static boolean customFunctionWasCall = false;

        @JavascriptInterface
        public void customFunction() {
            customFunctionWasCall = true;
        }
    }
}
