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

package com.pushwoosh.notification.builder;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationManagerCompat;
import android.text.TextUtils;

import com.pushwoosh.exception.GroupIdNotFoundException;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.internal.utils.PendingIntentUtils;
import com.pushwoosh.notification.Action;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.SummaryNotificationUtils;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;
import com.pushwoosh.repository.util.PushBundleDatabaseEntry;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class NotificationBuilderManager {
	/**
	 * @return NotificationBuilder depending on version of device
	 */
	public static NotificationBuilder createNotificationBuilder(Context context, String channelId) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			return new NotificationBuilderApi26(context, channelId);
		}

		return new NotificationBuilderApi14(context, channelId);
	}

	@RequiresApi(Build.VERSION_CODES.N)
	public static SummaryNotificationBuilder createSummaryNotificationBuilder(Context context, String channelId) {
		return new SummaryNotificationBuilderApi24(context, channelId);
	}

	public static void addAction(Context context, NotificationBuilder notificationBuilder, Action action) {
		int iconId = 0;
		if (action.getIcon() != null) {
			//if this is one of the android drawables
			if (action.getIcon().startsWith("android.R.drawable")) {
				String actionName = action.getIcon().replace("android.R.drawable.", "");
				Field[] drawables = android.R.drawable.class.getFields();
				for (Field f : drawables) {
					try {
						if (actionName.equalsIgnoreCase(f.getName())) {
							iconId = f.getInt(f);
						}
					} catch (Exception e) {
						PWLog.exception(e);
					}
				}
			}

			if (iconId == 0) {
				//try to get the one from the app package
				iconId = AndroidPlatformModule.getResourceProvider().getIdentifier(action.getIcon(), "drawable");
			}
		}
		String title = action.getTitle();

		String intentAction = action.getIntentAction();
		Intent actionIntent = new Intent();
		String url = action.getUrl();
		if (url != null) {
			actionIntent = new Intent(intentAction, Uri.parse(url));
		}

		@SuppressWarnings("rawtypes")
		Class clazz = action.getActionClass();
		if (clazz != null) {
			actionIntent.setClass(context, clazz);
		}

		if (intentAction != null) {
			actionIntent.setAction(intentAction);
		}

		JSONObject extras = action.getExtras();
		if (extras != null) {
			Iterator<?> keys = extras.keys();

			while (keys.hasNext()) {
				String key = (String) keys.next();
				try {
					actionIntent.putExtra(key, extras.getString(key));
				} catch (JSONException e) {
					PWLog.exception(e);
				}
			}
		}

		PendingIntent pendingIntent;
		switch (action.getType()) {
			case ACTIVITY:
				pendingIntent = PendingIntent.getActivity(context, 0, actionIntent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));
				break;

			case BROADCAST:
				pendingIntent = PendingIntent.getBroadcast(context, 0, actionIntent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));
				break;

			default:
				pendingIntent = PendingIntent.getService(context, 0, actionIntent, PendingIntentUtils.addImmutableFlag(PendingIntent.FLAG_UPDATE_CURRENT));
		}

		if (pendingIntent != null) {
			notificationBuilder.addAction(iconId, title, pendingIntent);
		}
	}

	public static void addLED(@NonNull NotificationBuilder notificationBuilder, @Nullable Integer color, int ledOnMs, int ledOffMs) {
		NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
		boolean enabled = notificationPrefs.ledEnabled().get();
		int defaultColor = notificationPrefs.ledColor().get();

		if (!enabled && color == null) {
			return;
		}

		notificationBuilder.setLed(color == null ? defaultColor : color, ledOnMs, ledOffMs);
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	public static List<StatusBarNotification> getActiveNotifications() {
		ArrayList<StatusBarNotification> activeNotifications = new ArrayList<>();
		NotificationManager notificationManager = AndroidPlatformModule.getManagerProvider().getNotificationManager();
		if (notificationManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			try {
				StatusBarNotification[] statusBarNotifications = notificationManager.getActiveNotifications();
				for (StatusBarNotification statusBarNotification : statusBarNotifications) {
					boolean isGroupSummary = isGroupSummary(statusBarNotification);
					boolean hasGroup = statusBarNotification.isGroup();
					if (!isGroupSummary && hasGroup) {
						activeNotifications.add(statusBarNotification);
					}
				}
			} catch (Throwable t) {
				// getActiveNotifications is not stable and sometimes throws an exception
				return Collections.emptyList();
			}
		} else {
			PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
		}
		return activeNotifications;
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	public static List<StatusBarNotification> getActiveNotificationsForGroup(String groupId) {
		//summary notification is generated only for Android N and higher, so it's safe to use streams
		return getActiveNotifications().stream()
				.filter(statusBarNotification -> TextUtils.equals(statusBarNotification.getNotification().getGroup(), groupId))
				.collect(Collectors.toList());
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public static boolean isGroupSummary(StatusBarNotification notification) {
		return (notification.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) != 0;
	}

	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	public static boolean isReplacingMessage(PushMessage pushMessage, List<StatusBarNotification> activeNotifications) {
		if (activeNotifications == null || activeNotifications.isEmpty()) {
			return false;
		}
		if (TextUtils.isEmpty(pushMessage.getTag())) {
			return false;
		}
		for (StatusBarNotification statusBarNotification : activeNotifications) {
			if (statusBarNotification.getId() == 0 && TextUtils.equals(pushMessage.getTag(), statusBarNotification.getTag())) {
				return true;
			}
		}
		return false;
	}

	@RequiresApi(api = Build.VERSION_CODES.KITKAT_WATCH)
	public static void setGroupToActiveNotifications(List<StatusBarNotification> activeNotifications, String group) {
		if (AndroidPlatformModule.getApplicationContext() == null) {
			PWLog.error("setGroupToActiveNotifications " + AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
			return;
		}
		Context context = AndroidPlatformModule.getApplicationContext();
		for (StatusBarNotification activeNotification : activeNotifications) {
			Notification.Builder notificationBuilder;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				notificationBuilder = Notification.Builder.recoverBuilder(context, activeNotification.getNotification());
			} else {
				notificationBuilder = new Notification.Builder(context);
			}
			Notification notification = notificationBuilder
					.setOnlyAlertOnce(true)
					.setGroup(group)
					.build();
			if (activeNotifications.size() == 1) {
				// cancel a single notification without a group
				NotificationManagerCompat.from(context).cancel(activeNotification.getTag(), activeNotification.getId());
			}
			// fire the same notification with the group set
			NotificationManagerCompat.from(context).notify(activeNotification.getId(), notification);
		}
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	public static Notification setNotificationGroup(@NonNull Notification notification, @Nullable String group) {
		if (AndroidPlatformModule.getApplicationContext() == null) {
			PWLog.error("setNotificationGroup " + AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
			return null;
		}
		Context context = AndroidPlatformModule.getApplicationContext();
		Notification.Builder notificationBuilder = Notification.Builder.recoverBuilder(context, notification);

		return notificationBuilder
				.setGroup(group)
				.build();
	}

	@RequiresApi(api = Build.VERSION_CODES.M)
	public static void removeInboxNotificationFromStatusBar(String inboxMessageId) {
		try {
			NotificationManager notificationManager = AndroidPlatformModule.getManagerProvider().getNotificationManager();
			StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();

			for (StatusBarNotification notification: activeNotifications) {
				Bundle bundle = notification.getNotification().extras;

				if (bundle != null) {
					String pushInboxId = bundle.getString("pw_inbox");

					if (pushInboxId != null && TextUtils.equals(inboxMessageId, pushInboxId)) {
						if (TextUtils.isEmpty(notification.getTag())) {
							notificationManager.cancel(notification.getId()); //remove from status bar
						} else {
							notificationManager.cancel(notification.getTag(), notification.getId());
						}

						PendingIntent deleteIntent = notification.getNotification().deleteIntent;

						if (deleteIntent != null) {
							notification.getNotification().deleteIntent.send(); //remove from db
						}

						break;
					}
				}
			}

			//remove summary notification
			activeNotifications = notificationManager.getActiveNotifications();

			if (activeNotifications.length == 1) {
				StatusBarNotification notification = activeNotifications[0];

				if (NotificationBuilderManager.isGroupSummary(notification)) {
					notificationManager.cancel(notification.getId());

					PendingIntent deleteIntent = notification.getNotification().deleteIntent;

					if (deleteIntent != null) {
						notification.getNotification().deleteIntent.send(); //remove from db
					}
				}
			}
		} catch (Exception e) {
			PWLog.error("Can't delete message from status bar", e);
		}
	}

	public static void removeInboxNotification(String inboxMessageId) {
		try {
			Integer notificationId = RepositoryModule.getInboxNotificationStorage().getNotificationId(inboxMessageId);
			String notificationTag = RepositoryModule.getInboxNotificationStorage().getNotificationTag(inboxMessageId);
			if (notificationId != null) {
				NotificationManager notificationManager = AndroidPlatformModule.getManagerProvider().getNotificationManager();
				if (notificationManager != null) {
					if (!TextUtils.isEmpty(notificationTag)) {
						notificationManager.cancel(notificationTag, notificationId);
					} else {
						notificationManager.cancel(notificationId);
					}
				}
			}
		} catch (Exception e) {
			PWLog.error("Can't delete notification from notification manager", e);
		}
	}

	public static void cancelLastStatusBarNotificationForGroup(String groupId) throws Exception {
		PushBundleDatabaseEntry entry = RepositoryModule.getPushBundleStorage().getLastPushBundleEntryForGroup(groupId);
		NotificationManager notificationManager = getNotificationManager();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			List<StatusBarNotification> activeNotifications = NotificationBuilderManager.getActiveNotificationsForGroup(groupId);
			for (StatusBarNotification statusBarNotification : activeNotifications) {
				if (statusBarNotification.getId() == entry.getNotificationId()) {
					statusBarNotification.getNotification().deleteIntent.send();
					notificationManager.cancel(entry.getNotificationId());
				}
			}
			if (getActiveNotifications().isEmpty()) {
				cancelGroupSummary(groupId);
			}
		}
		//remove push bundle from database
		RepositoryModule.getPushBundleStorage().removeGroupPushBundle(entry.getRowId());
	}

	public static void cancelGroupSummary(String groupId) {
		int notificationId = SummaryNotificationUtils.getNotificationIdForGroup(groupId);
		if (notificationId != -1) {
			getNotificationManager().cancel(notificationId);
			try {
				RepositoryModule.getSummaryNotificationStorage().remove(groupId);
			} catch (GroupIdNotFoundException e) {
				PWLog.error("Failed to remove entry for group id " + groupId + " from summaryNotificationStorage");
			}
		}
	}

	private static NotificationManager getNotificationManager() {
		return (NotificationManager) Objects.requireNonNull(AndroidPlatformModule.getApplicationContext())
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}
}
