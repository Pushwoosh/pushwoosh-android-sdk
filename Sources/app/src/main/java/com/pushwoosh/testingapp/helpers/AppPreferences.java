package com.pushwoosh.testingapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Spinner;

import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.testingapp.AppData;
import com.pushwoosh.testingapp.AppPreferencesStrings;
import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

/**
 * Created by etkachenko on 5/18/17.
 */

public class AppPreferences {
	private static Context context;
	private static final AppData APP_DATA = AppData.getInstance();
	private static AppPreferences appPreferences;
	private static SharedPreferences preferences;


	public static void initAppPreferences(Context appContext) {
		if (appPreferences == null) {
			appPreferences = new AppPreferences();
			context = appContext;
			preferences = context.getSharedPreferences(AppPreferencesStrings.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
			APP_DATA.setCustomNotifications(preferences.getBoolean(AppPreferencesStrings.SET_CUSTOM_NOTIFICATION_FACTORY_SWITCH, false));
			APP_DATA.setHandleInForeground(preferences.getBoolean(AppPreferencesStrings.BASE_PUSH_MESSAGE_RECEIVER_SWITCH, false));
			APP_DATA.setFirstRun(!preferences.getBoolean(AppPreferencesStrings.PREFERENCES_EXIST, false));
		}
	}

	public static void setDefaults() {
		if (APP_DATA.getFirstRun()) {
			PushwooshProxyController.getPushwooshProxy().setSimpleNotificationMode();
			PushwooshProxyController.getPushwooshProxy().setSoundNotificationType(SoundType.fromInt(0));
			PushwooshProxyController.getPushwooshProxy().setVibrateNotificationType(VibrateType.fromInt(0));
			PushwooshProxyController.getPushwooshProxy().setLightScreenOnNotification(false);
			PushwooshProxyController.getPushwooshProxy().setEnableLED(false);
			PushwooshProxyController.getPushwooshProxy().stopTrackingGeoPushes();
			APP_DATA.setCustomNotifications(false);
			APP_DATA.setPostEventAttributes(false);
			APP_DATA.setHandleInForeground(false);
		}
	}

	public static void loadBool(String name, SwitchCompat switcher) {
		Boolean savedBool = getSavedBool(name);
		switcher.setChecked(savedBool);
	}

	public static Boolean getSavedBool(String name) {
		Boolean savedBool = preferences.getBoolean(name, false);
		return savedBool;
	}

	public static void loadInt(String name, Spinner spinner) {
		Integer savedInt = preferences.getInt(name, 0);
		spinner.setSelection(savedInt);
	}

	public static void saveBool(String name, Boolean value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(name, value);
		editor.apply();
	}

	public static void saveInt(String name, Integer value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(name, value);
		editor.apply();
	}

	public static Context getContext() {
		return context;
	}
}
