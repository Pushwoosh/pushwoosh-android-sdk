package com.arellomobile.android.push.registrar;

import com.arellomobile.android.push.utils.GeneralUtils;
import com.arellomobile.android.push.utils.PreferenceUtils;
import com.google.android.gcm.GCMRegistrar;

import android.content.Context;

public class PushRegistrarGCM implements PushRegistrar {
	
	public PushRegistrarGCM(Context context)
	{}

	@Override
	public void checkDevice(Context context)
	{
		String appId = PreferenceUtils.getApplicationId(context);
		String senderId = PreferenceUtils.getSenderId(context);

		GeneralUtils.checkNotNullOrEmpty(appId, "mAppId");
		GeneralUtils.checkNotNullOrEmpty(senderId, "mSenderId");

		// Make sure the device has the proper dependencies.
		GCMRegistrar.checkDevice(context);
		// Make sure the manifest was properly set - comment out this line
		// while developing the app, then uncomment it when it's ready.
		GCMRegistrar.checkManifest(context);
	}
	
	@Override
	public void registerPW(Context context)
	{
		String senderId = PreferenceUtils.getSenderId(context);
		GCMRegistrar.register(context, senderId);
	}
	
	@Override
	public void unregisterPW(Context context)
	{
		GCMRegistrar.unregister(context);
	}
}
