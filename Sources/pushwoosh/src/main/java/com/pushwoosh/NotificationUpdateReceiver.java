package com.pushwoosh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.NotificationIntentHelper;


public class NotificationUpdateReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			NotificationIntentHelper.processIntent(context, intent);
		} catch (Exception e) {
			PWLog.error("NotificationUpdateReceiver", "Failed to process intent", e);
		}
	}
}
