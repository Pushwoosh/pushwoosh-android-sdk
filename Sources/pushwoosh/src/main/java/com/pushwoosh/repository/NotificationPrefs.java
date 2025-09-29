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

package com.pushwoosh.repository;

import android.content.SharedPreferences;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.platform.prefs.PrefsProvider;
import com.pushwoosh.internal.platform.prefs.migration.MigrationScheme;
import com.pushwoosh.internal.preference.PreferenceArrayListValue;
import com.pushwoosh.internal.preference.PreferenceBooleanValue;
import com.pushwoosh.internal.preference.PreferenceClassValue;
import com.pushwoosh.internal.preference.PreferenceIntValue;
import com.pushwoosh.internal.preference.PreferenceJsonObjectValue;
import com.pushwoosh.internal.preference.PreferenceSoundTypeValue;
import com.pushwoosh.internal.preference.PreferenceStringValue;
import com.pushwoosh.internal.preference.PreferenceVibrateTypeValue;
import com.pushwoosh.internal.utils.Config;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;

public class NotificationPrefs {
	private static final int INITIAL_MESSAGE_ID = 1001;

	private static final String PREFERENCE = "com.pushwoosh.pushnotifications";

	public static final String DEFAULT_CHANNEL_NAME = "Push notification";
	public static final String DEFAULT_GROUP_CHANNEL_ID = "pw_push_notifications_summary_id";
	public static final String DEFAULT_GROUP_CHANNEL_NAME = "Push notifications summary";
	public static final String DEPRECATED_GROUP_CHANNEL_NAME = "Push notifications group";
	public static final String DEFAULT_NOTIFICATION_GROUP = "group_undefined";

	private static final String PROPERTY_MULTI_MODE = "dm_multimode";
	private static final String PROPERTY_SOUND_TYPE = "dm_soundtype";
	private static final String PROPERTY_VIBRATE_TYPE = "dm_vibratetype";
	private static final String PROPERTY_CHANNEL_NAME = "channel_name";
	private static final String PROPERTY_MESSAGE_ID = "dm_messageid";
	private static final String PROPERTY_SCREEN_LIGHT = "dm_lightson";
	private static final String PROPERTY_LED = "dm_ledon";
	private static final String PROPERTY_LED_COLOR = "dm_led_color";
	private static final String PROPERTY_NOTIFICATION_FACTORY = "pw_notification_factory";
	private static final String PROPERTY_PUSH_HISTORY = "pushHistoryArray";
	private static final String PROPERTY_CACHED_TAGS = "cached_tags_string";
	private static final String PROPERTY_NOTIFICATION_BACKGROUND_COLOR = "pw_notification_background_color";
	private static final String PROPERTY_NOTIFICATION_HASH = "pw_notification_stat_hash";
	private static final String PROPERTY_RICHMEDIA_DELAY = "pw_richmedia_delay";
	private static final String PROPERTY_NOTIFICATIONS_ENABLED = "pw_notifications_enabled";
	private static final String PROPERTY_TAG_MIGRATION_DONE = "pw_tags_migration_done";
	private static final String PROPERTY_CUSTOM_DATA = "pw_custom_data";
	private static final String PROPERTY_MESSAGE_HASH = "pw_message_hash";
	private static final String PROPERTY_IS_SERVER_COMMUNICATION_ALLOWED = "pw_is_server_communication_allowed";
	private static final String PROPERTY_IS_SHOW_NOTIFICATION_ALERT = "pw_is_show_notification_alert";
	private static final String PROPERTY_IS_COLLECTING_DEVICE_OS_VERSION_ALLOWED = "pw_is_collecting_device_os_version_allowed";
	private static final String PROPERTY_IS_COLLECTING_DEVICE_LOCALE_ALLOWED = "pw_is_collecting_device_locale_allowed";
	private static final String PROPERTY_IS_COLLECTING_DEVICE_MODEL_ALLOWED = "pw_is_collecting_device_model_allowed";
	private static final String PROPERTY_HANDLE_NOTIFICATIONS_USING_WORK_MANAGER = "pw_handle_notifications_using_work_manager";
	private static final String PROPERTY_SHOW_FULLSCREEN_RICHMEDIA = "pw_show_fullscreen_richmedia";
	private static final String PROPERTY_RICHMEDIA_DISMISS_ANIMATION_TYPE = "pw_richmedia_dissmiss_animation";
	private static final String PROPERTY_RICHMEDIA_PRESENT_ANIMATION_TYPE = "pw_richmedia_present_animation";
	private static final String PROPERTY_RICHMEDIA_SWIPE_GESTURE_BITMASK = "pw_richmedia_swipe_gesture_bitmask";
	private static final String PROPERTY_RICHMEDIA_VIEW_POSITION = "pw_richmedia_view_position";
	private static final String PROPERTY_RICHMEDIA_WINDOW_WIDTH = "pw_richmedia_window_width";
	private static final String PROPERTY_RICHMEDIA_ANIMATION_DURATION = "pw_richmedia_animation_duration";
	private static final String PROPERTY_RICHMEDIA_STATUS_BAR_COVERED = "pw_richmedia_status_bar_covered";
	private static final String PROPERTY_RICHMEDIA_RESPECT_EDGE_TO_EDGE_LAYOUT = "pw_richmedia_respect_edge_to_edge_layout";
	private static final String PROPERTY_ALLOWED_EXTERNAL_HOSTS = "pw_allowed_external_hosts";



