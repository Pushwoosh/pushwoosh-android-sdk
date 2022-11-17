package com.pushwoosh.testingapp;

import android.app.Notification;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.widget.RemoteViews;

import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.PushwooshNotificationFactory;

/**
 * Created by etkachenko on 1/24/17.
 */

public class TestingAppNotificationFactory extends PushwooshNotificationFactory {

	@Override
	public Notification onGenerateNotification(@NonNull PushMessage pushData) {
		Boolean customNotifications = AppData.getInstance().getCustomNotifications();
		if (customNotifications) {
			return onGenerateNotificationCustom(pushData);
		} else {
			return super.onGenerateNotification(pushData);
		}
	}

	private Notification onGenerateNotificationCustom(PushMessage pushData) {
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext());
		notificationBuilder.setContentTitle(getContentFromHtml(pushData.getHeader()));
		notificationBuilder.setContentText(getContentFromHtml(pushData.getMessage()));
		notificationBuilder.setSmallIcon(pushData.getSmallIcon());
		notificationBuilder.setTicker(getContentFromHtml(pushData.getTicker()));
		notificationBuilder.setWhen(System.currentTimeMillis());

		RemoteViews contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.notification);
		contentView.setImageViewResource(R.id.image, R.drawable.image1);
		contentView.setTextViewText(R.id.title, pushData.getHeader());
		contentView.setTextViewText(R.id.text, pushData.getMessage());
		notificationBuilder.setContent(contentView);

		final Notification notification = notificationBuilder.build();
		addSound(notification, pushData.getSound());
		addVibration(notification, pushData.getVibration());
		addCancel(notification);

		return notification;
	}
}
