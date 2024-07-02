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

package com.pushwoosh;

import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;

import com.pushwoosh.internal.NativePluginProvider;
import com.pushwoosh.internal.Plugin;
import com.pushwoosh.internal.PluginProvider;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.FileUtils;
import com.pushwoosh.internal.utils.PWLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Encapsulates all pushwoosh metadata parameters in AndroidManifest.xml
 */
class AndroidManifestConfig implements Config {
	private static final String TAG = "Config";
	private String appId = null;
	private String projectId = null;
	private String xiaomiAppId = null;
	private String xiaomiAppKey = null;
	private String xiaomiAppRegion = null;
	private String logLevel = null;
	private String requestUrl = null;
	private String[] trustedPackageNames = {};
	private Class<?> notificationService;
	private Class<?> notificationFactory;
	private Class<?> summaryNotificationFactory;
	private boolean lazySdkInitialization = false;
	private boolean multinotificationMode = false;
	private boolean lightscreenNotification = false;
	private boolean sendPushStatIfShowForegroundDisabled = false;
	private boolean isServerCommunicationAllowed = true;
	private boolean showPushnotificationAlert = false;
	private boolean handleNotificationsUsingWorkManager = false;
	private boolean isCollectingDeviceOsVersionAllowed = true;
	private boolean isCollectingDeviceLocaleAllowed = true;
	private boolean isCollectingDeviceModelAllowed = true;
	private boolean shouldShowFullscreenRichMedia = false;

	@IdRes
	private int notificationIcon = 0;

	@ColorInt
	private int notificationIconColor = 0;

	private final List<Plugin> plugins = new ArrayList<>();
	private PluginProvider pluginProvider;

	AndroidManifestConfig() {
		ApplicationInfo applicationInfo = AndroidPlatformModule.getAppInfoProvider().getApplicationInfo();
		if (applicationInfo == null || applicationInfo.metaData == null) {
			PWLog.warn(TAG, "no metadata found");
			return;
		}

		appId = getString(applicationInfo.metaData, "com.pushwoosh.appid", "PW_APPID");
		projectId = getString(applicationInfo.metaData, "com.pushwoosh.senderid", "PW_PROJECT_ID");
		xiaomiAppId = getString(applicationInfo.metaData, "com.pushwoosh.xiaomiappid", "XM_APPID");
		xiaomiAppKey = getString(applicationInfo.metaData, "com.pushwoosh.xiaomiappkey", "XM_APPKEY");
		xiaomiAppRegion = getString(applicationInfo.metaData, "com.pushwoosh.xiaomiappregion", "XM_APPREGION");

		String trustedPackagesString = getString(applicationInfo.metaData, "com.pushwoosh.trusted_package_names", null);
		if (!TextUtils.isEmpty(trustedPackagesString)) {
			trustedPackageNames = trustedPackagesString.split(",");
		}
		if (trustedPackageNames.length > 0) {
			for (int i = 0; i < trustedPackageNames.length; ++i) {
				trustedPackageNames[i] = trustedPackageNames[i].trim();
			}
		}

		if (!TextUtils.isEmpty(projectId)) {
			// remove extra "A" or " " before number (is used to trick AndroidManifest.xml that value is string)
			if (!Character.isDigit(projectId.charAt(0))) {
				projectId = projectId.substring(1);
			}
		}

		logLevel = getString(applicationInfo.metaData, "com.pushwoosh.log_level", "PW_LOG_LEVEL");
		requestUrl = getString(applicationInfo.metaData, "com.pushwoosh.base_url", "PushwooshUrl");

		notificationService = getClass(applicationInfo.metaData, "com.pushwoosh.notification_service_extension");
		notificationFactory = getClass(applicationInfo.metaData, "com.pushwoosh.notification_factory");
		summaryNotificationFactory = getClass(applicationInfo.metaData, "com.pushwoosh.summary_notification_factory");

		lazySdkInitialization = applicationInfo.metaData.getBoolean("com.pushwoosh.lazy_initialization", false);
		multinotificationMode = applicationInfo.metaData.getBoolean("com.pushwoosh.multi_notification_mode", false);
		lightscreenNotification = applicationInfo.metaData.getBoolean("com.pushwoosh.light_screen_notification", false);
		sendPushStatIfShowForegroundDisabled = applicationInfo.metaData.getBoolean("com.pushwoosh.send_push_stats_if_alert_disabled", false);
		isServerCommunicationAllowed = applicationInfo.metaData.getBoolean("com.pushwoosh.allow_server_communication", true);
		showPushnotificationAlert = applicationInfo.metaData.getBoolean("com.pushwoosh.foreground_push", false);
		handleNotificationsUsingWorkManager = applicationInfo.metaData.getBoolean("com.pushwoosh.handle_notifications_using_workmanager", false);
		shouldShowFullscreenRichMedia = applicationInfo.metaData.getBoolean("com.pushwoosh.show_fullscreen_richmedia", true);

		String notificationIconPath = applicationInfo.metaData.getString("com.pushwoosh.notification_icon");
		if (notificationIconPath != null) {
			// AndroidManifest.xml contains full path of notification icon e.g "res/drawable-xxhdpi-v11/notification_small_icon.png"
			// Need to extract resource name from path first
			String resourceName = FileUtils.getLastPathComponent(notificationIconPath);
			resourceName = FileUtils.removeExtension(resourceName);
			notificationIcon = AndroidPlatformModule.getResourceProvider().getIdentifier(resourceName, "drawable");
		}

		notificationIconColor = applicationInfo.metaData.getInt("com.pushwoosh.notification_icon_color", NotificationCompat.COLOR_DEFAULT);

		for (String key : applicationInfo.metaData.keySet()) {
			if (key.startsWith("com.pushwoosh.plugin.")) {
				try {
					//noinspection unchecked
					Class<? extends Plugin> pluginClass = (Class<? extends Plugin>) getClass(applicationInfo.metaData, key);
					if (pluginClass != null) {
						plugins.add(pluginClass.newInstance());
					}
				} catch (Exception ignore) {

				}
			}
		}
		try {
			//noinspection unchecked
			Class<? extends PluginProvider> pluginProviderClass = (Class<? extends PluginProvider>) getClass(applicationInfo.metaData, "com.pushwoosh.internal.plugin_provider");
			if (pluginProviderClass != null) {
				pluginProvider = pluginProviderClass.newInstance();
			}
		} catch (Exception ignore) {
		}

		if (pluginProvider == null) {
			pluginProvider = new NativePluginProvider();
		}

		boolean isCollectingDeviceDataAllowed = applicationInfo.metaData.getBoolean("com.pushwoosh.allow_collecting_device_data", true);
		if (!isCollectingDeviceDataAllowed) {
			isCollectingDeviceOsVersionAllowed = false;
			isCollectingDeviceLocaleAllowed = false;
			isCollectingDeviceModelAllowed = false;
		} else {
			isCollectingDeviceOsVersionAllowed = applicationInfo.metaData.getBoolean("com.pushwoosh.allow_collecting_device_os_version", true);
			isCollectingDeviceLocaleAllowed = applicationInfo.metaData.getBoolean("com.pushwoosh.allow_collecting_device_locale", true);
			isCollectingDeviceModelAllowed = applicationInfo.metaData.getBoolean("com.pushwoosh.allow_collecting_device_model", true);
		}
	}