	private final PreferenceBooleanValue multiMode;
	private final PreferenceIntValue messageId;
	private final PreferenceBooleanValue lightScreenOn;
	private final PreferenceBooleanValue ledEnabled;
	private final PreferenceIntValue ledColor;
	private final PreferenceIntValue iconBackgroundColor;
	private final PreferenceIntValue richMediaDelayMs;
	private final PreferenceStringValue lastNotificationHash;
	private final PreferenceBooleanValue notificationEnabled;
	private final PreferenceSoundTypeValue soundType;
	private final PreferenceVibrateTypeValue vibrateType;
	private final PreferenceStringValue channelName;
	private final PreferenceArrayListValue<String> pushHistory;
	private final PreferenceArrayListValue<String> allowedExternalHosts;
	private final PreferenceJsonObjectValue tags;
	private final PreferenceClassValue notificationFactoryClass;
	private final PreferenceClassValue summaryNotificationFactoryClass;
	private final PreferenceBooleanValue tagsMigrationDone;
	private final PreferenceStringValue customData;
	private final PreferenceStringValue messageHash;
	private final PreferenceBooleanValue isServerCommunicationAllowed;
	private final PreferenceBooleanValue showPushnotificationAlert;
	private final PreferenceBooleanValue isCollectingDeviceOsVersionAllowed;
	private final PreferenceBooleanValue isCollectingDeviceLocaleAllowed;
	private final PreferenceBooleanValue isCollectingDeviceModelAllowed;
	private final PreferenceBooleanValue handleNotificationsUsingWorkManager;
	private final PreferenceBooleanValue showFullscreenRichMedia;
	private final PreferenceIntValue richMediaDismissAnimation;
	private final PreferenceIntValue richMediaPresentAnimation;
	private final PreferenceIntValue richMediaSwipeGestureBitMask;
	private final PreferenceIntValue richMediaViewPosition;
	private final PreferenceIntValue richmediaWindowWidth;
	private final PreferenceIntValue richMediaAnimationDuration;
	private final PreferenceBooleanValue richMediaStatusBarCovered;
	private final PreferenceBooleanValue richMediaRespectEdgeToEdgeLayout;


