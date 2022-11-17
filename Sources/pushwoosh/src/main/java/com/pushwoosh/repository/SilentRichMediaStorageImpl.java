package com.pushwoosh.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.pushwoosh.inapp.view.strategy.model.ResourceWrapper;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.notification.PushMessage;

public class SilentRichMediaStorageImpl extends SQLiteOpenHelper implements SilentRichMediaStorage {
    private static final String TAG = SilentRichMediaStorageImpl.class.getSimpleName();
    private static final String DB_NAME = "silentRichMediaStorage.db";
    private static final int VERSION = 2;

    private static final String TABLE_RESOURCES = "resources";

    private static class Column {
        static final String RICH_MEDIA = "richMedia";
        static final String SOUND = "sound";
    }

    private final Object mutex = new Object();

    public SilentRichMediaStorageImpl(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createResourcesTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESOURCES);
        createResourcesTable(db);
    }

    private void createResourcesTable(SQLiteDatabase db) {
        String createLocalNotificationTable =
                String.format("create table %s (", TABLE_RESOURCES) +
                        String.format("%s TEXT , ", Column.RICH_MEDIA) +
                        String.format("%s TEXT ", Column.SOUND) + ");";
        db.execSQL(createLocalNotificationTable);
    }

    @Override
    public void replaceResource(PushMessage pushMessage) {
        synchronized (mutex) {
            String richMedia = PushBundleDataProvider.getRichMedia(pushMessage.toBundle());
            String sound = pushMessage.getSound();
            try (SQLiteDatabase db = getWritableDatabase()) {
                ContentValues cv = getResourceContentValues(richMedia, sound);
                long insert = db.insertWithOnConflict(TABLE_RESOURCES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                if (insert == -1) {
                    PWLog.warn(TAG, "Rich media " + richMedia + " was not stored.");
                }
            } catch (Exception e) {
                PWLog.error("Can't cache richMedia resource: " + richMedia, e);
            }
        }
    }

    @Nullable
    @Override
    public ResourceWrapper getResourceWrapper() {
        ResourceWrapper resourceWrapper = null;
        synchronized (mutex) {
            try (SQLiteDatabase db = getWritableDatabase()) {
                try (Cursor cursor = db.query(TABLE_RESOURCES, null, null, null, null, null, null)) {
                    if (cursor.moveToLast()) {
                        resourceWrapper = getResourceWrapper(cursor);
                    }
                    if (!cursor.isClosed()) {
                        cursor.close();
                    }
                }
                db.execSQL("delete from "+ TABLE_RESOURCES);
            } catch (Exception e) {
                PWLog.error("Can't get cached resources: ", e);
            }
        }

        return resourceWrapper;
    }

    private ContentValues getResourceContentValues(String richMedia, String sound) {
        ContentValues values = new ContentValues();
        values.put(Column.RICH_MEDIA, richMedia);
        values.put(Column.SOUND, sound);

        return values;
    }

    private ResourceWrapper getResourceWrapper(Cursor cursor) {
        String richMedia = cursor.getString(cursor.getColumnIndex(Column.RICH_MEDIA));
        String sound = cursor.getString(cursor.getColumnIndex(Column.SOUND));

        return new ResourceWrapper.Builder()
                .setRichMedia(richMedia)
                .setSound(sound)
                .setLockScreen(false)
                .build();
    }
}
