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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.pushwoosh.inbox.notification.InboxPayloadDataProvider;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

class DeepLinkActionStrategy implements InboxActionStrategy {
	private Context context = AndroidPlatformModule.getApplicationContext();

	@Override
	public void performAction(JSONObject actionParams) throws JSONException {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		final String deepLink = InboxPayloadDataProvider.getUrl(actionParams);
		if (deepLink == null) {
			return;
		}

		intent.setData(Uri.parse(deepLink));
		try {
			context.startActivity(intent);
		} catch (ActivityNotFoundException e) {
			PWLog.error("Can't find activity for deep link: " + deepLink, e);
		}
	}
}
