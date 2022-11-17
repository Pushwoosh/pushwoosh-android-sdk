package com.pushwoosh.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;

import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.util.PushBundleDatabaseEntry;

import java.util.ArrayList;
import java.util.List;

public class PushBundleStorageImpl extends SQLiteOpenHelper implements PushBundleStorage {
    private static final String TAG = PushBundleStorageImpl.class.getSimpleName();
    private static final String DB_NAME = "pushBundleDb.db";
    private static final int VERSION = 4;

    private static final String TABLE_PUSH_BUNDLES = "pushBundles";
    private static final String TABLE_GROUP_PUSH_BUNDLES = "groupPushBundles";

    private final Object mutex = new Object();

    private static class Column {
        static final String ROW_ID = "rowid";
        static final String PUSH_BUNDLE_JSON = "push_bundle_json";
        static final String NOTIFICATION_ID = "notification_id";
    }

    public PushBundleStorageImpl(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createPushBundlesTable(db);
        createGroupPushBundlesTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PUSH_BUNDLES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROUP_PUSH_BUNDLES);
        createPushBundlesTable(db);
        createGroupPushBundlesTable(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.setVersion(oldVersion);
    }

    private void createPushBundlesTable(SQLiteDatabase db) {
        String createTable = String.format("create table %s (", TABLE_PUSH_BUNDLES)
                + getPushBundlesColumns() + ");";
        db.execSQL(createTable);
    }

    private void createGroupPushBundlesTable(SQLiteDatabase db) {
        String createTable = String.format("create table %s (", TABLE_GROUP_PUSH_BUNDLES)
                + getPushBundlesColumns()
                + ", " + getNotificationIdColumn()
                + ");";
        db.execSQL(createTable);
    }

    private String getPushBundlesColumns() {
        return String.format("%s TEXT ", Column.PUSH_BUNDLE_JSON);
    }

    private String getNotificationIdColumn() {
        return String.format("%s INTEGER ", Column.NOTIFICATION_ID);
    }

    @Override
    public long putPushBundle(Bundle pushBundle) throws Exception {
        ContentValues cv = getPushBundleContentValues(pushBundle);
        return put(cv, TABLE_PUSH_BUNDLES);
    }

    @Override
    public Bundle getPushBundle(long id) throws Exception {
        return get(id, TABLE_PUSH_BUNDLES);
    }

    @Override
    public void removePushBundle(long id) {
        remove(id, TABLE_PUSH_BUNDLES);
    }

    @Override
    public long putGroupPushBundle(Bundle pushBundle, int id) throws Exception {
        ContentValues cv = getGroupPushBundleContentValues(pushBundle, id);
        return put(cv, TABLE_GROUP_PUSH_BUNDLES);
    }

    @Override
    public List<Bundle> getGroupPushBundles() {
        return getAll(TABLE_GROUP_PUSH_BUNDLES);
    }

    @Override
    public void removeGroupPushBundle(long id) {
        remove(id, TABLE_GROUP_PUSH_BUNDLES);
    }

    @Override
    public void removeGroupPushBundles() {
        removeAll(TABLE_GROUP_PUSH_BUNDLES);
    }

    private long put(ContentValues cv, String tableName) throws Exception {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                long rowId = db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                if (rowId == -1) {
                    PWLog.warn(TAG, "Push bundle with message was not stored.");
                    throw new Exception();
                }
                return rowId;
            } catch (Exception e) {
                PWLog.error("Error occurred while storing push bundle", e);
                throw e;
            }
        }
    }

    private Bundle get(long id, String tableName) throws Exception {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                String selection = Column.ROW_ID + " = ?";
                String[] selectionArgs = { Long.toString(id) };
                try (Cursor cursor = db.query(tableName, null, selection, selectionArgs, null, null, null)) {
                    if (cursor.moveToFirst()) {
                        return getBundle(cursor);
                    } else {
                        PWLog.error("Can't get push bundle with id: " + id);
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                PWLog.error("Can't get push bundle with id: " + id, e);
                throw e;
            }
        }
    }

    private List<Bundle> getAll(String tableName) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try (Cursor cursor = db.query(tableName, null, null, null, null, null, null)) {
                    List<Bundle> bundles = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        bundles.add(getBundle(cursor));
                    }
                    return bundles;
                }
            } catch (Exception e) {
                PWLog.error("Can't get group push bundles", e);
                throw e;
            }
        }
    }

    @Override
    public PushBundleDatabaseEntry getLastPushBundleEntry() throws Exception {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                String[] columns = {Column.NOTIFICATION_ID, Column.PUSH_BUNDLE_JSON, Column.ROW_ID};
                try (Cursor cursor = db.query(TABLE_GROUP_PUSH_BUNDLES, columns, null , null,null,null,null)) {
                    if (cursor.moveToLast()) {
                        return new PushBundleDatabaseEntry(cursor.getInt(cursor.getColumnIndex(Column.NOTIFICATION_ID)),
                                cursor.getLong(cursor.getColumnIndex(Column.ROW_ID)),
                                getBundle(cursor));
                    } else {
                        throw new Exception();
                    }
                }
            } catch (Exception e) {
                PWLog.error("Failed to obtain the last status bar notification", e);
                throw e;
            }
        }
    }

    private void remove(long id, String tableName) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                int result = db.delete(tableName, Column.ROW_ID + "=" + id, null);
                if (result <= 0) {
                    PWLog.noise(TAG, "failed to remove push bundle with id: " + id);
                }
            }
        }
    }

    private void removeAll(String tableName) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                int result = db.delete(tableName, null, null);
                if (result <= 0) {
                    PWLog.noise(TAG, "failed to remove group push bundles");
                }
            }
        }
    }

    private ContentValues getPushBundleContentValues(Bundle pushBundle) {
        ContentValues cv = new ContentValues();
        cv.put(Column.PUSH_BUNDLE_JSON, JsonUtils.bundleToJson(pushBundle).toString());
        return cv;
    }

    private ContentValues getGroupPushBundleContentValues(Bundle pushBundle, int id) {
        ContentValues cv = new ContentValues();
        cv.put(Column.PUSH_BUNDLE_JSON, JsonUtils.bundleToJson(pushBundle).toString());
        cv.put(Column.NOTIFICATION_ID, id);
        return cv;
    }

    private Bundle getBundle(Cursor c) {
        String pushBundleJson = c.getString(c.getColumnIndex(Column.PUSH_BUNDLE_JSON));
        return JsonUtils.jsonStringToBundle(pushBundleJson);
    }
}