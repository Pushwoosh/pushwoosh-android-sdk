//
//  VibrateType.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.pushwoosh.notification;

/**
 * Push notification vibration setting.
 * Application must use <a href="https://developer.android.com/reference/android/Manifest.permission.html#VIBRATE">VIBRATE</a> permission in order for vibration to work.
 *
 * @see com.pushwoosh.notification.PushwooshNotificationSettings#setVibrateNotificationType(VibrateType)
 */
public enum VibrateType {
	/**
	 * Notification causes vibration if AudioManager ringer mode is <a href="https://developer.android.com/reference/android/media/AudioManager.html#RINGER_MODE_VIBRATE">RINGER_MODE_VIBRATE</a>.
	 */
	DEFAULT_MODE(0),

	/**
	 * Notification will not cause vibration.
	 */
	NO_VIBRATE(1),

	/**
	 * Notification will always cause vibration.
	 */
	ALWAYS(2);

	private final int value;

	VibrateType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static VibrateType fromInt(int x) {
		switch (x) {
			case 0:
				return DEFAULT_MODE;
			case 1:
				return NO_VIBRATE;
			case 2:
				return ALWAYS;
			default:
				return DEFAULT_MODE;
		}
	}
}
