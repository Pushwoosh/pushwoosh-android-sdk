package com.pushwoosh.notification;

import android.content.Context;

import androidx.annotation.ColorInt;

import com.pushwoosh.PushwooshInitializer;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

/**
 * PushwooshNotificationSettings class is used to customise push notification appearance.
 */
public class PushwooshNotificationSettings {
	private static final NotificationPrefs PREFERENCES = RepositoryModule.getNotificationPreferences();

	private static boolean checkIfInitializaed() {
		if (PREFERENCES != null)
			return true;
		PushwooshPlatform.notifyNotInitialized();
		return false;
	}

	/**
	 * Allows multiple notifications to be displayed in notification center.
	 * By default SDK uses single notification mode where each notification overrides previously displayed notification.
	 *
	 * @param on enable multi/single notification mode
	 */
	public static void setMultiNotificationMode(boolean on) {
		if (checkIfInitializaed()) {
			PREFERENCES.multiMode().set(on);
		}
	}

	/**
	 * Set whether sound should be played when notification is received.
	 *
	 * @param soundNotificationType sound setting
	 */
	public static void setSoundNotificationType(SoundType soundNotificationType) {
		if (checkIfInitializaed()) {
			PREFERENCES.soundType().set(soundNotificationType);
		}
	}

	/**
	 * Set whether device should vibrate when notification is received.
	 * If "Force Vibration" is set in Pushwoosh control panel for remote notification it will cause vibration regardless of this setting.
	 *
	 * @param vibrateNotificationType vibration setting
	 */
	public static void setVibrateNotificationType(VibrateType vibrateNotificationType) {
		if (checkIfInitializaed()) {
			PREFERENCES.vibrateType().set(vibrateNotificationType);
		}
	}

	/**
	 * Set whether notification should unlock screen.
	 *
	 * @param on enable screen unlock
	 */
	public static void setLightScreenOnNotification(boolean on) {
		if (checkIfInitializaed()) {
			PREFERENCES.lightScreenOn().set(on);
		}
	}

	/**
	 * Set whether notification should cause LED blinking.
	 *
	 * @param on enable LED blinking
	 */
	public static void setEnableLED(boolean on) {
		if (checkIfInitializaed()) {
			PREFERENCES.ledEnabled().set(on);
		}
	}

	/**
	 * Set LED color. {@link #setEnableLED(boolean)} must be set to adjust LED color.
	 *
	 * @param color LED color
	 */
	public static void setColorLED(@ColorInt int color) {
		if (checkIfInitializaed()) {
			PREFERENCES.ledColor().set(color);
		}
	}

	/**
	 * Set notification icon background color
	 *
	 * @param color background color
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setSmallIcon(int)">Notification.Builder.setSmallIcon</a>
	 */
	public static void setNotificationIconBackgroundColor(@ColorInt int color) {
		if (checkIfInitializaed()) {
			PREFERENCES.iconBackgroundColor().set(color);
		}
	}

	/**
	 * @return true if notifications are enabled and will appear in notification center.
	 */
	public static boolean areNotificationsEnabled() {
		if (checkIfInitializaed()) {
			return NotificationUtils.areNotificationsEnabled() && PREFERENCES.notificationEnabled().get();
		}
		return false;
	}

	/**
	 * Set whether notifications should be enabled
	 *
	 * @param on enable notifications
	 */
	public static void enableNotifications(boolean on) {
		if (checkIfInitializaed()) {
			PREFERENCES.notificationEnabled().set(on);
		}
	}

	/**
	 * Set default notification channel name for API 26
	 *
	 * @param name name of notification channel
	 */
	public static void setNotificationChannelName(String name) {
		if (checkIfInitializaed()) {
			PREFERENCES.channelName().set(name);
		}
	}

	public static void lazyInitPushwoosh(Context context) {
		PushwooshInitializer.lazyInit(context);
	}
}
