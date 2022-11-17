package com.pushwoosh.internal.network;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.utils.DbUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.UUIDFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRequestStorage extends SQLiteOpenHelper {
    protected final UUIDFactory uuidFactory;
    private final Object mutex = new Object();
    private final String tableName;
    private final String tag;

    protected static class Column {
        static final String REQUEST_ID = "requestId";
        static final String METHOD = "method";
        static final String BODY = "body";
    }

    public BaseRequestStorage(Context context, UUIDFactory uuidFactory, String dbName, int version, String tableName, String tag) {
        super(context, dbName, null, version);
        this.uuidFactory = uuidFactory;
        this.tableName = tableName;
        this.tag = tag;
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                String.format("create table %s (%s TEXT primary key, %s TEXT, %s TEXT)",
                        tableName, Column.REQUEST_ID, Column.METHOD, Column.BODY));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public long add(PushRequest<?> request) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                ContentValues contentValues = createContentValues(request);
                return db.insert(tableName, null, contentValues);
            } catch (Exception e) {
                PWLog.error(tag, "error add request", e);
                return -1;
            }
        }
    }

    @NonNull
    private ContentValues createContentValues(PushRequest<?> requestMock) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Column.REQUEST_ID, uuidFactory.createUUID());
        contentValues.put(Column.METHOD, requestMock.getMethod());
        try {
            contentValues.put(Column.BODY, requestMock.getParams().toString());
        } catch (JSONException e) {
            PWLog.error(tag, "not valid body request:", e);
        } catch (InterruptedException e) {
            PWLog.error(tag, "not valid body request:", e);
        }
        return contentValues;
    }

    public CachedRequest get(long rowId) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                String selection = "rowid = ?";
                String[] selectionArgs = DbUtils.getSelectionArgs(String.valueOf(rowId));
                try (Cursor cursor = db.query(tableName, null, selection, selectionArgs, null, null, null)) {
                    if (cursor.moveToFirst()) {
                        return getCachedRequest(cursor);
                    }
                }
            } catch (Exception e) {
                PWLog.error(tag, "Can't get cached request: ", e);
            }
            return null;
        }
    }

    public List<CachedRequest> getAll() {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try (Cursor cursor = db.query(tableName, null, null, null, null, null, null)) {
                    List<CachedRequest> cachedRequests = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        cachedRequests.add(getCachedRequest(cursor));
                    }
                    return cachedRequests;
                }
            } catch (Exception e) {
                PWLog.error("Can't get all cached requests", e);
                throw e;
            }
        }
    }

    private CachedRequest getCachedRequest(Cursor cursor) {
        String key = cursor.getString(cursor.getColumnIndex(Column.REQUEST_ID));
        String method = cursor.getString(cursor.getColumnIndex(Column.METHOD));
        String body = cursor.getString(cursor.getColumnIndex(Column.BODY));
        JSONObject jsonBody = new JSONObject();
        try {
            if (body != null) {
                jsonBody = new JSONObject(body);
            }
        } catch (JSONException e) {
            PWLog.error("Can't parse body of request: ", e);
        }
        return new CachedRequest(key, method, jsonBody);
    }

    public void remove(String key) {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                db.delete(tableName, Column.REQUEST_ID + "=?", new String[]{key});
            } catch (Exception e) {
                PWLog.error(tag, String.format("Can't remove cached request by key %s: ", key), e);
            }
        }
    }

    public void clear() {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                db.execSQL("delete from "+ tableName);
            }
        }
    }
}
