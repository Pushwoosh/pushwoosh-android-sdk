//
//  SoundType.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.pushwoosh.notification;

/**
 * Push notification sound setting.
 *
 * @see PushwooshNotificationSettings#setSoundNotificationType(SoundType)
 */
public enum SoundType {
	/**
	 * Sound is played when notification arrives if AudioManager ringer mode is <a href="https://developer.android.com/reference/android/media/AudioManager.html#RINGER_MODE_NORMAL">RINGER_MODE_NORMAL</a>.
	 */
	DEFAULT_MODE(0),

	/**
	 * Sound is never played when notification arrives.
	 */
	NO_SOUND(1),

	/**
	 * Sound is always played when notification arrives.
	 */
	ALWAYS(2);

	private final int value;

	SoundType(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public static SoundType fromInt(int x) {
		switch (x) {
			case 0:
				return DEFAULT_MODE;
			case 1:
				return NO_SOUND;
			case 2:
				return ALWAYS;
			default:
				return DEFAULT_MODE;
		}
	}
}
