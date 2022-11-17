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

package com.pushwoosh.notification;

import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.internal.chain.Chain;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.notification.handlers.notification.PushNotificationOpenHandler;

import java.util.Iterator;

class NotificationOpenHandler {
	private static final String MESSAGE_HANDLER_KEY = ".MESSAGE";

	private Chain<PushNotificationOpenHandler> notificationOpenHandlerChain;
	@Nullable
	private final Context context = AndroidPlatformModule.getApplicationContext();

	NotificationOpenHandler(final Chain<PushNotificationOpenHandler> notificationOpenHandlerChain) {
		this.notificationOpenHandlerChain = notificationOpenHandlerChain;
	}

	boolean preHandleNotification(Bundle pushBundle) {
		String link = PushBundleDataProvider.getLink(pushBundle);
		if (TextUtils.isEmpty(link) || context == null) {
			return false;
		}

		try {
			Intent notifyIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
			PackageManager packageManager = context.getPackageManager();
			if (notifyIntent.resolveActivity(packageManager) == null) {
				return false;
			}
			notifyIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
			PendingIntent.getActivity(context, 0, notifyIntent, PendingIntentUtils.addImmutableFlag(0)).send();
			return true;
		} catch (Exception e) {
			PWLog.exception(e);
		}

		return false;
	}

	void startPushLauncherActivity(PushMessage message) {
		Bundle extras = new Bundle();
		extras.putString(Pushwoosh.PUSH_RECEIVE_EVENT, message.toJson().toString());
		boolean launchDefaultActivity = false;

		Intent notifyIntent = new Intent();
		String intentAction = AndroidPlatformModule.getAppInfoProvider().getPackageName() + MESSAGE_HANDLER_KEY;
		notifyIntent.setAction(intentAction);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		notifyIntent.putExtras(extras);
		try {
			if (context != null) {
				context.startActivity(notifyIntent);
			}
		} catch (ActivityNotFoundException e) {
			launchDefaultActivity = true;
			PWLog.warn("Can't launch activity. Are you sure you have an activity with '" + intentAction + "' action in your manifest? Launching default activity.");
		}

		if (!launchDefaultActivity) {
			return;
		}

		//launching default launcher category activity
		try {
			String packageName = AndroidPlatformModule.getAppInfoProvider().getPackageName();
			PackageManager packageManager = AndroidPlatformModule.getManagerProvider().getPackageManager();
			Intent launchIntent = packageManager == null ? null : packageManager.getLaunchIntentForPackage(packageName);
			if (launchIntent == null) {
				throw new ActivityNotFoundException();
			}
			launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			launchIntent.putExtras(extras);
			context.startActivity(launchIntent);
		} catch (ActivityNotFoundException e) {
			PWLog.error("Failed to start default launch activity.", e);
		}
	}

	void postHandleNotification(Bundle pushBundle) {
		Iterator<PushNotificationOpenHandler> iterator = notificationOpenHandlerChain.getIterator();
		while (iterator.hasNext()) {
			iterator.next().postHandleNotification(pushBundle);
		}
	}
}
