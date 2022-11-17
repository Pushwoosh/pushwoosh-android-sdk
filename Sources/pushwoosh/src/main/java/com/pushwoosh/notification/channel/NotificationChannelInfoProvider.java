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

import android.app.NotificationManager;
import android.os.Build;

import com.pushwoosh.notification.PushBundleDataProvider;
import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.repository.RepositoryModule;

public final class NotificationChannelInfoProvider {
	private static final String CHANNEL_ID_PREFIX = "pushwoosh_";
	private static final long[] DEFAULT_VIBRATION_PATTERN = new long[]{0, 150, 50, 150};

	private NotificationChannelInfoProvider() {/*do nothing*/}

	static String channelId(String channelName) {
		if (!channelName.contains(" ") && channelName.startsWith(CHANNEL_ID_PREFIX)) {
			return channelName;
		}

		return CHANNEL_ID_PREFIX + channelName.trim().replaceAll("\\s+", "_").toLowerCase();
	}

	public static String getChannelName(PushMessage pushData) {
		final String notificationChannel = PushBundleDataProvider.getNotificationChannel(pushData.toBundle());
		return notificationChannel == null ? RepositoryModule.getNotificationPreferences().channelName().get() : notificationChannel;
	}

	static int getChannelImportance(PushMessage pushMessage) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
			return pushMessage.getPriority();
		}

		switch (pushMessage.getPriority()) {
			case -2:
			case -1:
				return NotificationManager.IMPORTANCE_LOW;
			case 0:
				return NotificationManager.IMPORTANCE_DEFAULT;
			case 1:
			case 2:
				return NotificationManager.IMPORTANCE_HIGH;
			default:
				return NotificationManager.IMPORTANCE_UNSPECIFIED;
		}
	}

	static long[] getVibrationPattern() {
		return DEFAULT_VIBRATION_PATTERN;
	}
}
