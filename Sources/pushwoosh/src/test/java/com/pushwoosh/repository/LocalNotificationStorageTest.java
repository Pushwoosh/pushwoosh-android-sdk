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

import com.pushwoosh.notification.LocalNotification;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by aevstefeev on 21/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "AndroidManifest.xml")
public class LocalNotificationStorageTest extends BaseLocalNotificationTest {
    private LocalNotificationStorage localNotificationStorage;


    private DbLocalNotificationHelper dbLocalNotificationHelper;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        dbLocalNotificationHelper = Mockito.mock(DbLocalNotificationHelper.class);
        localNotificationStorage = new LocalNotificationStorage(dbLocalNotificationHelper);
    }

    @Test
    public void saveLocalNotification() throws Exception {
        Bundle bundle = Mockito.spy(new Bundle());
        localNotificationStorage.saveLocalNotification(1, bundle, 10L);
        DbLocalNotification dbLocalNotification = new DbLocalNotification(1, 0, "", 10L, bundle);

        verify(dbLocalNotificationHelper).putDbLocalNotification(eq(dbLocalNotification));
    }

    @Test
    public void getBundleWithLocalNotificationData() throws Exception {
        List<DbLocalNotification> dbLocalNotifications = new ArrayList<>();
        localNotificationStorage.enumerateDbLocalNotificationList(dbLocalNotifications::add);

        ArgumentCaptor<DbLocalNotificationHelper.EnumeratorLocalNotification> enumeratorLocalNotificationArgumentCaptor =
                ArgumentCaptor.forClass(DbLocalNotificationHelper.EnumeratorLocalNotification.class);

        verify(dbLocalNotificationHelper).enumerateDbLocalNotificationList(enumeratorLocalNotificationArgumentCaptor.capture());
        DbLocalNotificationHelper.EnumeratorLocalNotification enumeratorLocalNotification =
                enumeratorLocalNotificationArgumentCaptor.getValue();
        enumeratorLocalNotification.enumerate(dbLocalNotificationList.get(0));
        enumeratorLocalNotification.enumerate(dbLocalNotificationList.get(1));
        enumeratorLocalNotification.enumerate(dbLocalNotificationList.get(2));

        Assert.assertEquals(3, dbLocalNotifications.size());
        Assert.assertEquals("Bundle[{test=test1}]", dbLocalNotifications.get(0).getBundle().toString());
        Assert.assertEquals("Bundle[{test=test2}]", dbLocalNotifications.get(1).getBundle().toString());
        Assert.assertEquals("Bundle[{test=test3}]", dbLocalNotifications.get(2).getBundle().toString());
    }

    @Test
    public void removeLocalNotification() throws Exception {
        localNotificationStorage.removeLocalNotification(1);
        verify(dbLocalNotificationHelper).removeDbLocalNotification(1);
    }

    @Test
    public void getRequestIds() throws Exception {
        Set<Integer> idList = new HashSet<>();
        idList.add(1);
        idList.add(2);
        idList.add(3);
        when(dbLocalNotificationHelper.getAllRequestIds()).thenReturn(idList);

        Set<Integer> ids = localNotificationStorage.getRequestIds();
        verify(dbLocalNotificationHelper).getAllRequestIds();
        Assert.assertEquals(ids.size(), 3);
        Assert.assertTrue(ids.contains(1));
        Assert.assertTrue(ids.contains(2));
        Assert.assertTrue(ids.contains(3));
    }

    @Test
    public void nextRequestId() throws Exception {
        localNotificationStorage.nextRequestId();
        verify(dbLocalNotificationHelper).nextRequestId();
    }

    @Test
    public void getDbLocalNotificationShown() throws Exception {
        DbLocalNotification dbLocalNotification = new DbLocalNotification(1, 1, "tag1", 1L, new Bundle());
        when(dbLocalNotificationHelper.getDbLocalNotificationShown(1, "tag1")).thenReturn(dbLocalNotification);

        localNotificationStorage.removeLocalNotificationShown(1, "tag1");

        verify(dbLocalNotificationHelper).removeDbLocalNotificationShown(eq(1));
    }

    // Verifies that getLocalNotificationShown returns the stored record and removes it on lookup hit (read-once).
    @Test
    public void getLocalNotificationShown_recordExists_returnsRecordAndRemoves() {
        DbLocalNotification stored = new DbLocalNotification(42, 1, "tag", 1L, new Bundle());
        when(dbLocalNotificationHelper.getDbLocalNotificationShown("42")).thenReturn(stored);

        DbLocalNotification result = localNotificationStorage.getLocalNotificationShown(42);

        Assert.assertSame(stored, result);
        verify(dbLocalNotificationHelper).removeDbLocalNotificationShown(42);
    }

    // Verifies that getLocalNotificationShown returns null and does NOT remove when record is missing.
    @Test
    public void getLocalNotificationShown_recordMissing_returnsNullAndSkipsRemove() {
        when(dbLocalNotificationHelper.getDbLocalNotificationShown(anyString())).thenReturn(null);

        DbLocalNotification result = localNotificationStorage.getLocalNotificationShown(42);

        Assert.assertNull(result);
        verify(dbLocalNotificationHelper, never()).removeDbLocalNotificationShown(anyInt());
    }

    // Verifies that removeLocalNotificationShown uses requestId from the stored record, not the input notificationId.
    @Test
    public void removeLocalNotificationShown_usesRequestIdFromStoredRecord() {
        DbLocalNotification stored = new DbLocalNotification(7, 99, "t", 1L, new Bundle());
        when(dbLocalNotificationHelper.getDbLocalNotificationShown(99, "t")).thenReturn(stored);

        localNotificationStorage.removeLocalNotificationShown(99, "t");

        verify(dbLocalNotificationHelper).removeDbLocalNotificationShown(7);
        verify(dbLocalNotificationHelper, never()).removeDbLocalNotificationShown(99);
    }

    // Verifies that removeLocalNotificationShown does NOT call remove when the record is not found.
    @Test
    public void removeLocalNotificationShown_recordMissing_skipsRemove() {
        when(dbLocalNotificationHelper.getDbLocalNotificationShown(anyInt(), anyString())).thenReturn(null);

        localNotificationStorage.removeLocalNotificationShown(99, "t");

        verify(dbLocalNotificationHelper, never()).removeDbLocalNotificationShown(anyInt());
    }

    // Verifies that addLocalNotificationShown assembles a DbLocalNotification with the three id/tag fields and forwards it to the helper.
    @Test
    public void addLocalNotificationShown_assemblesRecordAndForwardsToHelper() {
        localNotificationStorage.addLocalNotificationShown(5, 6, "tagX");

        ArgumentCaptor<DbLocalNotification> captor = ArgumentCaptor.forClass(DbLocalNotification.class);
        verify(dbLocalNotificationHelper).addDbLocalNotificationShown(captor.capture());
        DbLocalNotification captured = captor.getValue();
        Assert.assertEquals(5, captured.getRequestId());
        Assert.assertEquals(6, captured.getNotificationId());
        Assert.assertEquals("tagX", captured.getNotificationTag());
    }

}