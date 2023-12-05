package com.pushwoosh.xiaomi;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.security.CallingPackageChecker;

public class XiaomiInitProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        PWLog.debug("Xiaomi init");
        XiaomiInitializer.init(getContext());
        return true;
    }
    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        CallingPackageChecker.checkCallingPackage(this);
        return null;
    }
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) { return 0; }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) { return 0; }
}
