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

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.pushwoosh.internal.utils.PWLog;

public class Utils {
    private static final String TAG = "[PushwooshBaiduUtils]";

    public static String getMetaValue(Context context, String metaKey) {
        Bundle metaData = null;
        String apiKey = null;
        if (context == null || metaKey == null) {
            return null;
        }
        try {
            ApplicationInfo ai = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(),
                            PackageManager.GET_META_DATA);
            if (null != ai) {
                metaData = ai.metaData;
            }
            if (null != metaData) {
                apiKey = metaData.getString(metaKey);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "error " + e.getMessage());
        }
        return apiKey;
    }


    public static boolean checkPermission(Context context) {
        try {
            int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE);
            return permissionCheck == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) {
            PWLog.error("error in checking vibrate permission", e);
        }
        return false;
    }
}
