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
import android.net.Uri;

import com.pushwoosh.notification.PushMessage;
import com.pushwoosh.notification.VibrateType;

interface NotificationManagerProxy {

	boolean hasChannel(String id);

	NotificationChannel getNotificationChannel(String channelId);

	void addChannel(String channelId, String channelName, String channelDescription, PushMessage pushMessage);

	String addDefaultGroupChannel(String channelId, String channelName, String channelDescription);

	void addLED(Notification notification, int color, int ledOnMs, int ledOffMs);

	void addSound(Notification notification, Uri customSound, boolean isDefault);

	void addVibration(Notification notification, VibrateType vibrateType, boolean vibration);

	void migrateGroupChannel();
}
