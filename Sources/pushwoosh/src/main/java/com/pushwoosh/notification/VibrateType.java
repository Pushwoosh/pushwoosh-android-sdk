//
//  VibrateType.java
//
// Pushwoosh Push Notifications SDK
// www.pushwoosh.com
//
// MIT Licensed
package com.pushwoosh.notification;

/**
 * Defines vibration behavior for push notifications.
 * <p>
 * VibrateType controls when the device vibrates for push notifications based on the device's
 * ringer mode. This setting can be configured globally for all push notifications using
 * {@link PushwooshNotificationSettings#setVibrateNotificationType(VibrateType)}.
 * <p>
 * <b>Required Permission:</b><br>
 * Your application must declare the {@link android.Manifest.permission#VIBRATE VIBRATE} permission
 * in AndroidManifest.xml for vibration to work:
 * <pre>
 * {@code
 * <uses-permission android:name="android.permission.VIBRATE" />
 * }
 * </pre>
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
 *           // Configure notification vibration behavior
 *           PushwooshNotificationSettings settings =
 *               new PushwooshNotificationSettings();
 *
 *           // Always vibrate for important notifications
 *           settings.setVibrateNotificationType(VibrateType.ALWAYS);
 *
 *           // Or disable vibration completely
 *           // settings.setVibrateNotificationType(VibrateType.NO_VIBRATE);
 *
 *           // Or use default system behavior
 *           // settings.setVibrateNotificationType(VibrateType.DEFAULT_MODE);
 *       }
 *   }
 *
 *   // Example: User preference for vibration
 *   public void applyUserSettings(boolean userWantsVibration) {
 *       PushwooshNotificationSettings settings =
 *           new PushwooshNotificationSettings();
 *
 *       if (userWantsVibration) {
 *           settings.setVibrateNotificationType(VibrateType.ALWAYS);
 *       } else {
 *           settings.setVibrateNotificationType(VibrateType.NO_VIBRATE);
 *       }
 *   }
 *
 *   // Example: Different vibration for different notification types
 *   public void configureVibrateForMessageType(String messageType) {
 *       PushwooshNotificationSettings settings =
 *           new PushwooshNotificationSettings();
 *
 *       if ("urgent".equals(messageType)) {
 *           // Critical messages always vibrate
 *           settings.setVibrateNotificationType(VibrateType.ALWAYS);
 *       } else {
 *           // Normal messages respect device settings
 *           settings.setVibrateNotificationType(VibrateType.DEFAULT_MODE);
 *       }
 *   }
 * }
 * </pre>
 *
 * @see PushwooshNotificationSettings#setVibrateNotificationType(VibrateType)
 * @see SoundType
 * @see android.Manifest.permission#VIBRATE
 */
public enum VibrateType {
	/**
	 * Vibration occurs only when device is in vibrate ringer mode.
	 * <p>
	 * Notification will cause vibration only if the device's AudioManager ringer mode is set to
	 * {@link android.media.AudioManager#RINGER_MODE_VIBRATE RINGER_MODE_VIBRATE}.
	 * If the device is in normal or silent mode, no vibration will occur.
	 * <p>
	 * This is the default Android system behavior and respects user's device settings.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   PushwooshNotificationSettings settings = new PushwooshNotificationSettings();
	 *   // Respect device ringer mode
	 *   settings.setVibrateNotificationType(VibrateType.DEFAULT_MODE);
	 *
	 *   // Device will vibrate only if user has set it to vibrate mode
	 *   Log.d("App", "Vibration follows system settings");
	 * }
	 * </pre>
	 *
	 * @see android.media.AudioManager#RINGER_MODE_VIBRATE
	 */
	DEFAULT_MODE(0),

	/**
	 * Vibration is disabled for notifications.
	 * <p>
	 * Disables notification vibration completely, regardless of device ringer mode.
	 * Useful for apps that need non-intrusive notifications or when users prefer
	 * visual-only alerts.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Silent notifications for library or cinema app
	 *   PushwooshNotificationSettings settings = new PushwooshNotificationSettings();
	 *   settings.setVibrateNotificationType(VibrateType.NO_VIBRATE);
	 *   settings.setSoundNotificationType(SoundType.NO_SOUND);
	 *
	 *   Log.d("App", "Notifications will not vibrate or make sound");
	 *
	 *   // Useful for focus/productivity apps
	 *   if (isFocusModeEnabled) {
	 *       settings.setVibrateNotificationType(VibrateType.NO_VIBRATE);
	 *   }
	 * }
	 * </pre>
	 */
	NO_VIBRATE(1),

	/**
	 * Vibration always occurs for notifications.
	 * <p>
	 * Forces notification vibration regardless of device ringer mode (silent, vibrate, or normal).
	 * Use this for critical notifications that should always alert the user tactilely.
	 * <p>
	 * <b>Note:</b> This setting overrides the device's ringer mode. Use carefully as it may
	 * disturb users when they have explicitly silenced their device.
	 * <p>
	 * <b>Permission Required:</b> {@link android.Manifest.permission#VIBRATE VIBRATE} permission
	 * must be declared in AndroidManifest.xml.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Critical alerts that must vibrate (e.g., alarm or security app)
	 *   PushwooshNotificationSettings settings = new PushwooshNotificationSettings();
	 *   settings.setVibrateNotificationType(VibrateType.ALWAYS);
	 *
	 *   Log.d("App", "Critical notifications will always vibrate");
	 *
	 *   // For delivery apps with time-sensitive notifications
	 *   if (isDeliveryArriving) {
	 *       settings.setVibrateNotificationType(VibrateType.ALWAYS);
	 *       settings.setSoundNotificationType(SoundType.ALWAYS);
	 *   }
	 *
	 *   // For emergency or SOS apps
	 *   if (isEmergencyAlert) {
	 *       settings.setVibrateNotificationType(VibrateType.ALWAYS);
	 *   }
	 * }
	 * </pre>
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
