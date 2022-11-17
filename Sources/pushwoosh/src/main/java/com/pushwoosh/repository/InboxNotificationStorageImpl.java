package com.pushwoosh.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.DbUtils;
import com.pushwoosh.internal.utils.PWLog;

public class InboxNotificationStorageImpl extends SQLiteOpenHelper implements InboxNotificationStorage {
    private static final String TAG = InboxNotificationStorageImpl.class.getSimpleName();
    private static final String DB_NAME = "inboxNotificationDb.db";
    private static final int VERSION = 2;

    private static final String TABLE_INBOX_NOTIFICATIONS = "inboxNotifications";

    private final Object mutex = new Object();

    private static class Column {
        static final String INBOX_MESSAGE_ID = "inbox_message_id";
        static final String NOTIFICATION_ID = "notification_id";
        static final String NOTIFICATION_TAG = "notification_tag";
    }

    public InboxNotificationStorageImpl(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db, TABLE_INBOX_NOTIFICATIONS);
    }

    private void createTable(SQLiteDatabase db, String tableName) {
        String createTable = String.format("create table %s (", tableName) +
                String.format("%s TEXT, ", Column.INBOX_MESSAGE_ID) +
                String.format("%s TEXT, ", Column.NOTIFICATION_TAG) +
                String.format("%s INTEGER ", Column.NOTIFICATION_ID) + ");";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        upgradeTable(db, TABLE_INBOX_NOTIFICATIONS);
    }

    private void upgradeTable(SQLiteDatabase db, String tableName) {
        db.execSQL("DROP TABLE IF EXISTS " + tableName);
        createTable(db, tableName);
    }

    @Override
    public void putNotificationIdAndTag(String inboxMessageId, int notificationId, String notificationTag) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                ContentValues cv = getContentValues(inboxMessageId, notificationId, notificationTag);
                long rowId = db.insertWithOnConflict(TABLE_INBOX_NOTIFICATIONS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                if (rowId == -1) {
                    PWLog.warn(TAG, "Notification with inboxMessageId: " + inboxMessageId + " was not stored.");
                }
            } catch (Exception e) {
                PWLog.error(TAG, "Error occurred while storing notification id and notification tag", e);
            }
        }
    }

    @Override
    @Nullable
    public Integer getNotificationId(String inboxMessageId) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                String selection = Column.INBOX_MESSAGE_ID + " = ?";
                String[] selectionArgs = DbUtils.getSelectionArgs(inboxMessageId);
                try (Cursor cursor = db.query(TABLE_INBOX_NOTIFICATIONS, null, selection, selectionArgs, null, null, null)) {
                    if (cursor.moveToFirst()) {
                        return getNotificationId(cursor);
                    } else {
                        PWLog.error(TAG, "Can't find InboxMessage with id: " + inboxMessageId);
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                PWLog.error(TAG, "Can't get NotificationId for InboxMessage with id: " + inboxMessageId, e);
            }
            return null;
        }
    }

    @Nullable
    @Override
    public String getNotificationTag(String inboxMessageId) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                String selection = Column.INBOX_MESSAGE_ID + " = ?";
                String[] selectionArgs = DbUtils.getSelectionArgs(inboxMessageId);
                try (Cursor cursor = db.query(TABLE_INBOX_NOTIFICATIONS, null, selection, selectionArgs, null, null, null)) {
                    if (cursor.moveToFirst()) {
                        return getNotificationTag(cursor);
                    } else {
                        PWLog.error(TAG, "Can't find InboxMessage with id: " + inboxMessageId);
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                PWLog.error(TAG, "Can't get NotificationTag for InboxMessage with id: " + inboxMessageId, e);
            }
            return null;
        }
    }

    private ContentValues getContentValues(String inboxMessageId, int notificationId, String notificationTag) {
        ContentValues cv = new ContentValues();
        cv.put(Column.INBOX_MESSAGE_ID, inboxMessageId);
        cv.put(Column.NOTIFICATION_ID, notificationId);
        cv.put(Column.NOTIFICATION_TAG, notificationTag);
        return cv;
    }

    private int getNotificationId(Cursor c) {
        return c.getInt(c.getColumnIndex(Column.NOTIFICATION_ID));
    }

    private String getNotificationTag(Cursor c) {
        return c.getString(c.getColumnIndex(Column.NOTIFICATION_TAG));
    }
}
