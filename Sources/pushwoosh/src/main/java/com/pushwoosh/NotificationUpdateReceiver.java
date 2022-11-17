package com.pushwoosh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pushwoosh.notification.NotificationIntentHelper;


public class NotificationUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		NotificationIntentHelper.processIntent(context, intent);
	}
}
