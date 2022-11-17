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

package com.pushwoosh.internal.utils;

import java.util.Collection;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.PluginProvider;

public interface Config {
	/**
	 * @return Sets the Pushwoosh application ID
	 */
	String getAppId();

	/**
	 * @return Sets the Firebase project sender ID.
	 */
	String getProjectId();

	/**
	 * @return Sets logging level.
	 */
	String getLogLevel();

	/**
	 * @return Overrides the Pushwoosh server base url.
	 */
	String getRequestUrl();

	/**
	 * @return Custom NotificationServiceExtension.
	 */
	Class<?> getNotificationService();

	/**
	 * @return Custom NotificationFactory.
	 */
	Class<?> getNotificationFactory();

	/**
	 * @return Custom SummaryNotificationFactory.
	 */
	Class<?> getSummaryNotificationFactory();

	/**
	 * @return If true, notification will be grouped. If false, the last received notification will be displayed only.
	 */
	boolean isMultinotificationMode();

	/**
	 * @return If true, notification should unlock screen on arrive
	 */
	boolean isLightscreenNotification();

	/**
	 * @return If true, the SDK is allowed to send network requests to Pushwoosh servers.
	 */
	boolean isServerCommunicationAllowed();

	/**
	 * @return If true, SDK will add top margin to Rich Media view
	 */
	boolean shouldShowFullscreenRichMedia();

	/**
	 * @return If true, the SDK is allowed to collect and to send device OS version to Pushwoosh.
	 */
	boolean isCollectingDeviceOsVersionAllowed();

	/**
	 * @return If true, the SDK is allowed to collect and to send device locale to Pushwoosh.
	 */
	boolean isCollectingDeviceLocaleAllowed();


	/**
	 * @return If true, the SDK is allowed to collect and to send device model to Pushwoosh.
	 */
	boolean isCollectingDeviceModelAllowed();

	/**
	 * @return If true, the WorkManager is set to handle notifications.
	 */
	boolean handleNotificationsUsingWorkManager();

	/**
	 * @return Custom notification (small) icon resource name. If null, default application icon will be used.
	 */
	@IdRes
	int getNotificationIcon();

	/**
	 * @return Notification (small) icon background color.
	 */
	@ColorInt
	int getNotificationIconColor();

	/**
	 * @return Pushwoosh extension plugins
	 */
	@NonNull
	Collection<Plugin> getPlugins();

	/**
	 * @return plugin provider which return specific settings for each plugins such as Native, Cordova and other
	 */
	PluginProvider getPluginProvider();

    boolean getSendPushStatIfShowForegroundDisabled();

    String[] getTrustedPackageNames();
}
