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
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import com.pushwoosh.badge.thirdparty.shortcutbadger.Badger;
import com.pushwoosh.badge.thirdparty.shortcutbadger.ShortcutBadgeException;

public class ZTEHomeBadger implements Badger {

    @Override
    public void executeBadge(Context context, ComponentName componentName, int badgeCount)
            throws ShortcutBadgeException {
        Bundle extra = new Bundle();
        extra.putInt("app_badge_count", badgeCount);
        extra.putString("app_badge_component_name", componentName.flattenToString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            context.getContentResolver().call(
                    Uri.parse("content://com.android.launcher3.cornermark.unreadbadge"),
                    "setAppUnreadCount", null, extra);
        }
    }

    @Override
    public List<String> getSupportLaunchers() {
        return new ArrayList<String>(0);
    }
} 

