package com.pushwoosh.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.pushwoosh.exception.NotificationIdNotFoundException;
import com.pushwoosh.internal.utils.PWLog;

import java.util.List;

public class StatusBarNotificationStorageImpl extends SQLiteOpenHelper implements StatusBarNotificationStorage {
    private static final String TAG = StatusBarNotificationStorage.class.getSimpleName();
    private static final String DB_NAME = "StatusBarNotificationIds.db";
    private static final int VERSION = 1;

    private static final String TABLE_NOTIFICATION_IDS = "statusBarIds";

    private final Object mutex = new Object();

    private static class Column {
        static final String STATUS_BAR_ID = "status_bar_id";
        static final String PUSHWOOSH_ID = "pushwoosh_id";
    }

    public StatusBarNotificationStorageImpl(@Nullable Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createNotificationIdsTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATION_IDS);
        createNotificationIdsTable(db);
    }

    @Override
    public void put(long pushwooshId, int statusBarId) {
        ContentValues cv = new ContentValues();
        cv.put(Column.PUSHWOOSH_ID, pushwooshId);
        cv.put(Column.STATUS_BAR_ID, statusBarId);

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
    public int remove(long pushwooshId) throws NotificationIdNotFoundException {
        int cancelId = get(pushwooshId);

        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try {
                    db.beginTransaction();
                    try {
                        int result = db.delete(TABLE_NOTIFICATION_IDS, Column.PUSHWOOSH_ID + "=" + pushwooshId, null);
                        if (result <= 0) {
                            PWLog.noise(TAG, "failed to remove notification ids pair for id: " + pushwooshId);
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
    public void update(List<Pair<Long, Integer>> idsPairs) {
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
            if (idsPairs != null) {
                for (Pair<Long, Integer> pair: idsPairs) {
                    put(pair.first, pair.second);
                }
            }
        }
    }

    @Override
    public int get(long pushwooshId) throws NotificationIdNotFoundException {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try {
                    db.beginTransaction();
                    try {
                        String selection = Column.PUSHWOOSH_ID + " = ?";
                        String[] selectionArgs = {Long.toString(pushwooshId)};
                        try (Cursor cursor = db.query(TABLE_NOTIFICATION_IDS, null, selection, selectionArgs, null, null, null)) {
                            if (cursor.moveToFirst()) {
                                db.setTransactionSuccessful();
                                return getStatusBarId(cursor);
                            } else {
                                PWLog.error("Can't get StatusBarNotification with id: " + pushwooshId);
                                throw new NotificationIdNotFoundException();
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
                PWLog.error("Can't get StatusBarNotification with id: " + pushwooshId, e);
                throw new NotificationIdNotFoundException();
            }
        }
    }

    private int getStatusBarId(Cursor cursor) {
        return cursor.getInt(cursor.getColumnIndex(Column.STATUS_BAR_ID));
    }

    private void createNotificationIdsTable(SQLiteDatabase db) {
        String createTable = String.format("create table %s (", TABLE_NOTIFICATION_IDS)
                + getStatusBarIdColumns()
                + ", " + getPushwooshIdColumns()
                + "UNIQUE );";
        db.execSQL(createTable);
    }

    private String getStatusBarIdColumns() {
        return String.format("%s INTEGER ", Column.STATUS_BAR_ID);
    }

    private String getPushwooshIdColumns() {
        return String.format("%s INTEGER ", Column.PUSHWOOSH_ID);
    }
}
