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

package com.pushwoosh.firebase;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.PushwooshMessagingServiceHelper;
import com.pushwoosh.firebase.internal.RemoteMessageUtils;
import com.pushwoosh.firebase.internal.mapper.RemoteMessageMapper;
import com.pushwoosh.firebase.internal.registrar.FcmRegistrar;
import com.pushwoosh.firebase.internal.specific.FcmDeviceSpecificIniter;
import com.pushwoosh.firebase.internal.utils.FirebaseTokenHelper;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.specific.DeviceSpecificProvider;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.RepositoryModule;

import java.util.Date;
import java.util.Map;

import androidx.annotation.Nullable;

@SuppressWarnings("WeakerAccess")
public class PushwooshFcmHelper {
	private static final String TAG = "FcmHelper";

	/**
	 * if you use custom {@link FirebaseMessagingService}
	 * call this method when {@link FirebaseMessagingService#onNewToken(String token)} is invoked
	 */
	public static void onTokenRefresh(@Nullable String token) {
		RepositoryModule.getRegistrationPreferences().lastPushRegistration().set(new Date().getTime());
		Context context = AndroidPlatformModule.getApplicationContext();
		if (context == null) {
			PWLog.error(AndroidPlatformModule.NULL_CONTEXT_MESSAGE);
			return;
		}

		if (DeviceSpecificProvider.getInstance().pushRegistrar() instanceof FcmRegistrar) {
			try {
				PWLog.debug(TAG, "onTokenRefresh");
				if (token != null && token.equals(RepositoryModule.getRegistrationPreferences().pushToken().get())) {
					return;
				}

				PushwooshMessagingServiceHelper.onTokenRefresh(token);
			} catch (Exception e) {
				String error = e.getMessage();
				PWLog.error("PushwooshFcmHelper", "FCM registration error:" + error);
			}
		}
	}

	/**
	 * if you use custom {@link com.google.firebase.messaging.FirebaseMessagingService}
	 * call this method when {@link com.google.firebase.messaging.FirebaseMessagingService#onMessageReceived(RemoteMessage)} is invoked
	 *
	 * @return true if the remoteMessage was sent via Pushwoosh and was successfully processed; otherwise false
	 */
	@SuppressWarnings("UnusedReturnValue")
	public static boolean onMessageReceived(Context context, RemoteMessage remoteMessage) {

		//Fix for PUSH-32760
		try {
			if (DeviceSpecificProvider.getInstance() == null) {
				new DeviceSpecificProvider.Builder()
						.setDeviceSpecific(FcmDeviceSpecificIniter.create())
						.build(true);
			}

			if (!isPushwooshMessage(remoteMessage) || !DeviceSpecificProvider.getInstance().isFirebase()) {
				return false;
			}
		} catch (NullPointerException e) {
			PWLog.error("Firebase provider is not initialized, unsafe to handle received push");
			return false;
		}

		String from = remoteMessage.getFrom();
		Map<String, String> data = remoteMessage.getData();

		PWLog.info(TAG, "Received message: " + data.toString() + " from: " + from);

		Bundle pushBundle = RemoteMessageMapper.mapToBundle(remoteMessage);

		return PushwooshMessagingServiceHelper.onMessageReceived(context, pushBundle);
	}

	/**
	 * Check if the remoteMessage was sent via Pushwoosh
	 *
	 * @return true if remoteMessage was sent via Pushwoosh
	 */
	public static boolean isPushwooshMessage(RemoteMessage remoteMessage) {
		return RemoteMessageUtils.isPushwooshMessage(remoteMessage);
	}

	/**
	 * Convert RemoteMessage to Bundle object
	 *
	 * @param remoteMessage - message received from Firebase
	 * @return Bundle created from RemoteMessage
	 */
	public static Bundle messageToBundle(RemoteMessage remoteMessage) {
		return RemoteMessageMapper.mapToBundle(remoteMessage);
	}
}
