package com.pushwoosh.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.pushwoosh.exception.GroupIdNotFoundException;
import com.pushwoosh.exception.NotificationIdNotFoundException;
import com.pushwoosh.internal.utils.PWLog;

import java.util.List;

public class SummaryNotificationStorageImpl extends SQLiteOpenHelper implements SummaryNotificationStorage {
    private static final String TAG = SummaryNotificationStorage.class.getSimpleName();
    private static final String DB_NAME = "SummaryNotificationIds.db";
    private static final int VERSION = 1;

    private static final String TABLE_NOTIFICATION_IDS = "summaryNotificationIds";

    private static class Column {
        static final String GROUP_ID = "group_id";
        static final String NOTIFICATION_ID = "pushwoosh_id";
    }

    private final Object mutex = new Object();
    public SummaryNotificationStorageImpl(@Nullable Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createNotificationSummaryIdsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATION_IDS);
        createNotificationSummaryIdsTable(db);
    }

    @Override
    public void put(String groupId, int notificationId) {
        ContentValues cv = new ContentValues();
        cv.put(Column.GROUP_ID, groupId);
        cv.put(Column.NOTIFICATION_ID, notificationId);

        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try {
                    db.beginTransaction();
                    try {
                        long rowId = db.insertWithOnConflict(TABLE_NOTIFICATION_IDS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                        if (rowId == -1) {
                            PWLog.warn(TAG, "Notification IDs pair was not stored.");
                            throw new Exception();
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                } finally {
                    db.close();
                }
            } catch (Exception e) {
                PWLog.error("Error occurred while storing notification IDs", e);
            }
        }
    }

    @Override
    public int remove(String groupId) throws GroupIdNotFoundException {
        int cancelId = getNotificationId(groupId);

        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try {
                    db.beginTransaction();
                    try {
                        int result = db.delete(TABLE_NOTIFICATION_IDS, Column.GROUP_ID + "=" + groupId, null);
                        if (result <= 0) {
                            PWLog.noise(TAG, "failed to remove notification ids pair for id: " + groupId);
                        }
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                } finally {
                    db.close();
                }
            } catch (Exception e) {
                PWLog.error(TAG, "Failed to remove notification ids pair :" + e.getMessage());
            }
        }
        return cancelId;
    }

    @Override
    public void update(List<Pair<String, Integer>> ids) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try {
                    db.beginTransaction();
                    try {
                        db.execSQL("delete from " + TABLE_NOTIFICATION_IDS);
                        db.setTransactionSuccessful();
                    } finally {
                        db.endTransaction();
                    }
                } finally {
                    db.close();
                }
            } catch (Exception e) {
                PWLog.error(TAG, "Failed to update notification storage: " + e.getMessage());
                return;
            }
            if (ids != null) {
                for (Pair<String, Integer> pair: ids) {
                    put(pair.first, pair.second);
                }
            }
        }
    }

    @Override
    public int getNotificationId(String groupId) throws GroupIdNotFoundException {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try {
                    db.beginTransaction();
                    try {
                        String selection = Column.GROUP_ID + " = ?";
                        String[] selectionArgs = {groupId};
                        try (Cursor cursor = db.query(TABLE_NOTIFICATION_IDS, null, selection, selectionArgs, null, null, null)) {
                            if (cursor.moveToFirst()) {
                                db.setTransactionSuccessful();
                                return getStatusBarId(cursor);
                            } else {
                                PWLog.error("Can't get StatusBarNotification with group id: " + groupId);
                                throw new GroupIdNotFoundException("Can't get StatusBarNotification with group id: " + groupId);
                            }
                        }
                    } finally {
                        db.endTransaction();
                    }
                } finally {
                    db.close();
                }
            } catch (GroupIdNotFoundException e) {
                throw e;
            } catch (Exception e) {
                PWLog.error("Can't get StatusBarNotification with group id: " + groupId, e);
                throw new GroupIdNotFoundException("Can't get StatusBarNotification with group id: " + groupId);
            }
        }
    }

    @Override
    public String getGroup(int notificationId) throws NotificationIdNotFoundException {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try {
                    db.beginTransaction();
                    try {
                        String selection = Column.NOTIFICATION_ID + " = ?";
                        String[] selectionArgs = {Integer.toString(notificationId)};
                        try (Cursor cursor = db.query(TABLE_NOTIFICATION_IDS, null, selection, selectionArgs, null, null, null)) {
                            if (cursor.moveToFirst()) {
                                db.setTransactionSuccessful();
                                return getGroupId(cursor);
                            } else {
                                PWLog.error("Can't get group with notification id: " + notificationId);
                                throw new NotificationIdNotFoundException("Can't get group with notification id: " + notificationId);
                            }
                        }
                    } finally {
                        db.endTransaction();
                    }
                } finally {
                    db.close();
                }
            } catch (NotificationIdNotFoundException e) {
                throw e;
            } catch (Exception e) {
                PWLog.error("Can't get group with notification id: " + notificationId, e);
                throw new NotificationIdNotFoundException("Can't get group with notification id: " + notificationId);
            }
        }
    }

    private int getStatusBarId(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndexOrThrow(Column.NOTIFICATION_ID));
    }

    private String getGroupId(Cursor cursor) {
        return cursor.getString(cursor.getColumnIndexOrThrow(Column.GROUP_ID));
    }

    private void createNotificationSummaryIdsTable(SQLiteDatabase db) {
        String createTable = String.format("create table %s (", TABLE_NOTIFICATION_IDS)
                + getStatusBarIdsColumns()
                + ", " + getNotificationIdsColumns()
                + "UNIQUE );";
        db.execSQL(createTable);
    }

    private String getStatusBarIdsColumns() {
        return String.format("%s TEXT ", Column.GROUP_ID);
    }

    private String getNotificationIdsColumns() {
        return String.format("%s INTEGER ", Column.NOTIFICATION_ID);
    }
}
