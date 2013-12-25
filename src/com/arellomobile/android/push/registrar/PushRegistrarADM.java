package com.arellomobile.android.push.registrar;

import android.content.Context;

import com.amazon.device.messaging.ADM;
import com.arellomobile.android.push.ADMRegistrar;
import com.arellomobile.android.push.utils.GeneralUtils;
import com.arellomobile.android.push.utils.PreferenceUtils;

public class PushRegistrarADM implements PushRegistrar {
	
	private final ADM mAdm;
	
	public PushRegistrarADM(Context context)
	{
		mAdm = new ADM(context);
	}
	
	@Override
	public void checkDevice(Context context)
	{
		String appId = PreferenceUtils.getApplicationId(context);
		GeneralUtils.checkNotNullOrEmpty(appId, "mAppId");

		// Make sure the device has the proper dependencies.
		ADMRegistrar.checkDevice(mAdm);
		// Make sure the manifest was properly set - comment out this line
		// while developing the app, then uncomment it when it's ready.
		ADMRegistrar.checkManifest(context);
	}
	
	@Override
	public void registerPW(Context context)
	{
		mAdm.startRegister();
	}
	
	@Override
	public void unregisterPW(Context context)
	{
		mAdm.startUnregister();
	}
}
