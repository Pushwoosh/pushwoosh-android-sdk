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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

// Regression guard for the Cursor leak in DbLocalNotificationHelper.removeFirstDbLocalNotification:
// the spy only observes the real Cursor, so isClosed() reports the genuine leak rather than a stub.
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class DbLocalNotificationRemoveFirstCursorTest extends BaseLocalNotificationTest {

    private DbLocalNotificationHelper dbLocalNotificationHelper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Application application = RuntimeEnvironment.application;
        dbLocalNotificationHelper = new DbLocalNotificationHelper(application);
    }

    private void fillToCap() {
        for (int i = 0; i < 10; i++) {
            DbLocalNotification notification = new DbLocalNotification(i, i, "tag" + i, i, new Bundle());
            dbLocalNotificationHelper.addDbLocalNotificationShown(notification);
        }
    }

    @Test
    public void evictingOldestShownNotification_leavesNoOpenCursor() throws Exception {
        fillToCap();

        SQLiteDatabase realDb = dbLocalNotificationHelper.getWritableDatabase();
        SQLiteDatabase spyDb = Mockito.spy(realDb);
        Cursor[] captured = new Cursor[1];
        Mockito.doAnswer(invocation -> {
                    Cursor real = (Cursor) invocation.callRealMethod();
                    captured[0] = real;
                    return real;
                })
                .when(spyDb)
                .query(anyString(), isNull(), isNull(), isNull(), isNull(), isNull(), anyString());

        DbLocalNotificationHelper spyHelper = Mockito.spy(dbLocalNotificationHelper);
        Mockito.doReturn(spyDb).when(spyHelper).getWritableDatabase();

        DbLocalNotification triggeringEviction = new DbLocalNotification(10, 10, "tag10", 10, new Bundle());
        spyHelper.addDbLocalNotificationShown(triggeringEviction);

        assertNotNull("eviction query should have run and been captured", captured[0]);
        assertTrue("the cursor opened to find the eviction candidate must be closed", captured[0].isClosed());
    }
}
