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
import android.graphics.Bitmap
import android.widget.RemoteViews

interface NotificationBuilder {
    fun setContentTitle(title: CharSequence): NotificationBuilder

    fun setContentText(text: CharSequence): NotificationBuilder

    fun setSmallIcon(smallIcon: Int): NotificationBuilder

    fun setTicker(ticker: CharSequence): NotificationBuilder

    fun setWhen(time: Long): NotificationBuilder

    fun setStyle(bigPicture: Bitmap?, text: CharSequence): NotificationBuilder

    fun setColor(iconBackgroundColor: Int?): NotificationBuilder

    fun setLargeIcon(largeIcon: Bitmap): NotificationBuilder

    fun setCustomContentView(view: RemoteViews): NotificationBuilder

    fun setPriority(priority: Int): NotificationBuilder

    fun setVisibility(visibility: Int): NotificationBuilder

    fun addAction(icon: Int, title: CharSequence, intent: PendingIntent): NotificationBuilder

    fun setLed(arg: Int, ledOnMs: Int, ledOffMs: Int): NotificationBuilder

    fun build(): Notification
}
