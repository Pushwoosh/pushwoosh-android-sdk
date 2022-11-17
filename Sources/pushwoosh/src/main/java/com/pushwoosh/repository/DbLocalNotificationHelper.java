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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.pushwoosh.internal.utils.DbUtils;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by aevstefeev on 20/03/2018.
 */

public class DbLocalNotificationHelper extends SQLiteOpenHelper {
    private static final String TAG = DbLocalNotification.class.getSimpleName();
    private static final String DB_NAME = "localNotification.db";
    private static final int VERSION = 2;

    private static final String TABLE_LOCAL_NOTIFICATION = "localNotification";
    private static final String TABLE_LOCAL_NOTIFICATION_SHOWN = "localNotificationShown";
    private static final String TABLE_NEXT_REQUEST_ID = "nextRequestId";

    private static final String VALUE_REQUEST_ID = "value";

    private static class Column {
        static final String REQUEST_ID = "requestId";
        static final String NOTIFICATION_ID = "notificationId";
        static final String NOTIFICATION_TAG = "notificationTag";
        static final String TRIGGER_AT_MILLIS = "triggerAtMilles";
        static final String BUNDLE_CONTENT = "bundle";
    }

    private final Object mutex = new Object();

    public interface EnumeratorLocalNotification {
        void enumerate(DbLocalNotification localNotification);
    }
    
