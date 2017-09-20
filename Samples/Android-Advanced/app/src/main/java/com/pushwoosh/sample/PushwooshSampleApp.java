package com.pushwoosh.sample;

import android.app.Application;
import android.util.Log;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.location.PushwooshLocation;
import com.pushwoosh.notification.LocalNotification;
import com.pushwoosh.tags.Tags;


public class PushwooshSampleApp extends Application {
	public static final String LTAG = "PushwooshSample";

	@Override
	public void onCreate() {
		super.onCreate();

		Pushwoosh.getInstance().registerForPushNotifications(result -> {
			if (result.isSuccess()) {
				Log.d(LTAG, "Successfully registered for push notifications with token: " + result.getData());
			} else {
				Log.d(LTAG, "Failed to register for push notifications:u " + result.getException().getMessage());
			}
		});

		PushwooshLocation.startLocationTracking();

		Pushwoosh.getInstance().sendTags(Tags.intTag("fav_number", 42));

		LocalNotification notification = new LocalNotification.Builder()
				.setMessage("Local notification")
				.setDelay(5)
				.build();

		Pushwoosh.getInstance().scheduleLocalNotification(notification);
	}
}
