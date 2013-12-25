//
//  MessageActivity.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.arellomobile.android.push;

import android.content.Intent;
import android.util.Log;

import com.amazon.device.messaging.ADMMessageHandlerBase;
import com.amazon.device.messaging.ADMMessageReceiver;

public class PushAmazonIntentService extends ADMMessageHandlerBase
{
	private static final String TAG = "AmazonIntentService";

	/**
	 * The MessageAlertReceiver class listens for messages from ADM and forwards them to the
	 * SampleADMMessageHandler class.
	 */
	public static class MessageAlertReceiver extends ADMMessageReceiver
	{
		/** {@inheritDoc} */
		public MessageAlertReceiver()
		{
			super(PushAmazonIntentService.class);
		}
	}

	/**
	 * Class constructor.
	 */
	public PushAmazonIntentService()
	{
		super(PushAmazonIntentService.class.getName());
	}

	/**
	 * Class constructor, including the className argument.
	 *
	 * @param className The name of the class.
	 */
	public PushAmazonIntentService(final String className)
	{
		super(className);
	}

	@Override
	protected void onRegistered(String registrationId)
	{
		Log.i(TAG, "Device registered: regId = " + registrationId);
		DeviceRegistrar.registerWithServer(getApplicationContext(), registrationId);
	}

	@Override
	protected void onUnregistered(String registrationId)
	{
		Log.i(TAG, "Device unregistered");
		DeviceRegistrar.unregisterWithServer(getApplicationContext(), registrationId);
	}

	@Override
	protected void onMessage(Intent intent)
	{
		Log.i(TAG, "Received message");
		// notifies user
		PushServiceHelper.generateNotification(getApplicationContext(), intent);
	}

	@Override
	protected void onRegistrationError(String errorId)
	{
		Log.e(TAG, "Messaging registration error: " + errorId);
		PushEventsTransmitter.onRegisterError(getApplicationContext(), errorId);
	}
}

