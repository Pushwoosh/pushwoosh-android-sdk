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

package com.pushwoosh.repository;

import android.content.SharedPreferences;
import androidx.annotation.Nullable;

import android.text.TextUtils;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.internal.platform.utils.GeneralUtils;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.preference.PreferenceLongValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;

import java.util.Locale;

public class RegistrationPrefs implements RegistrationPrefsInterface {
	private static final String TAG = "RegistrationPrefs";

	private static final String OLD_BASE_API_URL = "https://cp.pushwoosh.com/json/1.3/";
	private static final String BASE_API_URL_FORMAT = "https://%s.api.pushwoosh.com/json/1.3/";

	private static final String PREFERENCE = "com.pushwoosh.registration";

	private static final String PROPERTY_APPLICATION_ID = "application_id";
	private static final String PROPERTY_PROJECT_ID = "project_id";

	private static final String PROPERTY_PUSH_TOKEN = "registration_id";
	private static final String PROPERTY_REGISTERED_ON_SERVER = "registered_on_server";
	private static final String PROPERTY_FORCE_REGISTER = "force_register";
	private static final String PROPERTY_LAST_REGISTRATION = "last_registration_change";
	private static final String PROPERTY_LAST_FIREBASE_TOKEN_REGISTRATION = "last_firebase_registration";
	private static final String PROPERTY_APP_VERSION = "app_version";
	private static final String PROPERTY_USER_ID = "user_id";
	private static final String PROPERTY_DEVICE_ID = "device_id";
	private static final String PROPERTY_LOG_LEVEL = "log_level";
	private static final String PROPERTY_SETTAGS_FAILED = "settags_failed";
	private static final String PROPERTY_BASE_URL = "pw_base_url";
	private static final String PROPERTY_IS_REGISTERED_FOR_NOTIFICATION = "pw_registered_for_push";
	private static final String PROPERTY_LANGUAGE = "pw_language";
	private static final String PROPERTY_DENIED_NOTIFICATIONS = "pw_user_denied_notification_permission";

	private static final String COMMUNICATION_ENABLE = "pw_communication_enable";
	private static final String REMOVE_ALL_DEVICE_DATA = "pw_remove_all_device_data";
	private static final String GDPR_ENABLE = "pw_gdpr_enable";
	private static final String HWID = "pw_hwid";
	private static final String API_TOKEN = "pw_api_token";

	private final PreferenceStringValue pushToken;
	private final PreferenceBooleanValue registeredOnServer;
	private final PreferenceStringValue projectId;
	private final PreferenceStringValue applicationId;
	private final PreferenceLongValue lastPushRegistration;
	private final PreferenceBooleanValue forceRegister;
	private final PreferenceStringValue userId;
	private final PreferenceStringValue deviceId;
	private final PreferenceStringValue logLevel;
	private final PreferenceBooleanValue setTagsFailed;
	private final PreferenceStringValue baseUrl;
	private final PreferenceBooleanValue communicationEnable;
	private final PreferenceBooleanValue removeAllDeviceData;
	private final PreferenceBooleanValue gdprEnable;
	private final PreferenceStringValue hwid;
	private final PreferenceStringValue apiToken;
	private final PreferenceStringValue language;

	private final Config config;
	private final DeviceRegistrar deviceRegistrar;
	private PreferenceBooleanValue registeredForPush;
	private PreferenceBooleanValue userDeniedNotificationPermission;

