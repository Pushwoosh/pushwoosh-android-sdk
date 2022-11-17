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

package com.pushwoosh.inbox.internal.action;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.pushwoosh.inbox.notification.InboxPayloadDataProvider;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

class UrlActionStrategy implements InboxActionStrategy {
	String TAG = "UrlActionStrategy";
	@Override
	public void performAction(JSONObject actionParams) throws JSONException {
		String remoteUrl = InboxPayloadDataProvider.getUrl(actionParams);
		if(remoteUrl == null){
			return;
		}

		Context context = AndroidPlatformModule.getApplicationContext();
		if (context == null) {
			PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
			return;
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(remoteUrl));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PackageManager packageManager = context.getPackageManager();
		if (intent.resolveActivity(packageManager) != null) {
			try {
				context.startActivity(intent);
			} catch (Exception e) {
				PWLog.error(TAG, "Failed to start activity: " + e.getMessage());
			}
		} else {
			PWLog.error(TAG, "Failed to resolve activity for Action.View intent");
		}
	}
}
