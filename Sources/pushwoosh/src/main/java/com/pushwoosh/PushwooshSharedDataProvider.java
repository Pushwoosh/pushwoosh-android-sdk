package com.pushwoosh;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.utils.DeviceUtils;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.utils.security.CallingPackageChecker;

public class PushwooshSharedDataProvider extends ContentProvider {
    public static final String HWID_PATH = "hwid";
    public static final String HWID_COLUMN_NAME = "hwid";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (getContext() == null)
            return null;

        String[] trustedPackageNames = PushwooshPlatform.getInstance().getConfig().getTrustedPackageNames();
        CallingPackageChecker.checkIfTrustedPackage(this, trustedPackageNames);

        final int code = 1;
        UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(getContext().getApplicationContext().getPackageName() + "." + getClass().getSimpleName(), HWID_PATH, code);
        if (uriMatcher.match(uri) == code) {
            MatrixCursor matrixCursor = new MatrixCursor(new String[]{HWID_COLUMN_NAME});
            if (sortOrder != null) {
                if (sortOrder.compareTo(GeneralUtils.md5(getContext().getApplicationContext().getPackageName())) < 0) {
                    matrixCursor.addRow(new String[]{DeviceUtils.getDeviceUUID()});
                } else {
                    matrixCursor.addRow(new String[]{DeviceUtils.getDeviceUUIDOrNull()});
                }
            }
            return matrixCursor;
        }

        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}