	RegistrationPrefs(Config config, DeviceRegistrar deviceRegistrar) {
		PWLog.noise("RegistrationPrefs()...");
		this.config = config;
		this.deviceRegistrar = deviceRegistrar;

		SharedPreferences preferences = AndroidPlatformModule.getPrefsProvider().providePrefs(PREFERENCE);

		applicationId = new PreferenceStringValue(preferences, PROPERTY_APPLICATION_ID, "");
		if (applicationId.get().isEmpty() && config.getAppId() != null) {
			applicationId.set(config.getAppId());
		}

		projectId = new PreferenceStringValue(preferences, PROPERTY_PROJECT_ID, "");
		if (projectId.get().isEmpty() && config.getProjectId() != null) {
			projectId.set(config.getProjectId());
		}

		pushToken = new PreferenceStringValue(preferences, PROPERTY_PUSH_TOKEN, "");
		PreferenceIntValue appVersion = new PreferenceIntValue(preferences, PROPERTY_APP_VERSION, 0);

		final String pushToken = this.pushToken.get();
		registeredForPush = new PreferenceBooleanValue(preferences, PROPERTY_IS_REGISTERED_FOR_NOTIFICATION, pushToken != null && !pushToken.isEmpty());
		forceRegister = new PreferenceBooleanValue(preferences, PROPERTY_FORCE_REGISTER, false);
		userDeniedNotificationPermission = new PreferenceBooleanValue(preferences, PROPERTY_DENIED_NOTIFICATIONS, false);

		int newVersion = GeneralUtils.getAppVersion();
		if (appVersion.get() != newVersion) {
			// Registration should be reset after application update:
			// http://stackoverflow.com/questions/11422806/why-do-gcm-docs-recommend-invalidating-registration-on-app-update
			PWLog.noise(TAG, "App version changed from " + appVersion.get() + " to " + newVersion + "; resetting registration id");
			this.pushToken.set("");
			appVersion.set(newVersion);
		}

		registeredOnServer = new PreferenceBooleanValue(preferences, PROPERTY_REGISTERED_ON_SERVER, false);

		lastPushRegistration = new PreferenceLongValue(preferences, PROPERTY_LAST_REGISTRATION, 0);
		userId = new PreferenceStringValue(preferences, PROPERTY_USER_ID, "");
		deviceId = new PreferenceStringValue(preferences, PROPERTY_DEVICE_ID, "");
		logLevel = new PreferenceStringValue(preferences, PROPERTY_LOG_LEVEL, config.getLogLevel());
		setTagsFailed = new PreferenceBooleanValue(preferences, PROPERTY_SETTAGS_FAILED, false);
		communicationEnable = new PreferenceBooleanValue(preferences, COMMUNICATION_ENABLE, true);
		removeAllDeviceData = new PreferenceBooleanValue(preferences, REMOVE_ALL_DEVICE_DATA, false);
		gdprEnable = new PreferenceBooleanValue(preferences, GDPR_ENABLE, true);

		// Not before applicationId setting!
		baseUrl = new PreferenceStringValue(preferences, PROPERTY_BASE_URL, "");
		baseUrl.set(computeBaseUrl(baseUrl.get()));

		hwid = new PreferenceStringValue(preferences, HWID, "");
		apiToken = new PreferenceStringValue(preferences, API_TOKEN, config.getApiToken());
		String defaultLocale = "en";
		language = new PreferenceStringValue(preferences, PROPERTY_LANGUAGE,
				config.isCollectingDeviceLocaleAllowed()
						? languageCode()
						: defaultLocale);

		PWLog.noise("RegistrationPrefs() done");
	}

	public PreferenceBooleanValue gdprEnable(){
		return gdprEnable;
	}

	public PreferenceBooleanValue communicationEnable(){
		return communicationEnable;
	}

	public PreferenceBooleanValue removeAllDeviceData(){
		return removeAllDeviceData;
	}

	public PreferenceStringValue applicationId() {
		return applicationId;
	}

	public PreferenceStringValue projectId() {
		return projectId;
	}

	public PreferenceStringValue pushToken() {
		return pushToken;
	}

	public PreferenceBooleanValue registeredOnServer() {
		return registeredOnServer;
	}

	@SuppressWarnings("WeakerAccess")
	public PreferenceLongValue lastPushRegistration() {
		return lastPushRegistration;
	}

	public PreferenceBooleanValue forceRegister() {
		return forceRegister;
	}

	public PreferenceStringValue userId() {
		return userId;
	}

	public PreferenceStringValue deviceId() {
		return deviceId;
	}

	public PreferenceStringValue logLevel() {
		return logLevel;
	}

	@SuppressWarnings("WeakerAccess")
	public PreferenceBooleanValue setTagsFailed() {
		return setTagsFailed;
	}

