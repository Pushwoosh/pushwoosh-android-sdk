/*
 *
 * Copyright (c) 2017. Pushwoosh Inc. (http://www.pushwoosh.com)
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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;

import java.util.Collections;
import java.util.Set;

public class LocalNotificationStorage {
    private static final String TAG = "LocalNotificationStorage";

    @Nullable
    private final DbLocalNotificationHelper dbLocalNotificationHelper;

    LocalNotificationStorage(@NonNull DbLocalNotificationHelper dbLocalNotificationHelper) {
        this.dbLocalNotificationHelper = dbLocalNotificationHelper;
    }

    public void saveLocalNotification(int id, Bundle extras, long triggerAtMillis) {
        PWLog.noise(TAG, "Saved local push: " + extras.toString());
        try {
            if (dbLocalNotificationHelper == null) {
                return;
            }
            DbLocalNotification dbLocalNotification = new DbLocalNotification(id, 0, "", triggerAtMillis, extras);
            dbLocalNotificationHelper.putDbLocalNotification(dbLocalNotification);
        } catch (Exception e) {
            PWLog.exception(e);
        }
    }

    public void enumerateDbLocalNotificationList(DbLocalNotificationHelper.EnumeratorLocalNotification enumeratorLocalNotification) {
        if (dbLocalNotificationHelper != null) {
            dbLocalNotificationHelper.enumerateDbLocalNotificationList(enumeratorLocalNotification);
        }else {
            PWLog.error(TAG, "dbLocalNotificationHelper is null, can't enumerate local notification list");
        }
    }

    public void removeLocalNotification(int id) {
        PWLog.noise(TAG, "Removed dbLocalNotification: " + id);
        try {
            if (dbLocalNotificationHelper == null) {
                PWLog.error("dbLocalNotificationHelper is null, can't remove local push");
                return;
            }
            dbLocalNotificationHelper.removeDbLocalNotification(id);
        } catch (Exception e) {
            // Avoid ConcurrentModificationException
            PWLog.exception(e);
        }
    }


    public Set<Integer> getRequestIds() {
        if (dbLocalNotificationHelper == null) {
            return Collections.emptySet();
        }
        Set<Integer> integerSet = dbLocalNotificationHelper.getAllRequestIds();
        return integerSet;
    }

    public int nextRequestId() {
        if (dbLocalNotificationHelper == null) {
            return 0;
        }
        return dbLocalNotificationHelper.nextRequestId();
    }

    public void removeLocalNotificationShown(int notificationId, String notificationTag) {
        if (dbLocalNotificationHelper == null) {
            PWLog.error("dbLocalNotificationHelper is null, can't removeLocalNotificationShown");
            return;
        }
        DbLocalNotification dbLocalNotification = dbLocalNotificationHelper.getDbLocalNotificationShown(notificationId, notificationTag);
        if (dbLocalNotification != null) {
            int requestId = dbLocalNotification.getRequestId();
            dbLocalNotificationHelper.removeDbLocalNotificationShown(requestId);
        }
    }

    public void addLocalNotificationShown(int requestId, int notificationId, String notificationTag) {
        DbLocalNotification dbLocalNotification = new DbLocalNotification(requestId, notificationId, notificationTag);
        if (dbLocalNotificationHelper != null) {
            dbLocalNotificationHelper.addDbLocalNotificationShown(dbLocalNotification);
        }
    }

    public DbLocalNotification getLocalNotificationShown(int requestId) {
        if (dbLocalNotificationHelper == null) {
            PWLog.error("dbLocalNotificationHelper is null, can't get Notification");
            return null;
        }
        String requestIdString = Integer.toString(requestId);
        DbLocalNotification dbLocalNotification = dbLocalNotificationHelper.getDbLocalNotificationShown(requestIdString);
        if (dbLocalNotification == null) {
            PWLog.noise("local notification not found");
            return null;
        }
        dbLocalNotificationHelper.removeDbLocalNotificationShown(requestId);

        return dbLocalNotification;
    }
}