    public DbLocalNotificationHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        creataRequestIdTable(db);
        createLocalNotificationTable(db, TABLE_LOCAL_NOTIFICATION);
        createLocalNotificationTable(db, TABLE_LOCAL_NOTIFICATION_SHOWN);
    }

    private void creataRequestIdTable(SQLiteDatabase db) {
        String createRequestIdTable =
                String.format("create table %s (", TABLE_NEXT_REQUEST_ID) +
                        String.format("%s INTEGER primary key ", VALUE_REQUEST_ID) + ");";
        db.execSQL(createRequestIdTable);
    }

    private void createLocalNotificationTable(SQLiteDatabase db, String table) {
        String createLocalNotificationTable =
                String.format("create table %s (", table) +
                        String.format("%s INTEGER primary key, ", Column.REQUEST_ID) +
                        String.format("%s INTEGER, ", Column.NOTIFICATION_ID) +
                        String.format("%s INTEGER, ", Column.TRIGGER_AT_MILLIS) +
                        String.format("%s TEXT, ", Column.NOTIFICATION_TAG) +
                        String.format("%s TEXT ", Column.BUNDLE_CONTENT) + ");";
        db.execSQL(createLocalNotificationTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public int nextRequestId() {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                int nextId = getNextId(db);
                saveNextId(nextId + 1, db);
                return nextId;
            } catch (Exception e) {
                PWLog.error("Can't set next RequestId", e);
                return 0;
            }
        }
    }

    private int getNextId(SQLiteDatabase db) {
        try (Cursor cursor = db.query(TABLE_NEXT_REQUEST_ID, null, null, null, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndex(VALUE_REQUEST_ID));
            }
        }
        PWLog.noise(TAG, "nextId is empty, return 0");
        return 0;
    }

    private void saveNextId(int nextId, SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(VALUE_REQUEST_ID, nextId);
        int updated = db.updateWithOnConflict(TABLE_NEXT_REQUEST_ID, values, null, null, SQLiteDatabase.CONFLICT_IGNORE);
        if (updated == 0) {
            long insert = db.insert(TABLE_NEXT_REQUEST_ID, null, values);
            if (insert == 0L) {
                PWLog.warn("saveNextId", "Not stored ");
            }
        }
    }

    public void setNextRequestId(int nextId) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                nextId++;
                saveNextId(nextId, db);
            } catch (Exception e) {
                PWLog.error("Can't set next RequestId", e);
            }
        }
    }

    public DbLocalNotification getLocalNotification(String requestId) {
        return getDbLocalNotificationInternal(requestId, TABLE_LOCAL_NOTIFICATION);
    }

    public DbLocalNotification getDbLocalNotificationShown(String requestId) {
        return getDbLocalNotificationInternal(requestId, TABLE_LOCAL_NOTIFICATION_SHOWN);
    }

    private DbLocalNotification getDbLocalNotificationInternal(String requestId, String table) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                return getDbLocalNotificationInternal(requestId, table, db);
            } catch (Exception e) {
                PWLog.error("Can't get notification from db with requestId: " + requestId, e);
                return new DbLocalNotification();
            }
        }
    }

    private DbLocalNotification getDbLocalNotificationInternal(String requestId, String table, SQLiteDatabase db) {
        String selection = Column.REQUEST_ID + " = ?";
        String[] selectionArgs = DbUtils.getSelectionArgs(requestId);
        try (Cursor cursor = db.query(table, null, selection, selectionArgs, null, null, null)) {
            if (cursor.moveToFirst()) {
                return getDbLocalNotification(cursor);
            }
        }
        PWLog.noise(TAG, "cant find local notification in table " + table + " by id " + requestId);
        return null;
    }

    @NonNull
    private DbLocalNotification getDbLocalNotification(Cursor cursor) {
        int requesId = cursor.getInt(cursor.getColumnIndex(Column.REQUEST_ID));
        int notificationId = cursor.getInt(cursor.getColumnIndex(Column.NOTIFICATION_ID));
        long triggerAtMillis = cursor.getLong(cursor.getColumnIndex(Column.TRIGGER_AT_MILLIS));
        String tag = cursor.getString(cursor.getColumnIndex(Column.NOTIFICATION_TAG));
        String bundleContent = cursor.getString(cursor.getColumnIndex(Column.BUNDLE_CONTENT));
        //todo make inject JsonUtils
        Bundle bundle = JsonUtils.jsonStringToBundle(bundleContent);
        return new DbLocalNotification(requesId, notificationId, tag, triggerAtMillis, bundle);
    }

    public Set<Integer> getAllRequestIds() {
        synchronized (mutex) {
            try {
                try (SQLiteDatabase db = getWritableDatabase()) {
                    return getAllRequestInternal(db);
                }
            } catch (Exception e) {
                PWLog.error("Can't get all request ids", e);
                return new HashSet<>();
            }
        }
    }

    @NonNull
    private Set<Integer> getAllRequestInternal(SQLiteDatabase db) {
        Set<Integer> idsSet = new HashSet<>();
        String[] columns = new String[]{Column.REQUEST_ID};
        try (Cursor cursor = db.query(TABLE_LOCAL_NOTIFICATION, columns, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                int notificationId = cursor.getInt(cursor.getColumnIndex(Column.REQUEST_ID));
                idsSet.add(notificationId);
            }
            return idsSet;
        }
    }

    public void putDbLocalNotification(DbLocalNotification dbLocalNotification) {
        int requestId = dbLocalNotification.getRequestId();
        ContentValues values = createContentValues(dbLocalNotification);
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                insertInDb(requestId, values, db, TABLE_LOCAL_NOTIFICATION);
            } catch (Exception e) {
                PWLog.error("Can't update preference value:" + requestId, e);
            }
        }
    }

    public void addDbLocalNotificationShown(DbLocalNotification dbLocalNotification) {
        ContentValues values = createContentValues(dbLocalNotification);
        synchronized (mutex) {
            int requestId = dbLocalNotification.getRequestId();
            try (SQLiteDatabase db = getWritableDatabase()) {
                insertInDb(requestId, values, db, TABLE_LOCAL_NOTIFICATION_SHOWN);
                long count = getNotificationShownCount(db);
                if (count > 10) {
                    removeFirstDbLocalNotification(db);
                }
            } catch (Exception e) {
                PWLog.error("Can't update preference value:" + requestId, e);
            }
        }
    }

    private void insertInDb(int requestId, ContentValues values, SQLiteDatabase db, String table) {
        String requestIdString = Integer.toString(requestId);
        int updated = db.updateWithOnConflict(table, values, Column.REQUEST_ID + "= ?", new String[]{requestIdString}, SQLiteDatabase.CONFLICT_IGNORE);
        if (updated == 0) {
            long insert = db.insert(table, null, values);
            if (insert == 0L) {
                PWLog.warn("notification", "Not stored " + requestIdString);
            }
        }
    }

    @NonNull
    private ContentValues createContentValues(DbLocalNotification dbLocalNotification) {
        ContentValues values = new ContentValues();
        values.put(Column.REQUEST_ID, dbLocalNotification.getRequestId());
        values.put(Column.NOTIFICATION_ID, dbLocalNotification.getNotificationId());
        values.put(Column.NOTIFICATION_TAG, dbLocalNotification.getNotificationTag());
        values.put(Column.TRIGGER_AT_MILLIS, dbLocalNotification.getTriggerAtMillis());

        JSONObject jsonObject = JsonUtils.bundleToJson(dbLocalNotification.getBundle());
        values.put(Column.BUNDLE_CONTENT, jsonObject.toString());
        return values;
    }


    private long getNotificationShownCount(SQLiteDatabase db) {
        return DatabaseUtils.queryNumEntries(db, TABLE_LOCAL_NOTIFICATION_SHOWN);
    }

    private void removeFirstDbLocalNotification(SQLiteDatabase db) {
        Cursor cursor = db.query(TABLE_LOCAL_NOTIFICATION_SHOWN, null, null, null, null, null, Column.REQUEST_ID);

        if (cursor.moveToFirst()) {
            String rowId = cursor.getString(cursor.getColumnIndex(Column.REQUEST_ID));
            db.delete(TABLE_LOCAL_NOTIFICATION_SHOWN, Column.REQUEST_ID + "=?", new String[]{rowId});
        }
    }

    public void removeDbLocalNotification(int requestId) {
        removeDbLocalNotificationInternal(requestId, TABLE_LOCAL_NOTIFICATION);
    }

    public void removeDbLocalNotificationShown(int id) {
        removeDbLocalNotificationInternal(id, TABLE_LOCAL_NOTIFICATION_SHOWN);
    }

    private void removeDbLocalNotificationInternal(int requestId, String table) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                int result = db.delete(table, Column.REQUEST_ID + "=" + requestId, null);
                PWLog.debug("delete notification " + result + " by requestID:" + requestId);

                if (result > 0) {
                    PWLog.noise(TAG, "success remove local notification by " + requestId);
                } else {
                    PWLog.noise(TAG, "fail remove local notification by " + requestId);
                }
            }
        }
    }

    public void enumerateDbLocalNotificationList(EnumeratorLocalNotification enumeratorLocalNotification) {
        enumerateDbLocalNotificationListInternal(TABLE_LOCAL_NOTIFICATION, enumeratorLocalNotification);
    }

    public void enumerateDbLocalNotificationShownList(EnumeratorLocalNotification enumeratorLocalNotification) {
        enumerateDbLocalNotificationListInternal(TABLE_LOCAL_NOTIFICATION_SHOWN, enumeratorLocalNotification);
    }

    private void enumerateDbLocalNotificationListInternal(String table, EnumeratorLocalNotification enumeratorLocalNotification) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try (Cursor cursor = db.query(table, null, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        DbLocalNotification dbLocalNotification = getDbLocalNotification(cursor);
                        enumeratorLocalNotification.enumerate(dbLocalNotification);
                    }
                }
            } catch (Exception e) {
                PWLog.error("Can't get NotificationList: ", e);
            }
        }
    }

    public DbLocalNotification getDbLocalNotificationShown(int notificationId, String notificationTag) {
        String notificationIdString = Integer.toString(notificationId);
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                String selection = Column.NOTIFICATION_ID + " = ? AND " + Column.NOTIFICATION_TAG + " = ?";
                String[] selectionArgs = DbUtils.getSelectionArgs(notificationIdString, notificationTag);
                try (Cursor cursor = db.query(TABLE_LOCAL_NOTIFICATION_SHOWN, null, selection, selectionArgs, null, null, null)) {
                    while (cursor.moveToNext()) {
                        return getDbLocalNotification(cursor);
                    }
                }
            } catch (Exception e) {
                PWLog.error("Can't get Notification: ", e);
            }
        }
        return null;
    }
}