	NotificationPrefs(Config config) {
		PWLog.noise("NotificationPrefs()...");

		SharedPreferences preferences = AndroidPlatformModule.getPrefsProvider().providePrefs(PREFERENCE);
		multiMode = new PreferenceBooleanValue(preferences, PROPERTY_MULTI_MODE, config.isMultinotificationMode());
		messageId = new PreferenceIntValue(preferences, PROPERTY_MESSAGE_ID, INITIAL_MESSAGE_ID);
		lightScreenOn = new PreferenceBooleanValue(preferences, PROPERTY_SCREEN_LIGHT, config.isLightscreenNotification());
		ledEnabled = new PreferenceBooleanValue(preferences, PROPERTY_LED, false);
		ledColor = new PreferenceIntValue(preferences, PROPERTY_LED_COLOR, 0xFFFFFFFF);
		notificationFactoryClass = new PreferenceClassValue(preferences, PROPERTY_NOTIFICATION_FACTORY, config.getNotificationFactory());
		summaryNotificationFactoryClass = new PreferenceClassValue(preferences, PROPERTY_NOTIFICATION_FACTORY, config.getSummaryNotificationFactory());
		iconBackgroundColor = new PreferenceIntValue(preferences, PROPERTY_NOTIFICATION_BACKGROUND_COLOR, config.getNotificationIconColor());
		richMediaDelayMs = new PreferenceIntValue(preferences, PROPERTY_RICHMEDIA_DELAY, config.getPluginProvider().richMediaStartDelay());
		lastNotificationHash = new PreferenceStringValue(preferences, PROPERTY_NOTIFICATION_HASH, null);
		notificationEnabled = new PreferenceBooleanValue(preferences, PROPERTY_NOTIFICATIONS_ENABLED, true);
		soundType = new PreferenceSoundTypeValue(preferences, PROPERTY_SOUND_TYPE, SoundType.DEFAULT_MODE);
		vibrateType = new PreferenceVibrateTypeValue(preferences, PROPERTY_VIBRATE_TYPE, VibrateType.DEFAULT_MODE);
		channelName = new PreferenceStringValue(preferences, PROPERTY_CHANNEL_NAME, DEFAULT_CHANNEL_NAME);
		pushHistory = new PreferenceArrayListValue<>(preferences, PROPERTY_PUSH_HISTORY, Pushwoosh.PUSH_HISTORY_CAPACITY, String.class);
		allowedExternalHosts = new PreferenceArrayListValue<>(preferences, PROPERTY_ALLOWED_EXTERNAL_HOSTS, 256, String.class);
		tags = new PreferenceJsonObjectValue(preferences, PROPERTY_CACHED_TAGS);
		tagsMigrationDone = new PreferenceBooleanValue(preferences, PROPERTY_TAG_MIGRATION_DONE, false);
		customData = new PreferenceStringValue(preferences, PROPERTY_CUSTOM_DATA, null);
		messageHash = new PreferenceStringValue(preferences, PROPERTY_MESSAGE_HASH, null);
		isServerCommunicationAllowed = new PreferenceBooleanValue(preferences, PROPERTY_IS_SERVER_COMMUNICATION_ALLOWED, config.isServerCommunicationAllowed());
		showPushnotificationAlert = new PreferenceBooleanValue(preferences, PROPERTY_IS_SHOW_NOTIFICATION_ALERT, config.showPushNotificationAlert());
		handleNotificationsUsingWorkManager = new PreferenceBooleanValue(preferences, PROPERTY_HANDLE_NOTIFICATIONS_USING_WORK_MANAGER, config.handleNotificationsUsingWorkManager());
		isCollectingDeviceOsVersionAllowed = new PreferenceBooleanValue(preferences, PROPERTY_IS_COLLECTING_DEVICE_OS_VERSION_ALLOWED, config.isCollectingDeviceOsVersionAllowed());
		isCollectingDeviceLocaleAllowed = new PreferenceBooleanValue(preferences, PROPERTY_IS_COLLECTING_DEVICE_LOCALE_ALLOWED, config.isCollectingDeviceLocaleAllowed());
		isCollectingDeviceModelAllowed = new PreferenceBooleanValue(preferences, PROPERTY_IS_COLLECTING_DEVICE_MODEL_ALLOWED, config.isCollectingDeviceModelAllowed());
		showFullscreenRichMedia = new PreferenceBooleanValue(preferences, PROPERTY_SHOW_FULLSCREEN_RICHMEDIA, config.shouldShowFullscreenRichMedia());

		richMediaDismissAnimation = new PreferenceIntValue(preferences, PROPERTY_RICHMEDIA_DISMISS_ANIMATION_TYPE, ModalRichMediaDismissAnimationType.FADE_OUT.getCode());
		richMediaPresentAnimation = new PreferenceIntValue(preferences, PROPERTY_RICHMEDIA_PRESENT_ANIMATION_TYPE, ModalRichMediaPresentAnimationType.FADE_IN.getCode());
		richMediaSwipeGestureBitMask = new PreferenceIntValue(preferences, PROPERTY_RICHMEDIA_SWIPE_GESTURE_BITMASK, 0);
		richMediaViewPosition = new PreferenceIntValue(preferences, PROPERTY_RICHMEDIA_VIEW_POSITION, ModalRichMediaViewPosition.FULLSCREEN.getCode());
		richmediaWindowWidth = new PreferenceIntValue(preferences, PROPERTY_RICHMEDIA_WINDOW_WIDTH, ModalRichMediaWindowWidth.FULL_SCREEN.getCode());

		richMediaAnimationDuration = new PreferenceIntValue(preferences, PROPERTY_RICHMEDIA_ANIMATION_DURATION, 1000);
		richMediaStatusBarCovered = new PreferenceBooleanValue(preferences, PROPERTY_RICHMEDIA_STATUS_BAR_COVERED, false);
		richMediaRespectEdgeToEdgeLayout = new PreferenceBooleanValue(preferences, PROPERTY_RICHMEDIA_RESPECT_EDGE_TO_EDGE_LAYOUT, true);

		PWLog.noise("NotificationPrefs() done");
	}

	public PreferenceIntValue richMediaDismissAnimation() { return richMediaDismissAnimation; }

	public PreferenceIntValue richMediaPresentAnimation() { return richMediaPresentAnimation; }

	public PreferenceIntValue richMediaSwipeGestureBitMask() { return richMediaSwipeGestureBitMask; }

