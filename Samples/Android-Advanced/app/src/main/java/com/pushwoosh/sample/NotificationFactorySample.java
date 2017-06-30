package com.pushwoosh.sample;

import android.app.Notification;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Log;

import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationFactory;

public class NotificationFactorySample extends PushwooshNotificationFactory {
	@Override
	public Notification onGenerateNotification(@NonNull PushMessage pushMessage) {
		Log.d(PushwooshSampleApp.LTAG, "onGenerateNotification: " + pushMessage.toJson().toString());

		Notification notification = super.onGenerateNotification(pushMessage);
		// TODO: customise notification content

		return notification;
	}

	@Override
	protected Bitmap getLargeIcon(PushMessage pushMessage) {
		// TODO: set custom large icon for notification

		return super.getLargeIcon(pushMessage);
	}
}
