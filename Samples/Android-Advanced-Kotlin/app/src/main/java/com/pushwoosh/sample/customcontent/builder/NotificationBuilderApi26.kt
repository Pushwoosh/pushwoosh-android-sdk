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

package com.pushwoosh.sample.customcontent.builder

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.support.annotation.RequiresApi
import android.widget.RemoteViews

@RequiresApi(Build.VERSION_CODES.O)
internal class NotificationBuilderApi26(private val context: Context, channelId: String) : NotificationBuilder {
    private val builder: Notification.Builder = Notification.Builder(context, channelId)

    override fun setContentTitle(title: CharSequence): NotificationBuilder {
        builder.setContentTitle(title)
        return this
    }

    override fun setContentText(text: CharSequence): NotificationBuilder {
        builder.setContentText(text)
        return this
    }

    override fun setSmallIcon(smallIcon: Int): NotificationBuilder {
        builder.setSmallIcon(smallIcon)
        if (smallIcon == -1) {
            builder.setSmallIcon(AppIconHelper.getAppIcon(
                    context,
                    context.packageName)
            )
        }
        return this
    }

    override fun setTicker(ticker: CharSequence): NotificationBuilder {
        builder.setTicker(ticker)
        return this
    }

    override fun setWhen(time: Long): NotificationBuilder {
        builder.setWhen(time)
        return this
    }

    override fun setCustomContentView(view: RemoteViews): NotificationBuilder {
        builder.setCustomContentView(view)
                .setStyle(Notification.DecoratedCustomViewStyle())
        return this
    }

    override fun setStyle(bigPicture: Bitmap?, text: CharSequence): NotificationBuilder {
        val style: Notification.Style

        if (bigPicture != null) {
            //Images should be ? 450dp wide, ~2:1 aspect (see slide 52)
            //The image will be centerCropped
            //here: http://commondatastorage.googleapis.com/io2012/presentations/live%20to%20website/105.pdf
            style = Notification.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .setSummaryText(text)
        } else {
            style = Notification.BigTextStyle()
                    .bigText(text)
        }

        builder.setStyle(style)
        return this
    }

    override fun setColor(iconBackgroundColor: Int?): NotificationBuilder {
        if (iconBackgroundColor != null) {
            builder.setColor(iconBackgroundColor)
        }
        return this
    }

    override fun setLargeIcon(largeIcon: Bitmap): NotificationBuilder {
        if (null != largeIcon) {
            builder.setLargeIcon(largeIcon)
        }
        return this
    }

    override fun setPriority(priority: Int): NotificationBuilder {
        builder.setPriority(priority)
        return this
    }

    override fun setVisibility(visibility: Int): NotificationBuilder {
        builder.setVisibility(visibility)
        return this
    }

    override fun addAction(icon: Int, title: CharSequence, intent: PendingIntent): NotificationBuilder {
        builder.addAction(Notification.Action(icon, title, intent))
        return this
    }

    override fun setLed(arg: Int, ledOnMs: Int, ledOffMs: Int): NotificationBuilder {
        return this
    }

    override fun build(): Notification {
        return builder.build()
    }
}