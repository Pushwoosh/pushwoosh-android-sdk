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

import android.app.Application;
import android.os.Bundle;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by aevstefeev on 22/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
public class DbLocalNotificationHelperTest extends BaseLocalNotificationTest {
    private DbLocalNotificationHelper dbLocalNotificationHelper;



    @Before
    public void setUp() throws Exception {
        super.setUp();
        Application application = RuntimeEnvironment.application;
        dbLocalNotificationHelper = new DbLocalNotificationHelper(application);
    }



    @After
    public void tearDown() throws Exception {
    }

    private void putLocalNotification() {
        dbLocalNotificationHelper.putDbLocalNotification(dbLocalNotification1);
        dbLocalNotificationHelper.putDbLocalNotification(dbLocalNotification2);
        dbLocalNotificationHelper.putDbLocalNotification(dbLocalNotification3);
    }

    private void addNotifivationShown() {
        dbLocalNotificationHelper.addDbLocalNotificationShown(dbLocalNotification1);
        dbLocalNotificationHelper.addDbLocalNotificationShown(dbLocalNotification2);
        dbLocalNotificationHelper.addDbLocalNotificationShown(dbLocalNotification3);
    }

    @Test
    public void getLocalNotification() throws Exception {
        putLocalNotification();

        DbLocalNotification dbLocalNotification = dbLocalNotificationHelper.getLocalNotification("11");

        assertEqualsLocalNotification(dbLocalNotification, dbLocalNotification1);
    }



    @Test
    public void getLocalNotificationShown() throws Exception {
        addNotifivationShown();

        DbLocalNotification dbLocalNotification = dbLocalNotificationHelper.getDbLocalNotificationShown("12");

        assertEqualsLocalNotification(dbLocalNotification, dbLocalNotification2);
    }

    @Test
    public void getAllRequestIds() throws Exception {
        putLocalNotification();

        Set<Integer> idsSet = dbLocalNotificationHelper.getAllRequestIds();
        Assert.assertEquals(3, idsSet.size());
        Assert.assertTrue(idsSet.contains(11));
        Assert.assertTrue(idsSet.contains(12));
    }

    @Test
    public void putLocalNotificationMaxCapacity() throws Exception {
        for (int i = 0; i < 15; i++) {
            DbLocalNotification dbLocalNotification = new DbLocalNotification(i, i, "tag" + i, i, new Bundle());
            dbLocalNotificationHelper.addDbLocalNotificationShown(dbLocalNotification);
        }
        List<DbLocalNotification> dbLocalNotificationList = new ArrayList<>();
        dbLocalNotificationHelper.enumerateDbLocalNotificationShownList(dbLocalNotificationList::add);

        int size = dbLocalNotificationList.size();
        Assert.assertEquals(10, size);
        Assert.assertEquals(5, dbLocalNotificationList.get(0).getRequestId());
        Assert.assertEquals(14, dbLocalNotificationList.get(size - 1).getRequestId());
    }

    @Test
    public void removeLocalNotification() throws Exception {
        putLocalNotification();

        dbLocalNotificationHelper.removeDbLocalNotification(28);
        Set<Integer> idsSet1 = dbLocalNotificationHelper.getAllRequestIds();
        Assert.assertEquals(3, idsSet1.size());

        dbLocalNotificationHelper.removeDbLocalNotification(12);
        Set<Integer> idsSet2 = dbLocalNotificationHelper.getAllRequestIds();
        Assert.assertEquals(2, idsSet2.size());
        Assert.assertTrue(idsSet2.contains(11));
    }

    @Test
    public void removeLocalNotificationShown() throws Exception {
        addNotifivationShown();

        dbLocalNotificationHelper.removeDbLocalNotificationShown(11);

        List<DbLocalNotification> dbLocalPusheList = new ArrayList<>();
        dbLocalNotificationHelper.enumerateDbLocalNotificationShownList(dbLocalPusheList::add);
        Assert.assertEquals(2, dbLocalPusheList.size());
        assertEqualsLocalNotification(dbLocalNotification2, dbLocalPusheList.get(0));
    }

    @Test
    public void getLocalNotificationShownByTagAndNotificationId() throws Exception {
        addNotifivationShown();

        DbLocalNotification dbLocalNotificationResult = dbLocalNotificationHelper.getDbLocalNotificationShown(21, "tag1");
        assertEqualsLocalNotification(dbLocalNotification1, dbLocalNotificationResult);

        DbLocalNotification dbLocalNotificationResult2 = dbLocalNotificationHelper.getDbLocalNotificationShown(22, "tag2");
        assertEqualsLocalNotification(dbLocalNotification2, dbLocalNotificationResult2);

    }

    @Test
    public void setNextRequestId() throws Exception {
        dbLocalNotificationHelper.setNextRequestId(6);
        int nextId = dbLocalNotificationHelper.nextRequestId();
        Assert.assertEquals(7, nextId);
    }

    @Test
    public void nextRequestId() throws Exception {
        int nextId1 = dbLocalNotificationHelper.nextRequestId();
        int nextId2 = dbLocalNotificationHelper.nextRequestId();
        int nextId3 = dbLocalNotificationHelper.nextRequestId();

        Assert.assertEquals(0, nextId1);
        Assert.assertEquals(1, nextId2);
        Assert.assertEquals(2, nextId3);
    }

    @Test
    public void getDbLocalNotificationShownWithNullTagTest(){
        DbLocalNotification dbLocalNotificationResult = dbLocalNotificationHelper.getDbLocalNotificationShown(24,null);

    }
}