package com.pushwoosh;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.pushwoosh.internal.checker.CheckerProvider;
import com.pushwoosh.internal.utils.security.CallingPackageChecker;

public class PushwooshInitProvider extends ContentProvider {
	
	public PushwooshInitProvider() {
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public boolean onCreate() {
		CheckerProvider.getInstance().check();
		PushwooshInitializer.init(getContext());
		return true;
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		return 0;
	}

	@Override
	public String getType(@NonNull Uri uri) {
		return null;
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		return null;
	}

	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection,
	                    String[] selectionArgs, String sortOrder) {
		CallingPackageChecker.checkCallingPackage(this);
		return null;
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection,
	                  String[] selectionArgs) {
		return 0;
	}
}
