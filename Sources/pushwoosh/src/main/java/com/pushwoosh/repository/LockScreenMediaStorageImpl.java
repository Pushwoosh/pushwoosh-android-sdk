package com.pushwoosh.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.text.TextUtils;

import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.notification.PushMessage;

import java.util.ArrayList;
import java.util.List;

public class LockScreenMediaStorageImpl extends SQLiteOpenHelper implements LockScreenMediaStorage {
    private static final String TAG = DbLocalNotification.class.getSimpleName();
    private static final String DB_NAME = "lockScreenRichMediaResources.db";
    private static final int VERSION = 2;

    private static final String TABLE_LOCK_SCREEN_RESOURCES = "lockScreenResources";
    private static final String TABLE_LOCK_SCREEN_REMOTE_URLS = "lockScreenRemoteUrls";

    private static class Column {
        static final String RICH_MEDIA = "richMedia";
        static final String SOUND = "sound";
        static final String REMOTE_URL = "remoteUrl";
    }

    private final Object mutex = new Object();

    public LockScreenMediaStorageImpl(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createLockScreenResourcesTable(db);
        createLockScreenRemoteUrlsTable(db);
    }

    private void createLockScreenResourcesTable(SQLiteDatabase db) {
        String createLocalNotificationTable =
                String.format("create table %s (", TABLE_LOCK_SCREEN_RESOURCES) +
                        String.format("%s TEXT , ", Column.RICH_MEDIA) +
                        String.format("%s TEXT ", Column.SOUND) + ");";
        db.execSQL(createLocalNotificationTable);
    }

    private void createLockScreenRemoteUrlsTable(SQLiteDatabase db) {
        String createRequestIdTable =
                String.format("create table %s (", TABLE_LOCK_SCREEN_REMOTE_URLS) +
                        String.format("%s TEXT ", Column.REMOTE_URL) + ");";
        db.execSQL(createRequestIdTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCK_SCREEN_RESOURCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LOCK_SCREEN_REMOTE_URLS);
        createLockScreenResourcesTable(db);
        createLockScreenRemoteUrlsTable(db);
    }

    @Override
    public void cacheResource(PushMessage pushMessage) {
        synchronized (mutex) {
            String richMedia = PushBundleDataProvider.getRichMedia(pushMessage.toBundle());
            String sound = pushMessage.getSound();
            try (SQLiteDatabase db = getWritableDatabase()) {
                ContentValues cv = getResourceContentValues(richMedia, sound);
                long insert = db.insertWithOnConflict(TABLE_LOCK_SCREEN_RESOURCES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                if (insert == -1) {
                    PWLog.warn(TAG, "Rich media " + richMedia + " was not stored.");
                }
            } catch (Exception e) {
                PWLog.error("Can't cache richMedia resource: " + richMedia, e);
            }
        }
    }

    @Override
    public List<ResourceWrapper> getCachedResourcesList() {
        List<ResourceWrapper> resourceWrapperList = new ArrayList<>();
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try (Cursor cursor = db.query(TABLE_LOCK_SCREEN_RESOURCES, null, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        resourceWrapperList.add(getResourceWrapper(cursor));
                    }
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                PWLog.error("Can't get cached resources: ", e);
            }
        }

        return resourceWrapperList;
    }

    @Override
    public void clearResources() {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                db.execSQL("delete from "+ TABLE_LOCK_SCREEN_RESOURCES);
            } catch (Exception e) {
                PWLog.error("Can't clear resources", e);
            }
        }
    }

    @Override
    public void cacheRemoteUrl(Uri url) {
        synchronized (mutex) {
            String urlString = url.toString();
            if (TextUtils.isEmpty(urlString)) {
                PWLog.warn(TAG, "Remote url is empty.");
                return;
            }
            try (SQLiteDatabase db = getWritableDatabase()) {
                ContentValues cv = getRemoteUrlContentValues(urlString);
                long insert = db.insertWithOnConflict(TABLE_LOCK_SCREEN_REMOTE_URLS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                if (insert == -1) {
                    PWLog.warn(TAG, "Remote url " + urlString + " was not stored.");
                }
            } catch (Exception e) {
                PWLog.error("Can't cache remote url: " + urlString, e);
            }
        }
    }

    @Override
    public List<Uri> getCachedRemoteUrls() {
        List<Uri> remoteUrls = new ArrayList<>();
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try (Cursor cursor = db.query(TABLE_LOCK_SCREEN_REMOTE_URLS, null, null, null, null, null, null)) {
                    while (cursor.moveToNext()) {
                        remoteUrls.add(getRemoteUrl(cursor));
                    }
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                PWLog.error("Can't get cached resources: ", e);
            }
        }

        return remoteUrls;
    }

    @Override
    public void clearRemoteUrls() {
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                db.execSQL("delete from "+ TABLE_LOCK_SCREEN_REMOTE_URLS);
            } catch (Exception e) {
                PWLog.error("Can't clear remote urls", e);
            }
        }
    }

    private ContentValues getResourceContentValues(String richMedia, String sound) {
        ContentValues values = new ContentValues();
        values.put(Column.RICH_MEDIA, richMedia);
        values.put(Column.SOUND, sound);

        return values;
    }

    private ContentValues getRemoteUrlContentValues(String url) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(Column.REMOTE_URL, url);

        return contentValues;
    }

    private ResourceWrapper getResourceWrapper(Cursor cursor) {
        String richMedia = cursor.getString(cursor.getColumnIndex(Column.RICH_MEDIA));
        String sound = cursor.getString(cursor.getColumnIndex(Column.SOUND));

        return new ResourceWrapper.Builder()
                .setRichMedia(richMedia)
                .setSound(sound)
                .setLockScreen(true)
                .build();
    }

    private Uri getRemoteUrl(Cursor cursor) {
        String urlString = cursor.getString(cursor.getColumnIndex(Column.REMOTE_URL));

        return Uri.parse(urlString);
    }
}