	public PreferenceStringValue baseUrl() {
		return baseUrl;
	}

	@Override
	public PreferenceStringValue hwid() {
		return hwid;
	}

	public PreferenceStringValue apiToken() { return apiToken; }

	public PreferenceStringValue language() {
		return language;
	}

	private String computeBaseUrl(String preferenceUrl) {
		String baseUrl = preferenceUrl;
		if (TextUtils.isEmpty(baseUrl) || baseUrl.startsWith("http://")) {
			baseUrl = getDefaultBaseUrl();
		}

		if (!baseUrl.endsWith("/")) {
			baseUrl += "/";
		}

		return baseUrl;
	}

	public String getDefaultBaseUrl() {
		String url = config.getRequestUrl();

		if (TextUtils.isEmpty(url)) {
			String appid = applicationId.get();
			if (!TextUtils.equals(appid, "") && !appid.contains(".")) {
				url = String.format(BASE_API_URL_FORMAT, appid);
			} else {
				url = OLD_BASE_API_URL;
			}
		}

		return url;
	}

	public void removeAppId() {
		applicationId().set("");
		baseUrl().set("");
		lastPushRegistration().set(0);
		setTagsFailed().set(false);
		registeredOnServer.set(false);
	}

	public void setAppId(final String appId) {
		applicationId().set(appId);
		baseUrl().set(getDefaultBaseUrl());
	}

	public void setLanguage(@Nullable String language) {
		if (language == null ) {
			return;
		}

		language().set(language);

		lastPushRegistration().set(0);
		deviceRegistrar.updateRegistration();
	}

	public void setApiToken(String apiToken) {
		if (apiToken == null) {
			return;
		}
		apiToken().set(apiToken);
	}

	public void removeSenderId() {
		clearSenderIdInfo();
		projectId().set("");
	}

	public void clearSenderIdInfo() {
		pushToken().set("");
		lastPushRegistration().set(0);
	}

	public PreferenceBooleanValue isRegisteredForPush() {
		return registeredForPush;
	}

	public PreferenceBooleanValue hasUserDeniedNotificationPermission() {
		return userDeniedNotificationPermission;
	}
	/**
	 * Create {@link com.pushwoosh.internal.platform.prefs.migration.MigrationScheme} associated with this class.
	 * Don't forget add field here if it will be added to this class
	 * @param prefsProvider - prefsProvider which will provide prefs for migrationScheme
	 * @return MigrationScheme to correct migration from one prefs to another
	 */
	static MigrationScheme provideMigrationScheme(PrefsProvider prefsProvider) {
		MigrationScheme migrationScheme = new MigrationScheme(PREFERENCE);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_APPLICATION_ID);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_PROJECT_ID);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_PUSH_TOKEN);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.INT, PROPERTY_APP_VERSION);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.BOOLEAN, PROPERTY_REGISTERED_ON_SERVER);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.LONG, PROPERTY_LAST_REGISTRATION);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.LONG, PROPERTY_LAST_FIREBASE_TOKEN_REGISTRATION);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_USER_ID);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_DEVICE_ID);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_LOG_LEVEL);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_BASE_URL);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.BOOLEAN, PROPERTY_SETTAGS_FAILED);

		final SharedPreferences sharedPreferences = prefsProvider.providePrefs(PREFERENCE);
		if (sharedPreferences == null) {
			return migrationScheme;
		}
		if (sharedPreferences.contains(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION)) {
			migrationScheme.putBoolean(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION, sharedPreferences.getBoolean(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION, false));
		} else {
			final String token = sharedPreferences.getString(PROPERTY_PUSH_TOKEN, "");
			migrationScheme.putBoolean(PROPERTY_IS_REGISTERED_FOR_NOTIFICATION, !TextUtils.isEmpty(token));
		}
		return migrationScheme;
	}

	private String languageCode() {
		//workaround to support simplified and traditional chinese on backend: https://jira.corp.pushwoosh.com/browse/PUSH-33298
		return Locale.getDefault().getLanguage().equals("zh")
				? Locale.getDefault().toLanguageTag()
				: Locale.getDefault().getLanguage();
	}
}
