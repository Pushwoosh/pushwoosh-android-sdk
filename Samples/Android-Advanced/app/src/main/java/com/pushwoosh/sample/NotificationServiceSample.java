package com.pushwoosh.sample;

import android.os.Handler;
import android.support.annotation.MainThread;
import android.util.Log;

import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

public class NotificationServiceSample extends NotificationServiceExtension {
	@Override
	public boolean onMessageReceived(final PushMessage message) {
		Log.d(PushwooshSampleApp.LTAG, "NotificationService.onMessageReceived: " + message.toJson().toString());

		// automatic foreground push handling
		if (isAppOnForeground()) {
			Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
			mainHandler.post(new Runnable() {
				@Override
				public void run() {
					handlePush(message);
				}
			});

			// this indicates that notification should not be displayed
			return true;
		}

		return false;
	}

	@Override
	protected void startActivityForPushMessage(PushMessage message) {
		super.startActivityForPushMessage(message);

		// TODO: start custom activity if necessary

		handlePush(message);
	}

	@MainThread
	private void handlePush(PushMessage message) {
		Log.d(PushwooshSampleApp.LTAG, "NotificationService.handlePush: " + message.toJson().toString());
		// TODO: handle push message
	}
}
