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

package com.pushwoosh.sample.customcontent.builder;

import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;

@SuppressWarnings("UnusedReturnValue")
public interface NotificationBuilder {
	NotificationBuilder setContentTitle(CharSequence title);

	NotificationBuilder setContentText(CharSequence text);

	NotificationBuilder setSmallIcon(int smallIcon);

	NotificationBuilder setTicker(CharSequence ticker);

	NotificationBuilder setWhen(long time);

	NotificationBuilder setStyle(@Nullable Bitmap bigPicture, CharSequence text);

	NotificationBuilder setColor(Integer iconBackgroundColor);

	NotificationBuilder setLargeIcon(Bitmap largeIcon);

	NotificationBuilder setCustomContentView(RemoteViews view);

	NotificationBuilder setPriority(int priority);

	NotificationBuilder setVisibility(int visibility);

	NotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent);

	NotificationBuilder setLed(int arg, int ledOnMs, int ledOffMs);

	Notification build();
}
