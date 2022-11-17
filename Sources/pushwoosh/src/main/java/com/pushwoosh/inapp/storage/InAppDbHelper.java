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

package com.pushwoosh.inapp.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.pushwoosh.inapp.network.model.InAppLayout;
import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.utils.DbUtils;
import com.pushwoosh.internal.utils.PWLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InAppDbHelper extends SQLiteOpenHelper implements InAppStorage {

    private static final String TAG = SQLiteOpenHelper.class.getSimpleName();
    private static final String DB_NAME = "inAppDb.db";
    private static final int VERSION = 4;

    private static final String TABLE = "inApps";
    public static final String COMMUNICATION_GDPR_RESOURCE = "Consent";
    public static final String REMOVE_DATA_DEVICE_GDPR_RESOURCE = "Delete";


    private static class Column {
        static final String CODE = "code";
        static final String URL = "url";
        static final String UPDATED = "updated";
        static final String FOLDER = "folder";
        static final String LAYOUT = "layout";
        static final String PRIORITY = "priority";
        static final String REQUIRED = "required";
        static final String BUSINESS_CASE = "businessCase";
        static final String GDPR = "gdpr";
    }

    private final Object mutex = new Object();

    public InAppDbHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //create InAppDTO
        String createInAppsTable =
                String.format("create table %s (", TABLE) +
                        String.format("%s text primary key, ", Column.CODE) +
                        String.format("%s text, ", Column.URL) +
                        String.format("%s text, ", Column.FOLDER) +
                        String.format("%s text, ", Column.LAYOUT) +
                        String.format("%s integer, ", Column.UPDATED) +
                        String.format("%s integer default 0, ", Column.PRIORITY) +
                        String.format("%s integer default 0, ", Column.REQUIRED) +
                        String.format("%s text, ", Column.BUSINESS_CASE) +
                        String.format("%s text", Column.GDPR) +
                        ");";

        db.execSQL(createInAppsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            String alertInAppTableHeader = String.format("ALTER TABLE %s ADD COLUMN ", TABLE);
            if (oldVersion < 2 && newVersion >= 2) {
                db.execSQL(String.format(alertInAppTableHeader + "%s INTEGER DEFAULT 0;", Column.PRIORITY));
                db.execSQL(String.format(alertInAppTableHeader + "%s INTEGER default 0;", Column.REQUIRED));
            }
            if (oldVersion < 3 && newVersion >= 3) {
                db.execSQL(String.format(alertInAppTableHeader + "%s TEXT;", Column.GDPR));
            }
            if (oldVersion < 4 && newVersion >= 4) {
                db.execSQL(String.format(alertInAppTableHeader + "%s TEXT;", Column.BUSINESS_CASE));
            }
        }
    }


    @Override
    public List<String> saveOrUpdateResources(@Nullable List<Resource> inApps) {
        if (inApps == null || inApps.size() == 0) {
            return Collections.emptyList();
        }

        List<String> updatedList = new ArrayList<>();
        synchronized (mutex) {
            try {
                SQLiteDatabase db = getWritableDatabase();
                db.beginTransaction();
                try {
                    for (Resource inApp : inApps) {
                        Resource resource = updateResource(db, inApp);
                        if (resource != null) {
                            updatedList.add(resource.getCode());
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                    db.close();
                }
            } catch (Exception e) {
                PWLog.error("Can't update inApp database", e);
            }
        }

        return updatedList;
    }

    private Resource updateResource(SQLiteDatabase db, Resource inApp) {
        Resource resource = getResource(inApp.getCode(), db);

        if (resource != null && resource.equals(inApp)) {
            return null;
        }

        ContentValues values = new ContentValues();
        values.put(Column.URL, inApp.getUrl());
        values.put(Column.UPDATED, inApp.getUpdated());
        values.put(Column.LAYOUT, inApp.getLayout().getCode());
        values.put(Column.PRIORITY, inApp.getPriority());
        values.put(Column.REQUIRED, inApp.isRequired() ? 1 : 0);
        values.put(Column.BUSINESS_CASE, inApp.getBusinessCase());
        values.put(Column.GDPR, inApp.getGdpr());

        int updated = db.updateWithOnConflict(TABLE, values, Column.CODE + "= ?", new String[]{inApp.getCode()}, SQLiteDatabase.CONFLICT_IGNORE);
        if (updated == 0) {
            values.put(Column.CODE, inApp.getCode());
            long insert = db.insert(TABLE, null, values);

            if (insert == 0L) {
                PWLog.warn("InAppRetrieverWorker", "Not stored " + inApp.getCode());
                return null;
            }
        }

        return resource;
    }

    @Override
    @Nullable
    public Resource getResource(String code) {
        if (code == null || TextUtils.isEmpty(code)) {
            return null;
        }

        synchronized (mutex) {
            try {
                SQLiteDatabase db = getWritableDatabase();
                try {
                    return getResource(code, db);
                } finally {
                    db.close();
                }
            } catch (Exception e) {
                PWLog.error("Can't download resource from db with code: " + code, e);
                return null;
            }
        }
    }

    private Resource getResource(String code, SQLiteDatabase db) {
        String selection = "code = ?";
        String[] selectionArgs = DbUtils.getSelectionArgs(code);
        Cursor cursor = db.query(TABLE, null, selection, selectionArgs, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return getResourceFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }

        return null;
    }

    @Override
    public Resource getResourceGDPRConsent() {
        return getResourceByGDPR(COMMUNICATION_GDPR_RESOURCE);
    }

    @Override
    public Resource getResourceGDPRDeletion() {
        return getResourceByGDPR(REMOVE_DATA_DEVICE_GDPR_RESOURCE);
    }

    @NonNull
    private Resource getResourceByGDPR(String gdpr) {
        synchronized (mutex) {
            try {
                SQLiteDatabase db = getWritableDatabase();
                try {
                    return getResourceByGDPR(gdpr, db);
                } finally {
                    db.close();
                }
            } catch (Exception e) {
                PWLog.error("Can't download resource from db : " + gdpr, e);
                return null;
            }
        }
    }

    private Resource getResourceByGDPR(String gpdr, SQLiteDatabase db) {
        String selection = Column.GDPR + "= ?";
        String[] selectionArgs = DbUtils.getSelectionArgs(gpdr);
        Cursor cursor = db.query(TABLE, null, selection, selectionArgs, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                return getResourceFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }
        return null;
    }


    @NonNull
    private Resource getResourceFromCursor(Cursor cursor) {
        String gpdr;
        gpdr = cursor.getString(cursor.getColumnIndex(Column.CODE));
        String url = cursor.getString(cursor.getColumnIndex(Column.URL));
        long updated = cursor.getLong(cursor.getColumnIndex(Column.UPDATED));
        InAppLayout layout = InAppLayout.of(cursor.getString(cursor.getColumnIndex(Column.LAYOUT)));
        int priority = cursor.getInt(cursor.getColumnIndex(Column.PRIORITY));
        boolean required = cursor.getInt(cursor.getColumnIndex(Column.REQUIRED)) == 1;
        String gdpr = cursor.getString(cursor.getColumnIndex(Column.GDPR));
        String businessCase = cursor.getString(cursor.getColumnIndex(Column.BUSINESS_CASE));
        return new Resource(gpdr, url, "", updated, layout, null, required, priority, businessCase, gdpr);
    }

}
