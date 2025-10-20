//
//  SoundType.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.pushwoosh.notification;

/**
 * Defines sound playback behavior for push notifications.
 * <p>
 * SoundType controls when notification sounds are played based on the device's ringer mode.
 * This setting can be configured globally for all push notifications using
 * {@link PushwooshNotificationSettings#setSoundNotificationType(SoundType)}.
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * {@code
 *   // In your Application class or initialization code
 *   public class MyApplication extends Application {
 *       @Override
 *       public void onCreate() {
 *           super.onCreate();
 *
 *           // Initialize Pushwoosh
 *           Pushwoosh.getInstance().registerForPushNotifications();
 *
 *           // Configure notification sound behavior
 *           PushwooshNotificationSettings settings =
 *               new PushwooshNotificationSettings();
 *
 *           // Always play sound for important notifications
 *           settings.setSoundNotificationType(SoundType.ALWAYS);
 *
 *           // Or disable sound completely for silent app
 *           // settings.setSoundNotificationType(SoundType.NO_SOUND);
 *
 *           // Or use default system behavior
 *           // settings.setSoundNotificationType(SoundType.DEFAULT_MODE);
 *       }
 *   }
 *
 *   // Example: Different sound settings for different app modes
 *   public void updateNotificationSettings(boolean isWorkingHours) {
 *       PushwooshNotificationSettings settings =
 *           new PushwooshNotificationSettings();
 *
 *       if (isWorkingHours) {
 *           // Silent during work hours
 *           settings.setSoundNotificationType(SoundType.NO_SOUND);
 *       } else {
 *           // Sound enabled after hours
 *           settings.setSoundNotificationType(SoundType.ALWAYS);
 *       }
 *   }
 * }
 * </pre>
 *
 * @see PushwooshNotificationSettings#setSoundNotificationType(SoundType)
 * @see VibrateType
 */
public enum SoundType {
	/**
	 * Sound is played only when device is in normal ringer mode.
	 * <p>
	 * Notification sound will play only if the device's AudioManager ringer mode is set to
	 * {@link android.media.AudioManager#RINGER_MODE_NORMAL RINGER_MODE_NORMAL}.
	 * If the device is in silent or vibrate mode, no sound will be played.
	 * <p>
	 * This is the default Android system behavior and respects user's device settings.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   PushwooshNotificationSettings settings = new PushwooshNotificationSettings();
	 *   // Respect device ringer mode
	 *   settings.setSoundNotificationType(SoundType.DEFAULT_MODE);
	 * }
	 * </pre>
	 *
	 * @see android.media.AudioManager#RINGER_MODE_NORMAL
	 */
	DEFAULT_MODE(0),

	/**
	 * Sound is never played for notifications.
	 * <p>
	 * Disables notification sounds completely, regardless of device ringer mode.
	 * Useful for apps that need silent notifications or have custom sound handling.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Silent notifications for focus/productivity app
	 *   PushwooshNotificationSettings settings = new PushwooshNotificationSettings();
	 *   settings.setSoundNotificationType(SoundType.NO_SOUND);
	 *
	 *   // User can still see notifications in tray, but no sound
	 *   Log.d("App", "Notifications will be silent");
	 * }
	 * </pre>
	 */
	NO_SOUND(1),

	/**
	 * Sound is always played for notifications.
	 * <p>
	 * Forces notification sound to play regardless of device ringer mode (silent, vibrate, or normal).
	 * Use this for critical notifications that should always alert the user with sound.
	 * <p>
	 * <b>Note:</b> This setting overrides the device's ringer mode. Use carefully as it may
	 * interrupt users when they have explicitly silenced their device.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Critical alerts that must make sound (e.g., delivery app)
	 *   PushwooshNotificationSettings settings = new PushwooshNotificationSettings();
	 *   settings.setSoundNotificationType(SoundType.ALWAYS);
	 *
	 *   Log.d("App", "Critical notifications will always play sound");
	 *
	 *   // For emergency or time-sensitive apps
	 *   if (isEmergencyMode) {
	 *       settings.setSoundNotificationType(SoundType.ALWAYS);
	 *   }
	 * }
	 * </pre>
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
