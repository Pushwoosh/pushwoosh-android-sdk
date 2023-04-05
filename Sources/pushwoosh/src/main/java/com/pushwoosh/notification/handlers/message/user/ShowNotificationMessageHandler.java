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

package com.pushwoosh.notification.handlers.message.user;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import android.text.TextUtils;

import com.pushwoosh.NotificationUpdateReceiver;
import com.pushwoosh.internal.event.EventBus;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.notification.*;
import com.pushwoosh.notification.builder.NotificationBuilderManager;
import com.pushwoosh.repository.LocalNotificationStorage;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import java.util.List;

import static com.pushwoosh.internal.platform.AndroidPlatformModule.getApplicationContext;
import static com.pushwoosh.notification.NotificationIntentHelper.SUMMARY_GROUP_ID;

class ShowNotificationMessageHandler extends NotificationMessageHandler {
	private final static String TAG = ShowNotificationMessageHandler.class.getSimpleName();
	private final static Object MUTEX = new Object();

	private final NotificationFactory notificationFactory;
	private final NotificationPrefs notificationPrefs;

	ShowNotificationMessageHandler() {
		notificationPrefs = RepositoryModule.getNotificationPreferences();
		this.notificationFactory = provideNotificationFactory();
	}

	@Override
	protected void handleNotification(final PushMessage pushMessage) {
		if (!pushMessage.isSilent()) {
			if (notificationPrefs.multiMode().get()) {
				handleMultiModeNotification(pushMessage);
				return;
			}

			Notification notification = notificationFactory.onGenerateNotification(pushMessage);
			if (notification == null) {
				return;
			}

			Intent notifyIntent = notificationFactory.getNotificationIntent(pushMessage);
			// Samsung devices with Android 13+ update existing notification incorrectly (sometimes old banner remains)
			// so we have to cancel existing notification before showing a new one, see
			// https://jira.corp.pushwoosh.com/browse/PUSH-33434
			NotificationManagerCompat.from(getApplicationContext()).cancelAll();
			fireNotification(notification, notifyIntent, pushMessage); // deleteIntent is null as
			                                                           // handleNotification method does not
																	   // store push bundles in database
		}
	}

	protected void handleMultiModeNotification(PushMessage pushMessage) {
		Notification notification = notificationFactory.onGenerateNotification(pushMessage);
		notification = NotificationBuilderManager.rebuildNotificationWithDefaultGroup(notification);

		if (notification == null) {
			return;
		}
		// generate summary notification only for Android.N and newer versions
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			List<StatusBarNotification> activeNotifications = NotificationBuilderManager.getActiveNotifications();
			if (activeNotifications.size() >= 1) {
				int activeNotificationsCount = NotificationBuilderManager.isReplacingMessage(pushMessage, activeNotifications)
						? activeNotifications.size() : activeNotifications.size() + 1;
				String notificationChannelId = (Build.VERSION.SDK_INT >= 26)
						? notification.getChannelId() : SummaryNotificationFactory.NEED_TO_ADD_NEW_NOTIFICATION_CHANNEL_ID;
				Notification summaryNotification = SummaryNotificationUtils.getSummaryNotification(activeNotificationsCount, notificationChannelId);
				if (summaryNotification != null) {
					SummaryNotificationUtils.fireSummaryNotification(summaryNotification);
				}
			}
		}

		Intent contentIntent = notificationFactory.getNotificationIntent(pushMessage);
		contentIntent.putExtra(NotificationIntentHelper.EXTRA_GROUP_ID, SUMMARY_GROUP_ID);

