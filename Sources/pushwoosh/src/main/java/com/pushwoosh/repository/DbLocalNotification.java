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

/**
 * Created by aevstefeev on 21/03/2018.
 */

public class DbLocalNotification {
    private int requestId;
    private int notificationId;
    private String notificationTag;
    private long triggerAtMillis;
    private Bundle bundle;

    public DbLocalNotification(int requestId, int notificationId, String notificationTag) {
        this.requestId = requestId;
        this.notificationId = notificationId;
        this.notificationTag = notificationTag;
        bundle = new Bundle();
    }

    public DbLocalNotification(int requestId, int notificationId, String notificationTag, long triggerAtMillis, Bundle bundle) {
        this.requestId = requestId;
        this.notificationId = notificationId;
        this.notificationTag = notificationTag;
        this.triggerAtMillis = triggerAtMillis;
        this.bundle = bundle;
    }

    public DbLocalNotification(int requestId, long triggerAtMillis, Bundle bundle) {
        this.requestId = requestId;
        this.notificationId = 0;
        this.notificationTag = "";
        this.triggerAtMillis = triggerAtMillis;
        this.bundle = bundle;
    }

    public DbLocalNotification() {
        notificationTag ="";
        bundle = new Bundle();
    }

    public void setTriggerAtMillis(long triggerAtMillis) {
        this.triggerAtMillis = triggerAtMillis;
    }

    public long getTriggerAtMillis() {

        return triggerAtMillis;
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setBundle(Bundle bundle) {
        this.bundle = bundle;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    public String getNotificationTag() {
        return notificationTag;
    }

    public void setNotificationTag(String notificationTag) {
        this.notificationTag = notificationTag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DbLocalNotification dbLocalNotification = (DbLocalNotification) o;

        if (requestId != dbLocalNotification.requestId) return false;
        if (notificationId != dbLocalNotification.notificationId) return false;
        if (triggerAtMillis != dbLocalNotification.triggerAtMillis) return false;
        if (notificationTag != null ? !notificationTag.equals(dbLocalNotification.notificationTag) : dbLocalNotification.notificationTag != null)
            return false;
        return bundle != null ? bundle.equals(dbLocalNotification.bundle) : dbLocalNotification.bundle == null;
    }

    @Override
    public int hashCode() {
        int result = requestId;
        result = 31 * result + notificationId;
        result = 31 * result + (notificationTag != null ? notificationTag.hashCode() : 0);
        result = 31 * result + (int) (triggerAtMillis ^ (triggerAtMillis >>> 32));
        result = 31 * result + (bundle != null ? bundle.hashCode() : 0);
        return result;
    }
}
