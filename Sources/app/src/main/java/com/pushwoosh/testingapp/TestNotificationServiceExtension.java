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

package com.pushwoosh.testingapp;

import android.os.Handler;
import androidx.annotation.MainThread;
import android.util.Log;

import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.testingapp.helpers.ShowMessageHelper;

import java.util.List;

@SuppressWarnings("unused")
public class TestNotificationServiceExtension extends NotificationServiceExtension {
	private static final String TAG = "TestNotificationService";

	@Override
	public boolean onMessageReceived(final PushMessage message) {
		if (message.isLocal()) {
			if (AppData.getInstance().getLocalNotificationIDList().size() > 0) {
				AppData.getInstance().getLocalNotificationIDList().remove(0);
			}
		}
		super.onMessageReceived(message);
		ShowMessageHelper.log("PushMessage recieved: " + message.toJson().toString());
		if (AppData.getInstance().getHandleInForeground()) {
			AppData.getInstance().setPushBundle(message.toBundle());
			if (isAppOnForeground() && getApplicationContext() != null) {
				Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
				mainHandler.post(() -> handlePush(message));
				return true;
			}
		}
		return false;
	}

	@Override
	protected void startActivityForPushMessage(PushMessage message) {
		super.startActivityForPushMessage(message);
		handlePush(message);
	}

	@MainThread
	private void handlePush(PushMessage message) {
		ShowMessageHelper.showMessage("PushMessage accepted: " + message.toJson().toString());
	}

	@Override
	protected void onMessagesGroupOpened(List<PushMessage> messages) {
		super.onMessagesGroupOpened(messages);
		Log.d(TAG, "opened group with " + messages.size() + " messages");
	}
}
