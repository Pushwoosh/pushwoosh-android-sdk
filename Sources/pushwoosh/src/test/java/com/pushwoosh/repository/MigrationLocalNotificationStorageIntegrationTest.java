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

package com.pushwoosh.repository;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import android.util.Log;

import com.pushwoosh.MigrateLocalNotificationStorageTask;
import com.pushwoosh.internal.preference.PreferenceArrayListValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.work.Configuration;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by aevstefeev on 22/03/2018.
 */
@Ignore
@RunWith(RobolectricTestRunner.class)
public class MigrationLocalNotificationStorageIntegrationTest {
    private PreferenceValueFactoryMock preferenceValueFactory;
    private SharedPreferences sharedPreferences;
    private DbLocalNotificationHelper dbLocalNotificationHelper;

    @Before
    public void setUp(){
        Context context = RuntimeEnvironment.application;
        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(Log.DEBUG)
                .setExecutor(new SynchronousExecutor())
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(
                context, config);

        dbLocalNotificationHelper = new DbLocalNotificationHelper(context);
        sharedPreferences = Mockito.mock(SharedPreferences.class);
        preferenceValueFactory = new PreferenceValueFactoryMock();

        prepareMock();
    }


    private void prepareMock() {
        SharedPreferences.Editor editor = Mockito.mock(SharedPreferences.Editor.class);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putStringSet(eq("pushwoosh_local_push_ids"), anySet())).thenReturn(editor);

        prepareNextIdMock();
        prerpareLocalNotification();
        prepareShownMock();
    }

    private void prepareNextIdMock() {
        PreferenceIntValue nextId = preferenceValueFactory.getPreferenceIntValue();
        when(nextId.get()).thenReturn(22);
    }

    private void prerpareLocalNotification() {
        Set<String> idSet = new HashSet<>(Arrays.asList("1", "2", "3"));
        when(sharedPreferences.getStringSet(eq("pushwoosh_local_push_ids"), anySet())).thenReturn(idSet);
        when(sharedPreferences.getAll()).thenReturn(new HashMap<>());
        when(sharedPreferences.getLong(Mockito.anyString(), eq(0L))).thenReturn(10L);
    }

    private void prepareShownMock() {
        PreferenceArrayListValue<String> shownNotification = preferenceValueFactory.getPreferenceArrayListValue();
        ArrayList<String> shownList = new ArrayList<>();
        shownList.add("{\"requestId\":11,\"notificationId\":101,\"notificationTag\":\"tag1\"}");
        shownList.add("{\"requestId\":12,\"notificationId\":102,\"notificationTag\":\"tag2\"}");
        shownList.add("{\"requestId\":13,\"notificationId\":103,\"notificationTag\":\"tag3\"}");
        Mockito.when(shownNotification.get()).thenReturn(shownList);
    }



    private void assertLocalPush(List<DbLocalNotification> dbLocalNotificationList) {
        Assert.assertEquals(1, dbLocalNotificationList.get(0).getRequestId());
        Assert.assertEquals(2, dbLocalNotificationList.get(1).getRequestId());
        Assert.assertEquals(3, dbLocalNotificationList.get(2).getRequestId());

        Assert.assertEquals(10L, dbLocalNotificationList.get(0).getTriggerAtMillis());
    }



    private void checkShownList(List<DbLocalNotification> shownListResult) {
        Assert.assertEquals(11, shownListResult.get(0).getRequestId());
        Assert.assertEquals(12, shownListResult.get(1).getRequestId());
        Assert.assertEquals(13, shownListResult.get(2).getRequestId());

        Assert.assertEquals(101, shownListResult.get(0).getNotificationId());
        Assert.assertEquals(102, shownListResult.get(1).getNotificationId());
        Assert.assertEquals(103, shownListResult.get(2).getNotificationId());

        Assert.assertEquals("tag1", shownListResult.get(0).getNotificationTag());
        Assert.assertEquals("tag2", shownListResult.get(1).getNotificationTag());
        Assert.assertEquals("tag3", shownListResult.get(2).getNotificationTag());
    }


    @Test
    public void executeMigration() {
        new MigrateLocalNotificationStorageExtendedTask(RuntimeEnvironment.application.getApplicationContext(), () -> {
            int nextId = dbLocalNotificationHelper.nextRequestId();
            Assert.assertEquals(23, nextId);
            Set<Integer> idList = dbLocalNotificationHelper.getAllRequestIds();
            Assert.assertEquals(3, idList.size());
            List<DbLocalNotification> notificationList = new ArrayList<>();
            dbLocalNotificationHelper.enumerateDbLocalNotificationList(notificationList::add);
            Assert.assertEquals(3, notificationList.size());
            assertLocalPush(notificationList);

            List<DbLocalNotification> notificationShownList = new ArrayList<>();
            dbLocalNotificationHelper.enumerateDbLocalNotificationShownList(notificationShownList::add);
            Assert.assertEquals(3, notificationShownList.size());
            checkShownList(notificationShownList);
        }).execute();
    }

    private class MigrateLocalNotificationStorageExtendedTask extends MigrateLocalNotificationStorageTask {
        private OnTaskCompeted onTaskCompeted;
        public MigrateLocalNotificationStorageExtendedTask(@NonNull Context context, OnTaskCompeted onTaskCompeted) {
            super(context);
            this.onTaskCompeted = onTaskCompeted;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            onTaskCompeted.onCompleted();
        }
    }

    private interface OnTaskCompeted {
        void onCompleted();
    }

}