		fireNotification(notification, contentIntent, pushMessage);
	}

	private void fireNotification(final Notification notification, Intent contentIntent, PushMessage data) {
		Context context = getApplicationContext();
		if (context == null) {
			PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
			return;
		}

		String tag = data.getTag();
		int messageId = getMessageId(tag);
		Intent deleteIntent = null;

		try {
			long notificationId = RepositoryModule.getPushBundleStorage().putGroupPushBundle(data.toBundle(), messageId);
			deleteIntent = getDeleteIntent(notificationId);
		} catch (Exception e) {
			// ignore
		}

		notification.contentIntent = PendingIntent.getActivity(context, messageId, contentIntent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_CANCEL_CURRENT));
		if (deleteIntent != null) {
			long pushwooshNotificationUID = data.getPushwooshNotificationId();
			if (pushwooshNotificationUID != -1) {
				deleteIntent.putExtra(NotificationIntentHelper.EXTRA_PUSHWOOSH_NOTIFICATION_ID, pushwooshNotificationUID);
			}
			notification.deleteIntent = PendingIntent.getBroadcast(context, messageId, deleteIntent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_CANCEL_CURRENT));
		}
		notifyNotificationCreated(contentIntent, tag, messageId);

		android.app.NotificationManager manager = AndroidPlatformModule.getManagerProvider().getNotificationManager();

		if (manager == null) {
			return;
		}

		// Store notification id to remove notification from notificationManager on Android 5 or less
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			saveNotificationIdAndTag(data, messageId, tag);
		}
		manager.notify(tag, messageId, notification);

		RepositoryModule.getStatusBarNotificationStorage().put(data.getPushwooshNotificationId(), messageId);

		tryTurnScreenOn();

		addToPushHistory(data);

		EventBus.sendEvent(new NotificationCreatedEvent(messageId, tag, data));
	}

	private void saveNotificationIdAndTag(PushMessage pushMessage, int notificationId, String tag) {
		try {
			Bundle bundle = pushMessage.toBundle();
			String pushInboxId = bundle.getString("pw_inbox");
			if (!TextUtils.isEmpty(pushInboxId)) {
				RepositoryModule.getInboxNotificationStorage().putNotificationIdAndTag(pushInboxId, notificationId, tag);
			}
		} catch (Exception e) {
			PWLog.error(TAG, e);
		}
	}

	private void tryTurnScreenOn() {
		NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
		if (notificationPrefs.lightScreenOn().get()) {
			NotificationUtils.turnScreenOn();
		}
	}

	private void addToPushHistory(final PushMessage data) {
		String message = data.toJson().toString();
		notificationPrefs.pushHistory().add(message);
	}

	private void notifyNotificationCreated(final Intent intent, final String tag, final int messageId) {
		LocalNotificationStorage localNotificationStorage = RepositoryModule.getLocalNotificationStorage();
		localNotificationStorage.removeLocalNotificationShown(messageId, tag);

		if (intent.hasExtra(LocalNotificationReceiver.EXTRA_NOTIFICATION_ID)) {
			int localNotificationId = intent.getIntExtra(LocalNotificationReceiver.EXTRA_NOTIFICATION_ID, 0);
			localNotificationStorage.addLocalNotificationShown(localNotificationId, messageId, tag);
		}
	}

	private int getMessageId(final String tag) {
		int messageId = 0;

		if (TextUtils.isEmpty(tag)) {
			synchronized (MUTEX) {
				messageId = notificationPrefs.messageId().get();

				if (notificationPrefs.multiMode().get()) {
					messageId++;
					notificationPrefs.messageId().set(messageId);
				}
			}
		}
		return messageId;
	}

	private NotificationFactory provideNotificationFactory() {
		NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
		try {
			Class<?> clazz = notificationPrefs.notificationFactoryClass().get();
			if (clazz != null) {
				return (NotificationFactory) clazz.newInstance();
			}
		} catch (Exception e) {
			PWLog.exception(e);
			// fallback to default
		}

		return new PushwooshNotificationFactory();
	}

	@NonNull
	private Intent getDeleteIntent(long rowId) {
		Context applicationContext = getApplicationContext();
		Intent deleteIntent = new Intent(applicationContext, NotificationUpdateReceiver.class);
		deleteIntent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_ROW_ID, rowId);
		deleteIntent.putExtra(NotificationIntentHelper.EXTRA_IS_DELETE_INTENT, true);
		deleteIntent.setAction(Long.toString(System.currentTimeMillis()));
		return deleteIntent;
	}
}
