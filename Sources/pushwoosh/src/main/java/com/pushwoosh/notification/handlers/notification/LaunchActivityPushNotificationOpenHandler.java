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

package com.pushwoosh.notification.handlers.notification;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

class LaunchActivityPushNotificationOpenHandler implements PushNotificationOpenHandler {


	LaunchActivityPushNotificationOpenHandler() {/*do nothing*/}

	@Override
	public void postHandleNotification(final Bundle pushBundle) {
		//temporary disable this code until the server supports it
		Context applicationContext = AndroidPlatformModule.getApplicationContext();
		if (applicationContext == null) {
			PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
			return;
		}

		String packageName = (String) pushBundle.get("launch");
		if (packageName != null) {
			Intent launchIntent = null;
			try {
				PackageManager packageManager = AndroidPlatformModule.getManagerProvider().getPackageManager();
				launchIntent = packageManager == null ? null : packageManager.getLaunchIntentForPackage(packageName);
			} catch (Exception e) {
				PWLog.error("Application not found", e);
			}

			if (launchIntent != null) {
				launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				applicationContext.startActivity(launchIntent);
			}
		}

	}
}
