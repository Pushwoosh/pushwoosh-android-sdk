package com.pushwoosh.internal.network;

import android.content.Context;

import com.pushwoosh.internal.utils.UUIDFactory;

public class RequestStorage extends BaseRequestStorage {
	private static final String TAG = RequestStorage.class.getSimpleName();
	private static final String DB_NAME = "request.db";
	private static final String TABLE_NAME = "REQUEST";

	public RequestStorage(Context context, UUIDFactory uuidFactory) {
		super(context, uuidFactory, DB_NAME, 1, TABLE_NAME, TAG);
	}
}
