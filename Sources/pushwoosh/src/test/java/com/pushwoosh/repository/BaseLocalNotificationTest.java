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

import android.os.Bundle;

import org.junit.Assert;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by aevstefeev on 23/03/2018.
 */

public class BaseLocalNotificationTest {

    protected DbLocalNotification dbLocalNotification1;
    protected DbLocalNotification dbLocalNotification2;
    protected DbLocalNotification dbLocalNotification3;
    protected DbLocalNotification dbLocalNotificationWithNullTag;
    protected Bundle bundle1;
    protected Bundle bundle2;
    protected Bundle bundle3;

    protected List<DbLocalNotification> dbLocalNotificationList = new ArrayList<>();


    @Before
    public void setUp() throws Exception {
        createTestDate();
    }

    private void createTestDate() {
        bundle1 = new Bundle();
        bundle1.putString("test", "test1");
        bundle2 = new Bundle();
        bundle2.putString("test", "test2");
        bundle3 = new Bundle();
        bundle3.putString("test", "test3");
        dbLocalNotification1 = new DbLocalNotification(11, 21, "tag1", 10L, bundle1);
        dbLocalNotification2 = new DbLocalNotification(12, 22, "tag2", 20L, bundle2);
        dbLocalNotification3 = new DbLocalNotification(13, 23, "tag3", 30L, bundle3);
        dbLocalNotificationWithNullTag = new DbLocalNotification(14, 24, null, 40L, bundle3);
        dbLocalNotificationList = Arrays.asList(dbLocalNotification1, dbLocalNotification2, dbLocalNotification3);
    }


    protected void assertEqualsLocalNotification(DbLocalNotification dbLocalNotification1, DbLocalNotification dbLocalNotification2) {
        Assert.assertEquals(dbLocalNotification2.getRequestId(), dbLocalNotification1.getRequestId());
        Assert.assertEquals(dbLocalNotification2.getNotificationId(), dbLocalNotification1.getNotificationId());
        Assert.assertEquals(dbLocalNotification2.getNotificationTag(), dbLocalNotification1.getNotificationTag());
        Assert.assertEquals(dbLocalNotification2.getTriggerAtMillis(), dbLocalNotification1.getTriggerAtMillis());
        Assert.assertEquals(dbLocalNotification2.getBundle().toString(), dbLocalNotification1.getBundle().toString());
    }

}
