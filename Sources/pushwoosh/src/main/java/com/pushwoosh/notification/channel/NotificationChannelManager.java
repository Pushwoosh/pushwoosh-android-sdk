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

package com.pushwoosh.notification.channel;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.VibrateType;

import static com.pushwoosh.notification.channel.NotificationChannelInfoProvider.getChannelName;
import static com.pushwoosh.repository.NotificationPrefs.DEFAULT_GROUP_CHANNEL_ID;

public class NotificationChannelManager {

	private final NotificationManagerProxy notificationManagerProxy;

	public NotificationChannelManager(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			notificationManagerProxy = new NotificationManagerProxyApi26(notificationManager);
		} else {
			notificationManagerProxy = new NotificationManagerProxyApi14();
		}
	}

	/**
	 * Create, if not exist, new notification channel from pushMessage.
	 *
	 * @param pushMessage - if push message doesn't contain "pw_channel" attribute, default channel will be created
	 * @param channelName - name of the channel that will be specified on its creation
	 * @param channelDescription - description of the channel that will be specified on its creation
	 * @return channel id which connected with channel name. For Api less than 26 it doesn't create anything
	 */
	public String addChannel(PushMessage pushMessage, String channelName, @Nullable String channelDescription) {
		String pushChannelName = getChannelName(pushMessage);
		final String channelId = NotificationChannelInfoProvider.channelId(pushChannelName);

		// empty channel names are not allowed
		if (TextUtils.isEmpty(channelName)) {
			channelName = pushChannelName;
		}

		notificationManagerProxy.addChannel(channelId, channelName, channelDescription, pushMessage);
		return channelId;
	}

	public String addGroupNotificationsChannel(String channelName) {
		return notificationManagerProxy.addDefaultGroupChannel(DEFAULT_GROUP_CHANNEL_ID, channelName, "");
	}

	/**
	 * return notification channel which connected with chanelId
	 *
	 * @param channelId - channel id
	 * @return notification channel connected with channelId. Or null if channel not exist. For Api less than 26 it always returns null
	 */
	@Nullable
	public NotificationChannel getNotificationChannel(String channelId) {
		return notificationManagerProxy.getNotificationChannel(channelId);
	}

	/**
	 * Check is notificationManager contains notification channel connected with channelId
	 *
	 * @param channelId - channel id
	 * @return true if notificationManager contains this channel id. For Api less than 26 it always return true
	 */
	public boolean hasNotificationChannel(String channelId) {
		return notificationManagerProxy.hasChannel(channelId);
	}

	/**
	 * Add led to notification. For Api large or equals 26 you can't change notification channel information after creating
	 *
	 * @param notification push notification
	 * @param color        led color
	 * @param ledOnMs      led on duration in ms
	 * @param ledOffMs     led off duration in ms
	 */
	public void addLED(Notification notification, int color, int ledOnMs, int ledOffMs) {
		notificationManagerProxy.addLED(notification, color, ledOnMs, ledOffMs);
	}

	/**
	 * Add sound to notification. For Api large or equals 26 you can't change notification channel information after creating
	 */
	public void addSound(Notification notification, Uri customSound, boolean isDefault) {
		notificationManagerProxy.addSound(notification, customSound, isDefault);
	}

	/**
	 * Add vibration to notification. For Api large or equals 26 you can't change notification channel information after creating
	 */
	public void addVibration(Notification notification, VibrateType vibrateType, boolean vibration) {
		notificationManagerProxy.addVibration(notification, vibrateType, vibration);
	}

	public void migrateGroupChannel() {
		notificationManagerProxy.migrateGroupChannel();
	}
}
