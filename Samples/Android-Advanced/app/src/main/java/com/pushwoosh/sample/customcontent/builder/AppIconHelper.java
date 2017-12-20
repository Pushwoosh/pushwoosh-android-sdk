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

package com.pushwoosh.sample.customcontent.builder;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import com.pushwoosh.internal.utils.PWLog;

@RequiresApi(Build.VERSION_CODES.M)
class AppIconHelper {

	@Nullable
	static Icon getAppIcon(@Nullable Context context, String packageName) {
		if (context == null) {
			return null;
		}

		ApplicationInfo appInfoProvider = context.getApplicationInfo();
		if (appInfoProvider == null) {
			return null;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			return AppIconHelperV26.getAppIcon(context.getPackageManager(), packageName);
		}

		try {
			return Icon.createWithResource(context, appInfoProvider.icon);
		} catch (Exception e) {
			PWLog.error("Failed creation of icon", e);
		}

		return null;
	}
} 