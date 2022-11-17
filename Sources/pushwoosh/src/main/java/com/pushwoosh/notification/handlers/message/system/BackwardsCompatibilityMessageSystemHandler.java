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

package com.pushwoosh.notification.handlers.message.system;

import android.content.Intent;
import android.os.Bundle;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.notification.PushBundleDataProvider;


import static com.pushwoosh.internal.platform.utils.DeviceUtils.isAppOnForeground;

class BackwardsCompatibilityMessageSystemHandler implements MessageSystemHandler {
	private static final String JSON_DATA_KEY = "pw_data_json_string";
	private static final String ACTION_SILENT_PUSH = ".action.SILENT_PUSH_RECEIVE";


	BackwardsCompatibilityMessageSystemHandler() {
	}

	@Override
	public boolean preHandleMessage(final Bundle pushBundle) {
		if (PushBundleDataProvider.isSilent(pushBundle)) {
			handleSilentPush(pushBundle);
		}

		boolean foreground = isAppOnForeground();
		PushBundleDataProvider.setForeground(pushBundle, foreground);
		return false;
	}

	private void handleSilentPush(final Bundle pushBundle) {
		String packageName = AndroidPlatformModule.getAppInfoProvider().getPackageName();

		Intent broadcastIntent = new Intent();
		broadcastIntent.setAction(packageName + ACTION_SILENT_PUSH);
		broadcastIntent.putExtras(pushBundle);

		broadcastIntent.putExtra(JSON_DATA_KEY, PushBundleDataProvider.asJson(pushBundle).toString());

		AndroidPlatformModule.getReceiverProvider().sendBroadcast(broadcastIntent, DeviceSpecificProvider.getInstance().permission(packageName));
	}
}
