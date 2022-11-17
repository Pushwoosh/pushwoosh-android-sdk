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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

import static com.pushwoosh.notification.channel.NotificationChannelInfoProvider.getChannelImportance;
import static com.pushwoosh.repository.NotificationPrefs.DEFAULT_GROUP_CHANNEL_ID;
import static com.pushwoosh.repository.NotificationPrefs.DEFAULT_GROUP_CHANNEL_NAME;
import static com.pushwoosh.repository.NotificationPrefs.DEPRECATED_GROUP_CHANNEL_NAME;

@TargetApi(Build.VERSION_CODES.O)
class NotificationManagerProxyApi26 implements NotificationManagerProxy {

	private final NotificationManager notificationManager;

	NotificationManagerProxyApi26(NotificationManager notificationManager) {
		this.notificationManager = notificationManager;
	}

	@Override
	public boolean hasChannel(String id) {
		return notificationManager.getNotificationChannel(id) != null;
	}

	@Override
	public NotificationChannel getNotificationChannel(String channelId) {
		return notificationManager.getNotificationChannel(channelId);
	}

	@Override
	public void addChannel(String channelId, String channelName, String channelDescription, PushMessage pushMessage) {
		Builder notificationChannelBuilder = new Builder();
		if (pushMessage.getLed() != null) {
			notificationChannelBuilder.setLightColor(pushMessage.getLed());
			notificationChannelBuilder.setLightScreenOn(true);
		}

		if (pushMessage.getSound() != null) {
			Uri customSound = NotificationUtils.getSoundUri(pushMessage.getSound());

			if (customSound != null) {
				notificationChannelBuilder.setSound(customSound);
			}
		}

		notificationChannelBuilder.setImportance(getChannelImportance(pushMessage));
		notificationChannelBuilder.setVibration(pushMessage.getVibration());

		notificationManager.createNotificationChannel(notificationChannelBuilder.build(channelId, channelName, channelDescription));
	}

	@Override
	public String addDefaultGroupChannel(String channelId, String channelName, String channelDescription) {
		Builder notificationChannelBuilder = new Builder();
		NotificationChannel channel = notificationChannelBuilder.build(channelId, channelName, channelDescription);
		channel.setSound(null, null);
		channel.enableVibration(false);
		notificationManager.createNotificationChannel(channel);
		return channelId;
	}


	@Override
	public void migrateGroupChannel() {
		if (notificationManager.getNotificationChannel(DEPRECATED_GROUP_CHANNEL_NAME) != null) {
			notificationManager.deleteNotificationChannel(DEPRECATED_GROUP_CHANNEL_NAME);
		}
		addDefaultGroupChannel(DEFAULT_GROUP_CHANNEL_ID, DEFAULT_GROUP_CHANNEL_NAME, "");
	}


	@Override
	public void addLED(Notification notification, int color, int ledOnMs, int ledOffMs) {
		//https://developer.android.com/guide/topics/ui/notifiers/notifications.html#UpdateChannel
		//After notification channel created, the user is in charge of its settings and behavior.
	}

	@Override
	public void addSound(Notification notification, Uri customSound, boolean isDefault) {
		//https://developer.android.com/guide/topics/ui/notifiers/notifications.html#UpdateChannel
		//After notification channel created, the user is in charge of its settings and behavior.
	}

	@Override
	public void addVibration(Notification notification, VibrateType vibrateType, boolean vibration) {
		//https://developer.android.com/guide/topics/ui/notifiers/notifications.html#UpdateChannel
		//After notification channel created, the user is in charge of its settings and behavior.
	}

	private static class Builder {
		private int lightColor = -1;
		private Uri sound;
		private boolean vibration;
		private boolean lightScreenOn;
		private int importance = NotificationManager.IMPORTANCE_DEFAULT;


		public Builder() {
			final NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
			setLightColor(notificationPrefs.ledColor().get())
					.setVibration(!notificationPrefs.vibrateType().get().equals(VibrateType.NO_VIBRATE))
					.setLightScreenOn(notificationPrefs.lightScreenOn().get());
		}

		public Builder(NotificationChannel notificationChannel) {
			setSound(notificationChannel.getSound())
					.setVibration(notificationChannel.getVibrationPattern() != null && notificationChannel.getVibrationPattern().length != 0)
					.setLightColor(notificationChannel.getLightColor())
					.setLightScreenOn(notificationChannel.shouldShowLights());
		}

		Builder setLightColor(int ledColor) {
			this.lightColor = ledColor;
			return this;
		}

		Builder setSound(Uri sound) {
			this.sound = sound;
			return this;
		}

		Builder setVibration(boolean vibration) {
			this.vibration = vibration;
			return this;
		}

		Builder setLightScreenOn(boolean lightScreenOn) {
			this.lightScreenOn = lightScreenOn;
			return this;
		}

		Builder setImportance(int importance) {
			this.importance = importance;
			return this;
		}

		NotificationChannel build(String channelId, String channelName, String channelDescription) {
			NotificationChannel notificationChannel = new NotificationChannel(channelId, channelName, importance);

			if (vibration) {
				if (NotificationUtils.phoneHaveVibratePermission()) {
					notificationChannel.enableVibration(true);
					notificationChannel.setVibrationPattern(NotificationChannelInfoProvider.getVibrationPattern());
				}
			} else {
				notificationChannel.enableVibration(false);
				notificationChannel.setVibrationPattern(null);
			}

			notificationChannel.enableLights(lightScreenOn);
			notificationChannel.setLightColor(lightColor);

			if (sound != null) {
				AudioAttributes attributes = new AudioAttributes.Builder()
						.setUsage(AudioAttributes.USAGE_NOTIFICATION)
						.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
						.build();

				notificationChannel.setSound(sound, attributes);
			}

			notificationChannel.setDescription(channelDescription);

			return notificationChannel;
		}
	}
}
