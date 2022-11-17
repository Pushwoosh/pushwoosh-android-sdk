/*
 *
 * Copyright (c) 2019. Pushwoosh Inc. (http://www.pushwoosh.com)
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

package com.pushwoosh.baidu;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.pushwoosh.baidu.internal.BaiduDeviceSpecific;
import com.pushwoosh.baidu.internal.BaiduPushRegistrar;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.security.CallingPackageChecker;

public class BaiduInitProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        Context applicationContext = getContext().getApplicationContext();
        BaiduPushRegistrar pushRegistrar = new BaiduPushRegistrar(applicationContext);
        BaiduDeviceSpecific deviceSpecific = new BaiduDeviceSpecific(pushRegistrar);
        new DeviceSpecificProvider.Builder()
                .setDeviceSpecific(deviceSpecific)
                .build(true);

        return true;
    }


    @Override
    public Cursor query( Uri uri, String[] projection, String selection,  String[] selectionArgs,  String sortOrder) {
        CallingPackageChecker.checkCallingPackage(this);
        return null;
    }


    @Override
    public String getType(Uri uri) {
        return null;
    }


    @Override
    public Uri insert( Uri uri,  ContentValues values) {
        return null;
    }

    @Override
    public int delete( Uri uri,  String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update( Uri uri,  ContentValues values,  String selection,  String[] selectionArgs) {
        return 0;
    }
}
