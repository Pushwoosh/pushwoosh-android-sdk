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

import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;
import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import android.text.TextUtils;
import android.util.Log;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.function.Callback;
import com.pushwoosh.function.Result;
import com.pushwoosh.inbox.PushwooshInbox;
import com.pushwoosh.inbox.internal.data.InboxMessageInternal;
import com.pushwoosh.inbox.storage.db.DbInboxStorage;
import com.pushwoosh.inbox.storage.db.InboxDbHelper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.location.PushwooshLocation;
import com.pushwoosh.location.network.exception.LocationNotAvailableException;
import com.pushwoosh.sender.PushMessage;
import com.pushwoosh.sender.PushSender;
import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.json.JSONArray;
import org.junit.Ignore;

import java.io.DataOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Created by aevstefeev on 23/03/2018.
 */
@Ignore
public class BaseTest extends TestCase {
    protected static final String TAG = "PWBaseTest";
    public static final int TIME_OUT = 30000;
    public static final int TIME_OUT_WAIT_NOTIFICATION = 7;
    DateFormat inboxDateFormat = new SimpleDateFormat("yyyy-MM-dd");

    protected StatusBarNotification[] getNotificationList() {
        Context applicationContext = AndroidPlatformModule.getApplicationContext();
        final NotificationManager mNotificationManager = (NotificationManager) applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        return mNotificationManager.getActiveNotifications();
    }

    protected boolean notificationsIsEmpty() {
        return getNotificationList().length == 0;
    }

    protected void waitNotification() throws InterruptedException {
        while (notificationsIsEmpty()) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    protected void assertEqualsTextMessage(String text) throws InterruptedException {
        StatusBarNotification[] notifications = getNotificationList();
        StatusBarNotification notification = notifications[0];
        Assert.assertEquals(notifications.length, 1);
        Assert.assertEquals(text, notification.getNotification().tickerText);
    }

    protected boolean isOneOfPushMessagesEqualsText(String text) {
        StatusBarNotification[] notifications = getNotificationList();
        Log.d(TAG, "total number of push messages: " + notifications.length);
        for (int i = 0; i < notifications.length; i++) {
            Log.d(TAG, "msg" + i + ": " + notifications[i].getNotification().tickerText);
            if (TextUtils.equals(notifications[i].getNotification().tickerText, text)) {
                return true;
            }
        }
        return false;
    }

    protected void wait(int second) throws Exception {
        TimeUnit.SECONDS.sleep(second);
    }

    protected void sendPush(String message, String sound) {
        sendPush(message, sound, false);
    }

    // token for ainohova: pCStgdc6EIjBUX5optdnXAbo3oYHjHvZFYOPLSVA2WNXvqofueCwf92BhDXdbFCUY2RPqtAR6AndqxP6ZOVl
    protected void sendPush(String message, String sound, boolean inbox) {
        Context applicationContext = AndroidPlatformModule.getApplicationContext();
        PushSender pushSender = new PushSender(applicationContext, "QjBZRWfYuG115ugeIl0ZEQRvzH2dHCDB8S2Af3kofytjVH0UlgGNFGsXUOMgRpS3il1GojOcS7IHlCNZMXno");
        PushMessage pushMessage = new PushMessage();
        pushMessage.content = message;
        pushMessage.notificationParams = new HashMap<>();
        JSONArray platforms = new JSONArray();
        platforms.put(3);
        pushMessage.notificationParams.put("platforms", platforms);
        pushMessage.notificationParams.put("android_sound", sound);
        if (inbox) {
            setInbox(pushMessage);
        }

        pushSender.sendPush(pushMessage);
    }

    private void setInbox(PushMessage pushMessage) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 2);
        String dataString = inboxDateFormat.format(calendar.getTime());
        pushMessage.notificationParams.put("inbox_date",dataString);
    }

    public void runCommand(String command) throws Exception {
        try {
            Process su = Runtime.getRuntime().exec(command);
            DataOutputStream outputStream = new DataOutputStream(su.getOutputStream());

            outputStream.writeBytes("screenrecord --time-limit 10 /sdcard/MyVideo.mp4\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();
            su.waitFor();
        } catch (IOException e) {
            throw new Exception(e);
        } catch (InterruptedException e) {
            throw new Exception(e);
        }
    }

    protected void clearInbox() {
        Context context = InstrumentationRegistry.getContext();
        DbInboxStorage dbInboxStorage = new DbInboxStorage(new InboxDbHelper(context));
        Collection<InboxMessageInternal> list = dbInboxStorage.getAllActualMessages();
        List<String> listCode = new ArrayList<>();
        for(InboxMessageInternal inboxMessageInternal : list){
            listCode.add(inboxMessageInternal.getId());
        }
        PushwooshInbox.deleteMessages(listCode);
    }

    protected boolean registerForPushMessages() throws InterruptedException {
        Log.d(TAG, "attempting to register for push messages");
        if (!TextUtils.isEmpty(Pushwoosh.getInstance().getPushToken())) {
            Log.d(TAG, "you've already been registered");
            return true;
        }
        CountDownLatch countDownLatch = new CountDownLatch(1);
        TestRegistrationCallback callback = new TestRegistrationCallback(countDownLatch);
        Pushwoosh.getInstance().registerForPushNotifications(callback);
        countDownLatch.await(15, TimeUnit.SECONDS);
        return callback.isSuccess();
    }

    protected void clearPushHistory() {
        Log.d(TAG, "clear push history");
        PushwooshProxyController.getPushwooshProxy().clearNotificationCenter();
    }

    protected boolean startTrackingLocation() throws InterruptedException {
        Log.d(TAG, "attempting to start tracking location");
        CountDownLatch countDownLatch = new CountDownLatch(1);
        TestLocationCallback callback = new TestLocationCallback(countDownLatch);
        PushwooshLocation.startLocationTracking(callback);
        countDownLatch.await(15, TimeUnit.SECONDS);
        return callback.isSuccess();
    }

    protected void stopTrackingLocation() {
        PushwooshLocation.stopLocationTracking();
        Log.d(TAG, "location tracking has stopped");
    }

    private class TestRegistrationCallback implements Callback<String, RegisterForPushNotificationsException> {
        private CountDownLatch countDownLatch;
        private boolean success;

        public boolean isSuccess() {
            return success;
        }

        public TestRegistrationCallback(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void process(@NonNull Result<String, RegisterForPushNotificationsException> result) {
            if (result.isSuccess()) {
                Log.d(TAG, "successfully registered");
                this.success = true;
            } else {
                Log.d(TAG, "an error occurred during registration");
                this.success = false;
            }
            countDownLatch.countDown();
        }
    }

    private class TestLocationCallback implements Callback<Void, LocationNotAvailableException> {
        private CountDownLatch countDownLatch;
        private boolean success;

        public TestLocationCallback(CountDownLatch countDownLatch) {
            this.countDownLatch = countDownLatch;
        }

        public boolean isSuccess() {
            return success;
        }

        @Override
        public void process(@NonNull Result<Void, LocationNotAvailableException> result) {
            if (result.isSuccess()) {
                Log.d(TAG, "location tracking has started");
                this.success = true;
            } else {
                Log.d(TAG, "an error occurred during the start of tracking");
                this.success = false;
            }
            countDownLatch.countDown();
        }
    }
}
