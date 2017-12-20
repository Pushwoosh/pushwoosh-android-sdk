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
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

@RequiresApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
class NotificationBuilderApi14 implements NotificationBuilder {
	private final NotificationCompat.Builder builder;
	private final Context context;

	NotificationBuilderApi14(Context context, String channelId) {
		builder = new NotificationCompat.Builder(context, channelId);
		this.context = context;
	}

	@Override
	public NotificationBuilder setContentTitle(CharSequence title) {
		builder.setContentTitle(title);
		return this;
	}

	@Override
	public NotificationBuilder setContentText(CharSequence text) {
		builder.setContentText(text);
		return this;
	}

	@Override
	public NotificationBuilder setSmallIcon(int smallIcon) {
		if (smallIcon == -1) {
			ApplicationInfo applicationInfo = context.getApplicationInfo();
			smallIcon = applicationInfo == null ? -1 : applicationInfo.icon;
		}

		if (smallIcon == -1) {
			return this;
		}

		builder.setSmallIcon(smallIcon);
		return this;
	}

	@Override
	public NotificationBuilder setTicker(CharSequence ticker) {
		builder.setTicker(ticker);
		return this;
	}

	@Override
	public NotificationBuilder setWhen(long time) {
		builder.setWhen(time);
		return this;
	}

	@Override
	public NotificationBuilder setStyle(@Nullable Bitmap bigPicture, CharSequence text) {
		final NotificationCompat.Style style;

		if (bigPicture != null) {
			//Images should be ? 450dp wide, ~2:1 aspect (see slide 52)
			//The image will be centerCropped
			//here: http://commondatastorage.googleapis.com/io2012/presentations/live%20to%20website/105.pdf
			style = new NotificationCompat.BigPictureStyle()
					.bigPicture(bigPicture)
					.setSummaryText(text);
		} else {
			style = new NotificationCompat.BigTextStyle()
					.bigText(text);
		}

		builder.setStyle(style);
		return this;
	}

	@Override
	public NotificationBuilder setCustomContentView(RemoteViews view) {
		builder.setCustomContentView(view)
				.setStyle(new NotificationCompat.DecoratedCustomViewStyle());

		return this;
	}

	@Override
	public NotificationBuilder setColor(Integer iconBackgroundColor) {
		if (iconBackgroundColor != null) {
			builder.setColor(iconBackgroundColor);
		}
		return this;
	}

	@Override
	public NotificationBuilder setLargeIcon(Bitmap largeIcon) {
		if (null != largeIcon) {
			builder.setLargeIcon(largeIcon);
		}
		return this;
	}

	@Override
	public NotificationBuilder setPriority(int priority) {
		builder.setPriority(priority);
		return this;
	}

	@Override
	public NotificationBuilder setVisibility(int visibility) {
		builder.setVisibility(visibility);
		return this;
	}

	@Override
	public NotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent) {
		builder.addAction(new NotificationCompat.Action(icon, title, intent));
		return this;
	}

	@Override
	public NotificationBuilder setLed(int arg, int ledOnMs, int ledOffMs) {
		builder.setLights(arg, ledOnMs, ledOffMs);
		return this;
	}

	@Override
	public Notification build() {
		return builder.build();
	}
}
