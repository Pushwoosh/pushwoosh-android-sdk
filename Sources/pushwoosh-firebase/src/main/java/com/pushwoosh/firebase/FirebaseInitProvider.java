package com.pushwoosh.firebase;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.firebase.internal.specific.FcmDeviceSpecificIniter;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.security.CallingPackageChecker;

public class FirebaseInitProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        try {
            FirebaseInitializer.init(getContext());
        } catch (Exception e) {
            PWLog.error("FirebaseInitProvider", "Failed to initialize", e);
        }
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        CallingPackageChecker.checkCallingPackage(this);
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