	public PreferenceIntValue richMediaViewPosition() { return richMediaViewPosition; }

	public PreferenceIntValue richMediaWindowWidth() { return richmediaWindowWidth; }

	public PreferenceIntValue richMediaAnimationDuration() { return richMediaAnimationDuration; }

	public PreferenceBooleanValue richMediaStatusBarCovered() { return richMediaStatusBarCovered; }

	public PreferenceBooleanValue richMediaRespectEdgeToEdgeLayout() { return richMediaRespectEdgeToEdgeLayout; }

	public PreferenceBooleanValue multiMode() {
		return multiMode;
	}

	public PreferenceIntValue messageId() {
		return messageId;
	}

	public PreferenceBooleanValue lightScreenOn() {
		return lightScreenOn;
	}

	public PreferenceBooleanValue ledEnabled() {
		return ledEnabled;
	}

	public PreferenceIntValue ledColor() {
		return ledColor;
	}

	public PreferenceClassValue notificationFactoryClass() {
		return notificationFactoryClass;
	}

	public PreferenceClassValue summaryNotificationFactoryClass() {
		return summaryNotificationFactoryClass;
	}

	public PreferenceIntValue iconBackgroundColor() {
		return iconBackgroundColor;
	}

	public PreferenceIntValue richMediaDelayMs() {
		return richMediaDelayMs;
	}

	public PreferenceStringValue lastNotificationHash() {
		return lastNotificationHash;
	}

	public PreferenceBooleanValue notificationEnabled() {
		return notificationEnabled;
	}

	public PreferenceSoundTypeValue soundType() {
		return soundType;
	}

	public PreferenceVibrateTypeValue vibrateType() {
		return vibrateType;
	}

	public PreferenceStringValue channelName() {
		return channelName;
	}

	public PreferenceArrayListValue<String> pushHistory() {
		return pushHistory;
	}

	public PreferenceArrayListValue<String> allowedExternalHosts() {
		return allowedExternalHosts;
	}

	public PreferenceJsonObjectValue tags() {
		return tags;
	}

	public PreferenceBooleanValue tagsMigrationDone() {
		return tagsMigrationDone;
	}

	public PreferenceStringValue customData() {
		return customData;
	}

	public PreferenceStringValue messageHash() {
		return messageHash;
	}

	public PreferenceBooleanValue isServerCommunicationAllowed() {
		return isServerCommunicationAllowed;
	}

	public PreferenceBooleanValue showPushnotificationAlert() {
		return showPushnotificationAlert;
	}

	public PreferenceBooleanValue isCollectingDeviceOsVersionAllowed() {
		return isCollectingDeviceOsVersionAllowed;
	}

	public PreferenceBooleanValue isCollectingDeviceLocaleAllowed() {
		return isCollectingDeviceLocaleAllowed;
	}

	public PreferenceBooleanValue isCollectingDeviceModelAllowed() {
		return isCollectingDeviceModelAllowed;
	}

	public PreferenceBooleanValue handleNotificationsUsingWorkManager() {
		return handleNotificationsUsingWorkManager;
	}

	public PreferenceBooleanValue showFullscreenRichMedia() {
		return showFullscreenRichMedia;
	}

	/**
	 * Create {@link com.pushwoosh.internal.platform.prefs.migration.MigrationScheme} associated with this class.
	 * Don't forget add field here if it will be added to this class
	 * @param prefsProvider - prefsProvider which will provide prefs for migrationScheme
	 * @return MigrationScheme to correct migration from one prefs to another
	 */
	static MigrationScheme provideMigrationScheme(PrefsProvider prefsProvider){
		MigrationScheme migrationScheme = new MigrationScheme(PREFERENCE);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.BOOLEAN, PROPERTY_MULTI_MODE);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.INT, PROPERTY_SOUND_TYPE);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.INT, PROPERTY_VIBRATE_TYPE);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_CHANNEL_NAME);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.INT, PROPERTY_MESSAGE_ID);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.BOOLEAN, PROPERTY_SCREEN_LIGHT);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.BOOLEAN, PROPERTY_LED);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.INT, PROPERTY_LED_COLOR);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_NOTIFICATION_FACTORY);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_PUSH_HISTORY);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_ALLOWED_EXTERNAL_HOSTS);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_CACHED_TAGS);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.INT, PROPERTY_NOTIFICATION_BACKGROUND_COLOR);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.STRING, PROPERTY_NOTIFICATION_HASH);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.INT, PROPERTY_RICHMEDIA_DELAY);
		migrationScheme.put(prefsProvider, MigrationScheme.AvailableType.BOOLEAN, PROPERTY_NOTIFICATIONS_ENABLED);
		return migrationScheme;
	}
}
