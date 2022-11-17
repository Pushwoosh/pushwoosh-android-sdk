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

//
//  MessageActivity.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.pushwoosh;

import android.content.Intent;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.pushwoosh.internal.utils.NotificationRegistrarHelper;
import com.pushwoosh.internal.utils.PWLog;

public class PushAmazonIntentService extends ADMMessageHandlerBase {
	private static final String TAG = "AmazonIntentService";

	/**
	 * Class constructor.
	 */
	public PushAmazonIntentService() {
		super(PushAmazonIntentService.class.getName());
	}

	/**
	 * Class constructor, including the className argument.
	 *
	 * @param className The name of the class.
	 */
	public PushAmazonIntentService(final String className) {
		super(className);
	}

	@Override
	protected void onRegistered(String registrationId) {
		PWLog.info(TAG, "Device registered: regId = " + registrationId);

		NotificationRegistrarHelper.onRegisteredForRemoteNotifications(registrationId);
	}

	@Override
	protected void onUnregistered(String registrationId) {
		PWLog.info(TAG, "Device unregistered");

		NotificationRegistrarHelper.onUnregisteredFromRemoteNotifications(registrationId);
	}

	@Override
	protected void onMessage(Intent intent) {
		PWLog.info(TAG, "Received message");

		NotificationRegistrarHelper.handleMessage(intent.getExtras());
	}

	@Override
	protected void onRegistrationError(String errorId) {
		PWLog.error(TAG, "Messaging registration error: " + errorId);

		NotificationRegistrarHelper.onFailedToRegisterForRemoteNotifications(errorId);
	}
}
