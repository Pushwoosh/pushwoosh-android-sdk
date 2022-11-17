package com.pushwoosh.repository.util;

import android.os.Bundle;

public class PushBundleDatabaseEntry {
    private final int notificationId;
    private final long rowId;
    private final Bundle pushBundle;


    public PushBundleDatabaseEntry(int notificationId, long rowId, Bundle pushBundle) {
        this.notificationId = notificationId;
        this.rowId = rowId;
        this.pushBundle = pushBundle;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public long getRowId() {
        return rowId;
    }

    public Bundle getPushBundle() {
        return pushBundle;
    }
}
