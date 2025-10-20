package com.pushwoosh.notification;

import android.content.Context;

import androidx.annotation.ColorInt;

import com.pushwoosh.PushwooshInitializer;
import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

/**
 * Provides global configuration for push notification appearance and behavior.
 * <p>
 * PushwooshNotificationSettings allows you to customize how push notifications are displayed
 * across your entire application. Settings configured here apply to all push notifications
 * received by the app, unless overridden by individual notification settings from the
 * Pushwoosh Control Panel.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Notification Display Mode - Single or multiple notifications in tray</li>
 * <li>Sound Settings - Control when notification sounds play</li>
 * <li>Vibration Settings - Control when device vibrates</li>
 * <li>Visual Customization - LED color, icon background color, screen wake</li>
 * <li>Notification Channel - Configure Android O+ notification channel name</li>
 * <li>Enable/Disable - Turn notifications on/off programmatically</li>
 * </ul>
 * <p>
 * <b>Basic Configuration Example:</b>
 * <pre>
 * {@code
 *   public class MyApplication extends Application {
 *       @Override
 *       public void onCreate() {
 *           super.onCreate();
 *
 *           // Initialize Pushwoosh first
 *           Pushwoosh.getInstance().registerForPushNotifications();
 *
 *           // Configure notification appearance
 *           PushwooshNotificationSettings.setMultiNotificationMode(true);
 *           PushwooshNotificationSettings.setSoundNotificationType(SoundType.ALWAYS);
 *           PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.DEFAULT_MODE);
 *           PushwooshNotificationSettings.setEnableLED(true);
 *           PushwooshNotificationSettings.setColorLED(Color.BLUE);
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * <b>E-commerce App Example - Custom Branding:</b>
 * <pre>
 * {@code
 *   public class ShoppingApplication extends Application {
 *       @Override
 *       public void onCreate() {
 *           super.onCreate();
 *
 *           Pushwoosh.getInstance().registerForPushNotifications();
 *
 *           // Brand colors for notifications
 *           int brandColor = ContextCompat.getColor(this, R.color.brand_primary);
 *           PushwooshNotificationSettings.setNotificationIconBackgroundColor(brandColor);
 *           PushwooshNotificationSettings.setColorLED(brandColor);
 *           PushwooshNotificationSettings.setEnableLED(true);
 *
 *           // Multiple notifications for order updates
 *           PushwooshNotificationSettings.setMultiNotificationMode(true);
 *
 *           // Always alert for important updates
 *           PushwooshNotificationSettings.setSoundNotificationType(SoundType.ALWAYS);
 *           PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.ALWAYS);
 *           PushwooshNotificationSettings.setLightScreenOnNotification(true);
 *
 *           // Android O+ notification channel
 *           PushwooshNotificationSettings.setNotificationChannelName("Order Updates");
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * <b>Productivity App Example - User Preferences:</b>
 * <pre>
 * {@code
 *   public class FocusApplication extends Application {
 *       @Override
 *       public void onCreate() {
 *           super.onCreate();
 *
 *           Pushwoosh.getInstance().registerForPushNotifications();
 *
 *           // Load user preferences
 *           SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
 *           boolean focusModeEnabled = prefs.getBoolean("focus_mode", false);
 *
 *           if (focusModeEnabled) {
 *               // Silent notifications during focus mode
 *               PushwooshNotificationSettings.setSoundNotificationType(SoundType.NO_SOUND);
 *               PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.NO_VIBRATE);
 *               PushwooshNotificationSettings.setEnableLED(false);
 *           } else {
 *               // Normal notifications
 *               PushwooshNotificationSettings.setSoundNotificationType(SoundType.DEFAULT_MODE);
 *               PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.DEFAULT_MODE);
 *               PushwooshNotificationSettings.setEnableLED(true);
 *           }
 *       }
 *   }
 *
 *   // Update settings when user toggles focus mode
 *   public void onFocusModeChanged(boolean enabled) {
 *       if (enabled) {
 *           PushwooshNotificationSettings.setSoundNotificationType(SoundType.NO_SOUND);
 *           PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.NO_VIBRATE);
 *       } else {
 *           PushwooshNotificationSettings.setSoundNotificationType(SoundType.DEFAULT_MODE);
 *           PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.DEFAULT_MODE);
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>All methods are static and apply settings globally across the app</li>
 * <li>Settings persist across app restarts</li>
 * <li>Pushwoosh must be initialized before calling these methods</li>
 * <li>Individual notification settings from Pushwoosh Control Panel can override these defaults</li>
 * <li>LED customization requires {@link android.Manifest.permission#VIBRATE VIBRATE} permission</li>
 * <li>For Android O (API 26+), use {@link #setNotificationChannelName(String)} to set channel name</li>
 * </ul>
 *
 * @see SoundType
 * @see VibrateType
 * @see com.pushwoosh.Pushwoosh
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
	 * Enables or disables multi-notification mode.
	 * <p>
	 * By default, the SDK uses single notification mode where each new notification replaces
	 * the previous one in the notification tray. When multi-notification mode is enabled,
	 * each notification is displayed separately, allowing users to see multiple notifications
	 * at once.
	 * <p>
	 * <b>Single Mode (default):</b> Only the latest notification is visible<br>
	 * <b>Multi Mode:</b> All notifications are visible in the tray
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // E-commerce app - show multiple order notifications
	 *   public class ShoppingApp extends Application {
	 *       @Override
	 *       public void onCreate() {
	 *           super.onCreate();
	 *           Pushwoosh.getInstance().registerForPushNotifications();
	 *
	 *           // Enable multi-notification mode for order updates
	 *           PushwooshNotificationSettings.setMultiNotificationMode(true);
	 *           // Now users can see: "Order shipped", "Order delivered", etc.
	 *       }
	 *   }
	 *
	 *   // Messaging app - single mode to avoid clutter
	 *   public class MessagingApp extends Application {
	 *       @Override
	 *       public void onCreate() {
	 *           super.onCreate();
	 *           Pushwoosh.getInstance().registerForPushNotifications();
	 *
	 *           // Keep only latest message notification
	 *           PushwooshNotificationSettings.setMultiNotificationMode(false);
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param on {@code true} to show multiple notifications, {@code false} to show only the latest
	 * @see PushMessage#getTag()
	 */
	public static void setMultiNotificationMode(boolean on) {
		if (checkIfInitializaed()) {
			PREFERENCES.multiMode().set(on);
		}
	}

	/**
	 * Configures when notification sounds should play.
	 * <p>
	 * Sets the global sound behavior for all push notifications. This setting determines
	 * whether sounds play based on the device's ringer mode.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Always play sound for critical apps (delivery, emergency)
	 *   PushwooshNotificationSettings.setSoundNotificationType(SoundType.ALWAYS);
	 *
	 *   // Silent app (productivity, focus)
	 *   PushwooshNotificationSettings.setSoundNotificationType(SoundType.NO_SOUND);
	 *
	 *   // Respect device settings (most apps)
	 *   PushwooshNotificationSettings.setSoundNotificationType(SoundType.DEFAULT_MODE);
	 * }
	 * </pre>
	 *
	 * @param soundNotificationType the sound behavior mode
	 * @see SoundType
	 * @see #setVibrateNotificationType(VibrateType)
	 */
	public static void setSoundNotificationType(SoundType soundNotificationType) {
		if (checkIfInitializaed()) {
			PREFERENCES.soundType().set(soundNotificationType);
		}
	}

	/**
	 * Configures when device should vibrate for notifications.
	 * <p>
	 * Sets the global vibration behavior for all push notifications. This setting determines
	 * whether the device vibrates based on the ringer mode.
	 * <p>
	 * <b>Note:</b> If "Force Vibration" is enabled in the Pushwoosh Control Panel for a specific
	 * notification, it will vibrate regardless of this setting.
	 * <p>
	 * <b>Required Permission:</b> {@link android.Manifest.permission#VIBRATE VIBRATE}
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Always vibrate for urgent notifications (delivery arriving)
	 *   PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.ALWAYS);
	 *
	 *   // Never vibrate (silent mode app)
	 *   PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.NO_VIBRATE);
	 *
	 *   // Respect device settings (recommended)
	 *   PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.DEFAULT_MODE);
	 * }
	 * </pre>
	 *
	 * @param vibrateNotificationType the vibration behavior mode
	 * @see VibrateType
	 * @see #setSoundNotificationType(SoundType)
	 */
	public static void setVibrateNotificationType(VibrateType vibrateNotificationType) {
		if (checkIfInitializaed()) {
			PREFERENCES.vibrateType().set(vibrateNotificationType);
		}
	}

	/**
	 * Enables or disables screen wake-up when notification arrives.
	 * <p>
	 * When enabled, the device screen will turn on briefly when a push notification is received,
	 * even if the screen was off. This helps ensure users notice important notifications.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Wake screen for important notifications (delivery, alerts)
	 *   PushwooshNotificationSettings.setLightScreenOnNotification(true);
	 *
	 *   // Don't wake screen (battery saving)
	 *   PushwooshNotificationSettings.setLightScreenOnNotification(false);
	 *
	 *   // Conditional based on notification type
	 *   boolean isUrgent = checkNotificationType();
	 *   PushwooshNotificationSettings.setLightScreenOnNotification(isUrgent);
	 * }
	 * </pre>
	 *
	 * @param on {@code true} to wake screen on notification, {@code false} to keep screen off
	 */
	public static void setLightScreenOnNotification(boolean on) {
		if (checkIfInitializaed()) {
			PREFERENCES.lightScreenOn().set(on);
		}
	}

	/**
	 * Enables or disables LED blinking for notifications.
	 * <p>
	 * When enabled, the device's notification LED will blink when a notification is received
	 * (on devices that have an LED indicator). The LED color can be customized using
	 * {@link #setColorLED(int)}.
	 * <p>
	 * <b>Note:</b> Many modern devices no longer have physical notification LEDs.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Enable LED with custom color
	 *   PushwooshNotificationSettings.setEnableLED(true);
	 *   PushwooshNotificationSettings.setColorLED(Color.BLUE);
	 *
	 *   // Brand color LED for e-commerce app
	 *   int brandColor = ContextCompat.getColor(context, R.color.brand_primary);
	 *   PushwooshNotificationSettings.setEnableLED(true);
	 *   PushwooshNotificationSettings.setColorLED(brandColor);
	 * }
	 * </pre>
	 *
	 * @param on {@code true} to enable LED blinking, {@code false} to disable
	 * @see #setColorLED(int)
	 */
	public static void setEnableLED(boolean on) {
		if (checkIfInitializaed()) {
			PREFERENCES.ledEnabled().set(on);
		}
	}

	/**
	 * Sets the LED notification color.
	 * <p>
	 * Configures the color of the notification LED that blinks when notifications arrive.
	 * LED must be enabled first using {@link #setEnableLED(boolean)}.
	 * <p>
	 * <b>Note:</b> LED color support varies by device. Many modern devices no longer
	 * have physical notification LEDs.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Blue LED for general notifications
	 *   PushwooshNotificationSettings.setEnableLED(true);
	 *   PushwooshNotificationSettings.setColorLED(Color.BLUE);
	 *
	 *   // Red LED for urgent notifications
	 *   PushwooshNotificationSettings.setColorLED(Color.RED);
	 *
	 *   // Brand color from resources
	 *   int brandColor = ContextCompat.getColor(context, R.color.brand_primary);
	 *   PushwooshNotificationSettings.setEnableLED(true);
	 *   PushwooshNotificationSettings.setColorLED(brandColor);
	 * }
	 * </pre>
	 *
	 * @param color LED color as an integer (use {@link android.graphics.Color} or color resource)
	 * @see #setEnableLED(boolean)
	 */
	public static void setColorLED(@ColorInt int color) {
		if (checkIfInitializaed()) {
			PREFERENCES.ledColor().set(color);
		}
	}

	/**
	 * Sets the background color for the notification icon.
	 * <p>
	 * On Android 5.0 (Lollipop) and above, notification icons are displayed as white silhouettes
	 * on a colored circle background. This method sets that background color.
	 * <p>
	 * This is commonly used to match your app's brand colors.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Set brand color for notification icons
	 *   public class MyApplication extends Application {
	 *       @Override
	 *       public void onCreate() {
	 *           super.onCreate();
	 *
	 *           int brandColor = ContextCompat.getColor(this, R.color.brand_primary);
	 *           PushwooshNotificationSettings.setNotificationIconBackgroundColor(brandColor);
	 *       }
	 *   }
	 *
	 *   // Blue background for social app
	 *   PushwooshNotificationSettings.setNotificationIconBackgroundColor(Color.parseColor("#1DA1F2"));
	 *
	 *   // Green background for messaging app
	 *   PushwooshNotificationSettings.setNotificationIconBackgroundColor(Color.parseColor("#25D366"));
	 * }
	 * </pre>
	 *
	 * @param color background color as an integer (use {@link android.graphics.Color} or color resource)
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setSmallIcon(int)">Notification.Builder.setSmallIcon</a>
	 */
	public static void setNotificationIconBackgroundColor(@ColorInt int color) {
		if (checkIfInitializaed()) {
			PREFERENCES.iconBackgroundColor().set(color);
		}
	}

	/**
	 * Checks if push notifications are currently enabled.
	 * <p>
	 * Returns {@code true} only if both the system-level notifications are enabled for the app
	 * AND the app-level notification setting is enabled via {@link #enableNotifications(boolean)}.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Check notification status before showing opt-in UI
	 *   if (!PushwooshNotificationSettings.areNotificationsEnabled()) {
	 *       showNotificationPermissionDialog();
	 *   }
	 *
	 *   // Conditional feature based on notification status
	 *   if (PushwooshNotificationSettings.areNotificationsEnabled()) {
	 *       enableRealtimeUpdates();
	 *   } else {
	 *       Log.d("App", "Notifications disabled - using polling");
	 *   }
	 * }
	 * </pre>
	 *
	 * @return {@code true} if notifications are enabled and will be displayed, {@code false} otherwise
	 * @see #enableNotifications(boolean)
	 */
	public static boolean areNotificationsEnabled() {
		if (checkIfInitializaed()) {
			return NotificationUtils.areNotificationsEnabled() && PREFERENCES.notificationEnabled().get();
		}
		return false;
	}

	/**
	 * Enables or disables push notifications for the app.
	 * <p>
	 * This provides app-level control over whether push notifications are shown, independent
	 * of system notification permissions. When disabled, notifications will not be displayed
	 * even if system permissions are granted.
	 * <p>
	 * Use this for implementing user preferences or temporary "Do Not Disturb" modes.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // User preference toggle
	 *   public void onNotificationToggle(boolean enabled) {
	 *       PushwooshNotificationSettings.enableNotifications(enabled);
	 *
	 *       SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
	 *       prefs.edit().putBoolean("notifications_enabled", enabled).apply();
	 *
	 *       Toast.makeText(this,
	 *           enabled ? "Notifications enabled" : "Notifications disabled",
	 *           Toast.LENGTH_SHORT).show();
	 *   }
	 *
	 *   // Temporary disable during onboarding
	 *   public void startOnboarding() {
	 *       PushwooshNotificationSettings.enableNotifications(false);
	 *   }
	 *
	 *   public void finishOnboarding() {
	 *       PushwooshNotificationSettings.enableNotifications(true);
	 *   }
	 *
	 *   // Focus mode toggle
	 *   public void setFocusMode(boolean focusModeOn) {
	 *       if (focusModeOn) {
	 *           PushwooshNotificationSettings.enableNotifications(false);
	 *           Log.d("App", "Focus mode: notifications disabled");
	 *       } else {
	 *           PushwooshNotificationSettings.enableNotifications(true);
	 *           Log.d("App", "Focus mode off: notifications enabled");
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param on {@code true} to enable notifications, {@code false} to disable
	 * @see #areNotificationsEnabled()
	 */
	public static void enableNotifications(boolean on) {
		if (checkIfInitializaed()) {
			PREFERENCES.notificationEnabled().set(on);
		}
	}

	/**
	 * Sets the notification channel name for Android O (API 26) and above.
	 * <p>
	 * On Android 8.0 and higher, all notifications must belong to a notification channel.
	 * This method sets the name of the default Pushwoosh notification channel that users
	 * will see in system settings.
	 * <p>
	 * The channel name should be descriptive and help users understand what types of
	 * notifications they'll receive through this channel.
	 * <p>
	 * <b>Note:</b> This setting only affects Android O (API 26) and above. On older versions,
	 * this method has no effect.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   public class MyApplication extends Application {
	 *       @Override
	 *       public void onCreate() {
	 *           super.onCreate();
	 *
	 *           Pushwoosh.getInstance().registerForPushNotifications();
	 *
	 *           // Set channel name for different app types
	 *
	 *           // E-commerce app
	 *           PushwooshNotificationSettings.setNotificationChannelName("Order Updates");
	 *
	 *           // News app
	 *           // PushwooshNotificationSettings.setNotificationChannelName("Breaking News");
	 *
	 *           // Messaging app
	 *           // PushwooshNotificationSettings.setNotificationChannelName("Messages");
	 *
	 *           // General app
	 *           // PushwooshNotificationSettings.setNotificationChannelName("Notifications");
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param name the user-visible name for the notification channel
	 * @see <a href="https://developer.android.com/training/notify-user/channels">Android Notification Channels</a>
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
