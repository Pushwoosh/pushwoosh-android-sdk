package com.pushwoosh.testingapp;

import android.app.Application;

import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.testingapp.helpers.AppPreferences;
import com.pushwoosh.testingapp.helpers.ShowMessageHelper;
import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

public class TestingApp extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

//		RichMediaManager.setDelegate(new DefaultRichMediaPresentingDelegate());
		ShowMessageHelper.initShowMessageHelper(getApplicationContext());
		PWLog.setLogsUpdateListener(LogsBuffer.instance);
		AppPreferences.initAppPreferences(getApplicationContext());

		PushwooshProxyController.getPushwooshProxy().initPushwoosh(this);
		InAppManager.getInstance().postEvent(AppPreferencesStrings.APP_OPEN);
	}
}
