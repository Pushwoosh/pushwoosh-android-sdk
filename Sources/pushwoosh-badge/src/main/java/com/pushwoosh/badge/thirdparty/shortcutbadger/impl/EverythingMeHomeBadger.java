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

package com.pushwoosh.badge.thirdparty.shortcutbadger.impl;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import com.pushwoosh.badge.thirdparty.shortcutbadger.Badger;
import com.pushwoosh.badge.thirdparty.shortcutbadger.ShortcutBadgeException;

import java.util.Arrays;
import java.util.List;


/**
 * @author Radko Roman
 * @since  13.04.17.
 */
public class EverythingMeHomeBadger implements Badger {

    private static final String CONTENT_URI = "content://me.everything.badger/apps";
    private static final String COLUMN_PACKAGE_NAME = "package_name";
    private static final String COLUMN_ACTIVITY_NAME = "activity_name";
    private static final String COLUMN_COUNT = "count";

    @Override
    public void executeBadge(Context context, ComponentName componentName, int badgeCount) throws ShortcutBadgeException  {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_PACKAGE_NAME, componentName.getPackageName());
        contentValues.put(COLUMN_ACTIVITY_NAME, componentName.getClassName());
        contentValues.put(COLUMN_COUNT, badgeCount);
        context.getContentResolver().insert(Uri.parse(CONTENT_URI), contentValues);
    }

    @Override
    public List<String> getSupportLaunchers() {
        return Arrays.asList("me.everything.launcher");
    }
}
