//
//  DeviceRegistrar.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed

package com.arellomobile.android.push;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.arellomobile.android.push.request.RegisterDeviceRequest;
import com.arellomobile.android.push.request.RequestManager;
import com.arellomobile.android.push.request.UnregisterDeviceRequest;
import com.arellomobile.android.push.utils.PreferenceUtils;
import com.google.android.gcm.GCMRegistrar;

import java.util.Date;

/**
 * Register/unregister with the App server.
 */
public class DeviceRegistrar
{
	private static final String TAG = "DeviceRegistrar";

	static void registerWithServer(final Context context, final String deviceRegistrationID)
	{
		Log.w(TAG, "Registering for pushes");

		RegisterDeviceRequest request = new RegisterDeviceRequest(deviceRegistrationID);
		try
		{
			RequestManager.sendRequest(context, request);

			GCMRegistrar.setRegisteredOnServer(context, true);
			PushEventsTransmitter.onRegistered(context, deviceRegistrationID);
			PreferenceUtils.setLastRegistration(context, new Date().getTime());
			Log.w(TAG, "Registered for pushes: " + deviceRegistrationID);
		}
		catch (Exception e)
		{
			if (e.getMessage() != null)
			{
				PushEventsTransmitter.onRegisterError(context, e.getMessage());
				Log.e(TAG, "Registration error " + e.getMessage(), e);
			}
			else
			{
				String err = request.getRawResponse();
				Log.e(TAG, "Registration error " + err);
				PushEventsTransmitter.onRegisterError(context, err);
			}
		}
	}

	static void unregisterWithServer(final Context context, final String deviceRegistrationID)
	{
		Log.w(TAG, "Try To Unregistered for pushes");
		GCMRegistrar.setRegisteredOnServer(context, false);

		UnregisterDeviceRequest request = new UnregisterDeviceRequest();

		try
		{
			RequestManager.sendRequest(context, request);

			PushEventsTransmitter.onUnregistered(context, deviceRegistrationID);
			Log.w(TAG, "Unregistered for pushes: " + deviceRegistrationID);
			PreferenceUtils.resetLastRegistration(context);
		}
		catch (Exception e)
		{
			if (!TextUtils.isEmpty(e.getMessage()))
			{
				PushEventsTransmitter.onUnregisteredError(context, e.getMessage());
				Log.e(TAG, "Unregistration error " + e.getMessage(), e);
			}
			else
			{
				String err = request.getRawResponse();
				PushEventsTransmitter.onUnregisteredError(context, err);
				Log.e(TAG, "Unregistration error " + err);
			}
		}
	}
}
