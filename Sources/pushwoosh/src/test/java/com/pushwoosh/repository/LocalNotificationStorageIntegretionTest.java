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
import android.os.Bundle;
import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created by aevstefeev on 21/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class LocalNotificationStorageIntegretionTest extends BaseLocalNotificationTest {
    private LocalNotificationStorage localNotificationStorage;


    private DbLocalNotificationHelper dbLocalNotificationHelper;


    @Before
    public void setUp() throws Exception {
        Context context = RuntimeEnvironment.application;
        dbLocalNotificationHelper = spy(new DbLocalNotificationHelper(context));
        localNotificationStorage = new LocalNotificationStorage(dbLocalNotificationHelper);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void saveLocalPush() throws Exception {
        Bundle bundle = createTestBundle();
        localNotificationStorage.saveLocalNotification(1, bundle, 10L);

        Set<String> idsSet = new HashSet<>();
        idsSet.add(String.valueOf(1));

        DbLocalNotification dbLocalNotification = new DbLocalNotification(1, 0, "", 10L, bundle);
        verify(dbLocalNotificationHelper).putDbLocalNotification(eq(dbLocalNotification));
    }

    @NonNull
    private Bundle createTestBundle() {
        Bundle bundle = Mockito.spy(new Bundle());
        bundle.putInt("pw_msg", 1);
        bundle.putBoolean("local", true);
        bundle.putString("title", "title1");
        return bundle;
    }

    @Test
    public void getBundleWithLocalNotificationData() throws Exception {
        Bundle bundle = createTestBundle();
        localNotificationStorage.saveLocalNotification(1, bundle, 10L);

        List<DbLocalNotification> localPushes = new ArrayList<>();
        localNotificationStorage.enumerateDbLocalNotificationList(localPushes::add);

        Assert.assertEquals(1, localPushes.size());
        DbLocalNotification dbLocalNotification = localPushes.get(0);
        Bundle bundleResult = dbLocalNotification.getBundle();
        Assert.assertEquals(3, bundleResult.size());
        Assert.assertEquals(1, bundleResult.getInt("pw_msg", 0));
        Assert.assertEquals("title1", bundleResult.getString("title", ""));
        Assert.assertEquals(true, bundleResult.getBoolean("local", false));
        Assert.assertEquals("Bundle[{pw_msg=1, local=true, title=title1}]", bundleResult.toString());
    }

    @Test
    public void removeLocalNotification() throws Exception {
        Bundle bundle = Mockito.spy(new Bundle());
        localNotificationStorage.saveLocalNotification(1, bundle, 10L);
        localNotificationStorage.removeLocalNotification(1);

        Assert.assertTrue(localNotificationStorage.getRequestIds().isEmpty());
        List<DbLocalNotification> localNotificationList = new ArrayList<>();
        localNotificationStorage.enumerateDbLocalNotificationList(localNotificationList::add);
        Assert.assertTrue(localNotificationList.isEmpty());

        localNotificationStorage.saveLocalNotification(1, bundle, 10L);
        localNotificationStorage.saveLocalNotification(2, bundle, 10L);
        localNotificationStorage.removeLocalNotification(1);

        Set<Integer> requestIds = localNotificationStorage.getRequestIds();
        Assert.assertEquals(1, requestIds.size());
        Assert.assertTrue(requestIds.contains(2));

        List<DbLocalNotification> bundleList = new ArrayList<>();
        localNotificationStorage.enumerateDbLocalNotificationList(bundleList::add);
        Assert.assertEquals(1, bundleList.size());

        DbLocalNotification bundleResult = bundleList.get(0);
        Assert.assertEquals(10, bundleResult.getTriggerAtMillis());

    }

    @Test
    public void getRequestIds() throws Exception {
        Bundle bundle = Mockito.spy(new Bundle());
        Set<Integer> notificationSet1 = localNotificationStorage.getRequestIds();
        Assert.assertTrue(notificationSet1.isEmpty());

        localNotificationStorage.saveLocalNotification(1, bundle, 10L);
        Set<Integer> notificationSet2 = localNotificationStorage.getRequestIds();
        Assert.assertEquals(1, notificationSet2.size());
        Assert.assertTrue(notificationSet2.contains(1));

        localNotificationStorage.saveLocalNotification(21, bundle, 10L);
        Set<Integer> notificationSet3 = localNotificationStorage.getRequestIds();
        Assert.assertEquals(2, notificationSet3.size());
        Assert.assertTrue(notificationSet3.contains(1));
        Assert.assertTrue(notificationSet3.contains(21));
    }

    @Test
    public void nextRequestId() throws Exception {
        File file = new File(RuntimeEnvironment.application.getFilesDir(), "next_request_id");
        file.delete();

        int nextId1 = localNotificationStorage.nextRequestId();
        int nextId2 = localNotificationStorage.nextRequestId();
        int nextId3 = localNotificationStorage.nextRequestId();

        Assert.assertEquals(0, nextId1);
        Assert.assertEquals(1, nextId2);
        Assert.assertEquals(2, nextId3);
    }

    @Test
    public void removeLocalNotificationShown() throws Exception {
        dbLocalNotificationHelper.addDbLocalNotificationShown(new DbLocalNotification(12, 21, "tag1"));
        dbLocalNotificationHelper.addDbLocalNotificationShown(new DbLocalNotification(13, 22, "tag2"));

        localNotificationStorage.removeLocalNotificationShown(1, "tag1");
        verify(dbLocalNotificationHelper, never()).removeDbLocalNotificationShown(anyInt());

        Assert.assertNotNull(dbLocalNotificationHelper.getDbLocalNotificationShown("12"));
        Assert.assertNotNull(dbLocalNotificationHelper.getDbLocalNotificationShown("13"));

        localNotificationStorage.removeLocalNotificationShown(21, "tag1");
        verify(dbLocalNotificationHelper).removeDbLocalNotificationShown(eq(12));

        Assert.assertNull(dbLocalNotificationHelper.getDbLocalNotificationShown("12"));
        Assert.assertNotNull(dbLocalNotificationHelper.getDbLocalNotificationShown("13"));

    }

    @Test
    public void addLocalNotificationShown() throws Exception {
        localNotificationStorage.addLocalNotificationShown(12, 21, "tag1");

        DbLocalNotification localNotificationShown = dbLocalNotificationHelper.getDbLocalNotificationShown("12");
        Assert.assertEquals(12, localNotificationShown.getRequestId());
        Assert.assertEquals(21, localNotificationShown.getNotificationId());
        Assert.assertEquals("tag1", localNotificationShown.getNotificationTag());
    }

    @Test
    public void getLocalNotificationShown() throws Exception {
        localNotificationStorage.addLocalNotificationShown(12, 21, "tag1");
        localNotificationStorage.addLocalNotificationShown(13, 22, "tag2");
        localNotificationStorage.addLocalNotificationShown(14, 25, "tag3");

        DbLocalNotification notification = localNotificationStorage.getLocalNotificationShown(12);

        verify(dbLocalNotificationHelper).removeDbLocalNotificationShown(12);
        assertEqualsLocalNotification(new DbLocalNotification(12, 21, "tag1"), notification);
        List<DbLocalNotification> localNotificationList = new ArrayList<>();
        dbLocalNotificationHelper.enumerateDbLocalNotificationShownList(localNotificationList::add);
        Assert.assertEquals(2, localNotificationList.size());

    }

}