	private String getString(Bundle metadata, String key, String deprecatedKey) {
		String result = metadata.getString(key);
		if (result == null) {
			result = metadata.getString(deprecatedKey);
			if (result != null) {
				PWLog.warn("'" + deprecatedKey + "' is deprecated consider using '" + key + "'");
			}
		}
		return result;
	}

	private Class<?> getClass(Bundle metadata, String key) {
		String className = metadata.getString(key);
		if (className != null && className.startsWith(".")) {
			className = AndroidPlatformModule.getAppInfoProvider().getPackageName() + className;
		}

		if (className != null) {
			try {
				Class<?> clazz = Class.forName(className);
				clazz.getConstructor();
				return clazz;
			} catch (ClassNotFoundException e) {
				PWLog.exception(e);
				throw new IllegalStateException("Could not find class for name: " + className);
			} catch (NoSuchMethodException e) {
				PWLog.exception(e);
				throw new IllegalStateException("Could not find public default constructor for class: " + className);
			}
		}

		return null;
	}

	@Override
	public String getAppId() {
		return appId;
	}

	@Override
	public String getProjectId() {
		return projectId;
	}

	@Override
	public String getXiaomiAppId() {
		return xiaomiAppId;
	}

	@Override
	public String getXiaomiAppKey() {
		return xiaomiAppKey;
	}
	@Override
	public String getXiaomiAppRegion() {
		return xiaomiAppRegion;
	}

	@Override
	public String getLogLevel() {
		return logLevel;
	}

	@Override
	public String getRequestUrl() {
		return requestUrl;
	}

	@Override
	public Class<?> getNotificationService() {
		return notificationService;
	}

	@Override
	public Class<?> getNotificationFactory() {
		return notificationFactory;
	}

	@Override
	public Class<?> getSummaryNotificationFactory() {
		return summaryNotificationFactory;
	}


	@Override
	public boolean isLazySdkInitialization() {
		return lazySdkInitialization;
	}

	@Override
	public boolean isMultinotificationMode() {
		return multinotificationMode;
	}

	@Override
	public boolean isLightscreenNotification() {
		return lightscreenNotification;
	}

	@Override
	public boolean isServerCommunicationAllowed() {
		return isServerCommunicationAllowed;
	}

	@Override
	public boolean showPushNotificationAlert() {
		return showPushnotificationAlert;
	}

	@Override
	public boolean shouldShowFullscreenRichMedia() {
		return shouldShowFullscreenRichMedia;
	}

	@Override
	public boolean isCollectingDeviceOsVersionAllowed() {
		return isCollectingDeviceOsVersionAllowed;
	}

	@Override
	public boolean isCollectingDeviceLocaleAllowed() {
		return isCollectingDeviceLocaleAllowed;
	}

	@Override
	public boolean isCollectingDeviceModelAllowed() {
		return isCollectingDeviceModelAllowed;
	}

	@Override
	public boolean handleNotificationsUsingWorkManager() {
		return handleNotificationsUsingWorkManager;
	}

	@Override
	@IdRes
	public int getNotificationIcon() {
		return notificationIcon;
	}

	@Override
	@ColorInt
	public int getNotificationIconColor() {
		return notificationIconColor;
	}

	@Override
	@NonNull
	public Collection<Plugin> getPlugins() {
		return plugins;
	}

	@Override
	public PluginProvider getPluginProvider() {
		return pluginProvider;
	}

	@Override
	public boolean getSendPushStatIfShowForegroundDisabled() {
		return sendPushStatIfShowForegroundDisabled;
	}

	@Override
	public String[] getTrustedPackageNames() {
		return trustedPackageNames;
	}

	@Override
	public void setLazySdkInitialization(boolean value) {
		lazySdkInitialization = value;
	}
}
