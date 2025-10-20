package com.pushwoosh.notification;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import android.text.Html;
import android.text.TextUtils;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.notification.channel.NotificationChannelInfoProvider;
import com.pushwoosh.notification.channel.NotificationChannelManager;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

/**
 * Base class for customizing push notification appearance and behavior.
 * <p>
 * NotificationFactory allows you to control how push notifications are displayed to users by
 * overriding the notification generation process. You can customize notification icons, colors,
 * sounds, vibration patterns, LED indicators, and even the notification layout itself.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Custom notification layouts - Create your own notification designs with custom views</li>
 * <li>Dynamic content - Modify notification content based on app state or user preferences</li>
 * <li>Notification channels - Customize Android 8.0+ notification channel names and descriptions</li>
 * <li>Rich media handling - Override how images and other media are displayed in notifications</li>
 * <li>Helper methods - Use built-in methods for adding sounds, vibration, LED effects, and auto-cancel behavior</li>
 * </ul>
 * <p>
 * <b>Quick Start:</b>
 * <pre>
 * {@code
 *   // 1. Create custom factory
 *   public class CustomNotificationFactory extends NotificationFactory {
 *       @Override
 *       public Notification onGenerateNotification(@NonNull PushMessage data) {
 *           // Get notification channel (Android 8.0+)
 *           String channelId = addChannel(data);
 *
 *           // Build notification with custom styling
 *           NotificationCompat.Builder builder = new NotificationCompat.Builder(
 *               getApplicationContext(), channelId)
 *               .setContentTitle(data.getHeader())
 *               .setContentText(data.getMessage())
 *               .setSmallIcon(R.drawable.ic_notification)
 *               .setColor(0xFF6200EE); // Custom brand color
 *
 *           Notification notification = builder.build();
 *
 *           // Add sound, vibration, LED using helper methods
 *           addSound(notification, data.getSound());
 *           addVibration(notification, data.getVibration());
 *           addLED(notification, data.getLed(), data.getLedOnMS(), data.getLedOffMS());
 *           addCancel(notification); // Auto-dismiss when tapped
 *
 *           return notification;
 *       }
 *   }
 *
 *   // 2. Register in AndroidManifest.xml
 *   <application>
 *       <meta-data
 *           android:name="com.pushwoosh.notification_factory"
 *           android:value=".CustomNotificationFactory" />
 *   </application>
 * }
 * </pre>
 * <p>
 * <b>Important Requirements:</b>
 * <ul>
 * <li>Your factory class MUST be public</li>
 * <li>Your factory MUST have a public no-argument constructor</li>
 * <li>Application will crash on startup if these requirements are not met</li>
 * <li>The onGenerateNotification() method runs on a background thread</li>
 * </ul>
 * <p>
 * <b>Advanced Usage - Custom Notification Channels:</b>
 * <pre>
 * {@code
 *   public class CustomNotificationFactory extends NotificationFactory {
 *       @Override
 *       public String channelName(String channelName) {
 *           // Customize channel name based on push payload
 *           if (channelName.equals("urgent")) {
 *               return "Urgent Notifications";
 *           }
 *           return "General Notifications";
 *       }
 *
 *       @Override
 *       public String channelDescription(String channelName) {
 *           // Provide user-friendly descriptions
 *           return "Important updates about your account";
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * <b>See Also:</b>
 * <ul>
 * <li>{@link PushwooshNotificationFactory} - Default implementation showing best practices</li>
 * <li>{@link NotificationServiceExtension} - Alternative way to customize notifications before they are displayed</li>
 * <li>{@link PushMessage} - Contains all push notification data and metadata</li>
 * </ul>
 *
 * @see #onGenerateNotification(PushMessage)
 * @see #getNotificationIntent(PushMessage)
 * @see PushwooshNotificationFactory
 * @see NotificationServiceExtension
 */
public abstract class NotificationFactory {

	@Nullable
	private final Context applicationContext;
	private NotificationChannelManager notificationChannelManager;

	@SuppressWarnings("WeakerAccess")
	public NotificationFactory() {
		applicationContext = AndroidPlatformModule.getApplicationContext();
		notificationChannelManager = new NotificationChannelManager(applicationContext);
	}

	/**
	 * Creates and configures a notification from push message data.
	 * <p>
	 * This is the main method you must implement to customize notification appearance. The SDK calls
	 * this method on a background thread when a push notification arrives. You should build and return
	 * a fully configured {@link Notification} object, or return null to prevent the notification from
	 * being displayed.
	 * <p>
	 * <b>Thread Safety:</b> This method is called on a worker thread, so it's safe to perform network
	 * requests or other blocking operations (e.g., downloading notification images).
	 * <br><br>
	 * Example - Basic custom notification:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       // Create notification channel (required for Android 8.0+)
	 *       String channelId = addChannel(data);
	 *
	 *       // Build notification with standard Android APIs
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *
	 *       // Apply notification behaviors using helper methods
	 *       addSound(notification, data.getSound());
	 *       addVibration(notification, data.getVibration());
	 *       addCancel(notification); // Dismiss when user taps
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Advanced with custom image loading:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *
	 *       // Load large icon from URL (safe - running on worker thread)
	 *       Bitmap icon = null;
	 *       String iconUrl = data.getLargeIconUrl();
	 *       if (iconUrl != null) {
	 *           try {
	 *               icon = Glide.with(getApplicationContext())
	 *                   .asBitmap()
	 *                   .load(iconUrl)
	 *                   .submit(256, 256)
	 *                   .get();
	 *           } catch (Exception e) {
	 *               Log.e("CustomFactory", "Failed to load icon", e);
	 *           }
	 *       }
	 *
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification)
	 *           .setLargeIcon(icon)
	 *           .setPriority(NotificationCompat.PRIORITY_HIGH);
	 *
	 *       Notification notification = builder.build();
	 *       addSound(notification, data.getSound());
	 *       addVibration(notification, data.getVibration());
	 *       addCancel(notification);
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Conditional notification display:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       // Check custom data to decide whether to show notification
	 *       String customData = data.getCustomData();
	 *       if (customData != null && customData.contains("silent")) {
	 *           return null; // Don't show notification
	 *       }
	 *
	 *       // Check app settings
	 *       SharedPreferences prefs = getApplicationContext()
	 *           .getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
	 *       boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
	 *
	 *       if (!notificationsEnabled) {
	 *           return null; // User disabled notifications in app settings
	 *       }
	 *
	 *       // Show notification normally
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *       addCancel(notification);
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 *
	 * @param data Push notification data containing message, title, icons, sounds, and custom payload
	 * @return Configured notification to display, or null to suppress the notification
	 *
	 * @see #addChannel(PushMessage)
	 * @see #addSound(Notification, String)
	 * @see #addVibration(Notification, boolean)
	 * @see #addLED(Notification, Integer, int, int)
	 * @see #addCancel(Notification)
	 * @see PushMessage
	 */
	@WorkerThread
	@Nullable
	public abstract Notification onGenerateNotification(@NonNull PushMessage data);

	/**
	 * Creates the intent that will be fired when the user taps the notification.
	 * <p>
	 * By default, this method returns an intent that opens the app and triggers push notification
	 * click tracking. Override this method to customize what happens when users tap notifications,
	 * such as opening a specific activity or deep link.
	 * <p>
	 * <b>Important:</b> To maintain push statistics tracking, your custom intent should still include
	 * the notification bundle data using {@link NotificationIntentHelper#EXTRA_NOTIFICATION_BUNDLE}.
	 * <br><br>
	 * Example - Open specific activity on notification tap:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Intent getNotificationIntent(@NonNull PushMessage data) {
	 *       // Parse custom data to determine target screen
	 *       String customData = data.getCustomData();
	 *       Intent intent;
	 *
	 *       try {
	 *           JSONObject json = new JSONObject(customData);
	 *           String screen = json.optString("target_screen");
	 *
	 *           if ("profile".equals(screen)) {
	 *               // Open profile activity directly
	 *               intent = new Intent(getApplicationContext(), ProfileActivity.class);
	 *           } else if ("chat".equals(screen)) {
	 *               // Open chat with specific user
	 *               String userId = json.optString("user_id");
	 *               intent = new Intent(getApplicationContext(), ChatActivity.class);
	 *               intent.putExtra("user_id", userId);
	 *           } else {
	 *               // Default: open main activity
	 *               intent = new Intent(getApplicationContext(), MainActivity.class);
	 *           }
	 *       } catch (Exception e) {
	 *           intent = new Intent(getApplicationContext(), MainActivity.class);
	 *       }
	 *
	 *       // Required: Attach notification data for tracking
	 *       intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE, data.toBundle());
	 *       intent.setAction(Long.toString(System.currentTimeMillis()));
	 *       intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
	 *
	 *       return intent;
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Handle deep links:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Intent getNotificationIntent(@NonNull PushMessage data) {
	 *       String customData = data.getCustomData();
	 *       Intent intent;
	 *
	 *       try {
	 *           JSONObject json = new JSONObject(customData);
	 *           String deepLink = json.optString("deep_link");
	 *
	 *           if (!TextUtils.isEmpty(deepLink)) {
	 *               // Handle deep link
	 *               intent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLink));
	 *           } else {
	 *               intent = new Intent(getApplicationContext(), MainActivity.class);
	 *           }
	 *       } catch (Exception e) {
	 *           intent = new Intent(getApplicationContext(), MainActivity.class);
	 *       }
	 *
	 *       intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE, data.toBundle());
	 *       intent.setAction(Long.toString(System.currentTimeMillis()));
	 *
	 *       return intent;
	 *   }
	 * }
	 * </pre>
	 *
	 * @param data Push notification data with message content and custom payload
	 * @return Intent to be fired when the user taps the notification
	 *
	 * @see NotificationIntentHelper#EXTRA_NOTIFICATION_BUNDLE
	 * @see PushMessage#getCustomData()
	 */
	@NonNull
	public Intent getNotificationIntent(@NonNull PushMessage data) {
		Intent intent = new Intent(applicationContext, NotificationOpenActivity.class);
		intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE, data.toBundle());
		intent.setAction(Long.toString(System.currentTimeMillis()));

		return intent;
	}

	/**
	 * Customizes the display name of a notification channel.
	 * <p>
	 * On Android 8.0 (API 26) and higher, notifications are organized into channels that users can
	 * manage in system settings. Override this method to provide user-friendly channel names based
	 * on the channel identifier sent in the push payload.
	 * <p>
	 * The channel name appears in the notification settings UI where users control notification
	 * behavior. Choose clear, descriptive names that help users understand what notifications
	 * they'll receive in each channel.
	 * <p>
	 * <b>Note:</b> Returning an empty string will cause the SDK to use the default channel name.
	 * Channel names cannot be changed after the channel is created.
	 * <br><br>
	 * Example - Map channel IDs to friendly names:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public String channelName(String channelName) {
	 *       // channelName comes from "pw_channel" in push payload
	 *       switch (channelName) {
	 *           case "orders":
	 *               return "Order Updates";
	 *           case "promotions":
	 *               return "Deals and Offers";
	 *           case "urgent":
	 *               return "Urgent Notifications";
	 *           default:
	 *               return "General Notifications";
	 *       }
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Localized channel names:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public String channelName(String channelName) {
	 *       Context context = getApplicationContext();
	 *
	 *       if ("orders".equals(channelName)) {
	 *           return context.getString(R.string.channel_orders);
	 *       } else if ("promotions".equals(channelName)) {
	 *           return context.getString(R.string.channel_promotions);
	 *       }
	 *
	 *       return context.getString(R.string.channel_default);
	 *   }
	 * }
	 * </pre>
	 *
	 * @param channelName Channel identifier from the push payload's "pw_channel" attribute,
	 *                    or the default channel name if not specified
	 * @return User-friendly name to display in notification settings. Empty string will be
	 *         ignored and the default channel name will be used
	 *
	 * @see #channelDescription(String)
	 * @see #addChannel(PushMessage)
	 */
	public String channelName(String channelName) {
		return channelName;
	}

	/**
	 * Provides a description for a notification channel.
	 * <p>
	 * Channel descriptions appear below the channel name in notification settings, helping users
	 * understand what types of notifications they'll receive. Descriptions should be brief but
	 * informative, explaining the purpose of the channel.
	 * <p>
	 * <b>Note:</b> Channel descriptions cannot be changed after the channel is created.
	 * This method is only called on Android 8.0 (API 26) and higher.
	 * <br><br>
	 * Example - Provide helpful descriptions:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public String channelDescription(String channelName) {
	 *       switch (channelName) {
	 *           case "orders":
	 *               return "Order confirmations, shipping updates, and delivery notifications";
	 *           case "promotions":
	 *               return "Special offers, discounts, and promotional campaigns";
	 *           case "urgent":
	 *               return "Time-sensitive alerts requiring immediate attention";
	 *           default:
	 *               return "General app notifications and updates";
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param channelName Channel identifier from the push payload's "pw_channel" attribute,
	 *                    or the default channel name if not specified
	 * @return Description text to show in notification settings, or null/empty for no description
	 *
	 * @see #channelName(String)
	 */
	@Nullable
	@SuppressWarnings("WeakerAccess")
	public String channelDescription(String channelName) {
		return "";
	}

	/**
	 * Makes the notification automatically dismiss when the user taps it.
	 * <p>
	 * By default, notifications remain in the notification shade after being tapped. This helper
	 * method adds the {@link Notification#FLAG_AUTO_CANCEL} flag, which causes the notification
	 * to be automatically removed when the user taps on it.
	 * <p>
	 * <b>Best Practice:</b> Most notifications should be auto-cancelable to avoid cluttering the
	 * notification shade. Only persistent notifications (like music playback or downloads) should
	 * remain after being tapped.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *       addCancel(notification); // Dismiss when tapped
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 *
	 * @param notification The notification to make auto-cancelable
	 *
	 * @see Notification#FLAG_AUTO_CANCEL
	 */
	protected final void addCancel(@NonNull Notification notification) {
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
	}

	/**
	 * Adds LED blinking effect to the notification.
	 * <p>
	 * On devices with notification LEDs, this method configures the LED color and blink pattern.
	 * The LED provides a visual indicator that a notification has arrived, useful when the device
	 * screen is off.
	 * <p>
	 * <b>Android 8.0+ Note:</b> LED settings are controlled by notification channels and cannot be
	 * changed after the channel is created. This method works best for new channels or on
	 * devices running Android 7.1 and lower.
	 * <p>
	 * <b>Important:</b> LED functionality depends on the device having a notification LED and the
	 * user's system settings. Many modern devices have removed physical notification LEDs.
	 * <br><br>
	 * Example - Add custom LED color:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *
	 *       // Blue LED blinking: 1 second on, 2 seconds off
	 *       addLED(notification, 0xFF0000FF, 1000, 2000);
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Use LED from push payload:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *
	 *       // Use LED settings from push payload
	 *       addLED(notification, data.getLed(), data.getLedOnMS(), data.getLedOffMS());
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 *
	 * @param notification The notification to add LED effect to
	 * @param color LED color in ARGB format (e.g., 0xFFFF0000 for red), or null to use default color
	 * @param ledOnMs Duration the LED should be lit in milliseconds
	 * @param ledOffMs Duration the LED should be off in milliseconds
	 *
	 * @see PushMessage#getLed()
	 * @see PushMessage#getLedOnMS()
	 * @see PushMessage#getLedOffMS()
	 */
	@SuppressWarnings("WeakerAccess")
	protected final void addLED(@NonNull Notification notification, @Nullable Integer color, int ledOnMs, int ledOffMs) {
		NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
		boolean enabled = notificationPrefs.ledEnabled().get();
		int defaultColor = notificationPrefs.ledColor().get();

		if (!enabled && color == null) {
			return;
		}

		notificationChannelManager.addLED(notification, color == null ? defaultColor : color, ledOnMs, ledOffMs);
	}

	/**
	 * Adds vibration to the notification.
	 * <p>
	 * Configures whether the device should vibrate when this notification is displayed. Vibration
	 * provides tactile feedback that a notification has arrived, useful when the device is in
	 * the user's pocket or on silent mode.
	 * <p>
	 * The vibration pattern is controlled by the {@link VibrateType} setting configured through
	 * {@link PushwooshNotificationSettings}. You can customize the vibration pattern globally or
	 * per notification.
	 * <p>
	 * <b>Android 8.0+ Note:</b> Vibration settings are controlled by notification channels and
	 * cannot be changed after the channel is created.
	 * <p>
	 * <b>Permission Required:</b> The app needs the VIBRATE permission in AndroidManifest.xml.
	 * <br><br>
	 * Example - Enable vibration:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *
	 *       // Enable vibration using push payload setting
	 *       addVibration(notification, data.getVibration());
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Conditional vibration based on priority:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *
	 *       // Only vibrate for high-priority notifications
	 *       boolean shouldVibrate = data.getPriority() >= NotificationCompat.PRIORITY_HIGH;
	 *       addVibration(notification, shouldVibrate);
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 *
	 * @param notification The notification to add vibration to
	 * @param vibration true to enable vibration, false to disable
	 *
	 * @see PushMessage#getVibration()
	 * @see PushwooshNotificationSettings
	 * @see VibrateType
	 */
	protected final void addVibration(@NonNull Notification notification, boolean vibration) {
		NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
		VibrateType vibrateType = notificationPrefs.vibrateType().get();

		notificationChannelManager.addVibration(notification, vibrateType, vibration);
	}

	/**
	 * Adds a sound to the notification.
	 * <p>
	 * Configures the audio alert that plays when the notification is displayed. You can use the
	 * default system sound, a custom sound from your app resources, or disable sound entirely.
	 * <p>
	 * <b>Sound File Locations:</b> Custom sounds should be placed in:
	 * <ul>
	 * <li>res/raw/ directory (recommended)</li>
	 * <li>assets/www/res/ directory</li>
	 * </ul>
	 * <p>
	 * <b>Android 8.0+ Note:</b> Sound settings are controlled by notification channels and cannot
	 * be changed after the channel is created.
	 * <br><br>
	 * Example - Use default system sound:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *
	 *       // Use default system sound
	 *       addSound(notification, null);
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Custom sound from resources:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *
	 *       // Use custom sound from res/raw/notification_sound.mp3
	 *       addSound(notification, "notification_sound");
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Conditional sound based on message type:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       Notification notification = builder.build();
	 *
	 *       // Check custom data for message type
	 *       String customData = data.getCustomData();
	 *       if (customData != null && customData.contains("urgent")) {
	 *           addSound(notification, "urgent_alert"); // Urgent sound
	 *       } else if (customData != null && customData.contains("silent")) {
	 *           addSound(notification, ""); // No sound
	 *       } else {
	 *           addSound(notification, data.getSound()); // Use payload sound
	 *       }
	 *
	 *       return notification;
	 *   }
	 * }
	 * </pre>
	 *
	 * @param notification The notification to add sound to
	 * @param sound Sound resource name from res/raw or assets/www/res directory.
	 *              If null or file doesn't exist, default system sound will be played.
	 *              If empty string, no sound will be played.
	 *
	 * @see PushMessage#getSound()
	 */
	protected final void addSound(@NonNull Notification notification, @Nullable String sound) {
		Uri customSound = NotificationUtils.getSoundUri(sound);
		if (customSound != null) {
			notificationChannelManager.addSound(notification, customSound, sound == null);
		}
	}

	/**
	 * Returns the application context.
	 * <p>
	 * Provides access to the application-level context for building notifications, accessing
	 * resources, and performing other context-dependent operations. This context is safe to use
	 * in background threads and won't leak activity references.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       Context context = getApplicationContext();
	 *
	 *       // Access resources
	 *       String appName = context.getString(R.string.app_name);
	 *
	 *       // Check preferences
	 *       SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
	 *       boolean enableSound = prefs.getBoolean("notification_sound", true);
	 *
	 *       // Build notification
	 *       String channelId = addChannel(data);
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       return builder.build();
	 *   }
	 * }
	 * </pre>
	 *
	 * @return Application context, or null if not available
	 */
	@Nullable
	protected final Context getApplicationContext() {
		return applicationContext;
	}

	/**
	 * Converts HTML-formatted string to styled CharSequence for display in notifications.
	 * <p>
	 * Push notification messages can contain HTML formatting (bold, italic, etc.). This helper
	 * method parses HTML tags and returns a styled CharSequence that preserves the formatting
	 * when displayed in the notification.
	 * <p>
	 * <b>Supported HTML tags:</b> &lt;b&gt;, &lt;i&gt;, &lt;u&gt;, &lt;big&gt;, &lt;small&gt;,
	 * and other basic formatting tags supported by {@link Html#fromHtml}.
	 * <br><br>
	 * Example - Display formatted notification text:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       String channelId = addChannel(data);
	 *
	 *       // Convert HTML to styled text
	 *       CharSequence title = getContentFromHtml(data.getHeader());
	 *       CharSequence message = getContentFromHtml(data.getMessage());
	 *
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(title)  // Preserves bold, italic, etc.
	 *           .setContentText(message)
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       return builder.build();
	 *   }
	 * }
	 * </pre>
	 *
	 * @param content HTML-formatted string from push notification message
	 * @return Styled CharSequence with HTML formatting applied, or the original string if empty
	 *
	 * @see PushMessage#getHeader()
	 * @see PushMessage#getMessage()
	 * @see PushMessage#getTicker()
	 */
	protected final CharSequence getContentFromHtml(String content) {
		return TextUtils.isEmpty(content) ? content : Html.fromHtml(content);
	}

	/**
	 * Creates a notification channel for the push message if it doesn't already exist.
	 * <p>
	 * On Android 8.0 (API 26) and higher, all notifications must be assigned to a channel. This
	 * method automatically creates the appropriate channel based on the push message payload's
	 * "pw_channel" attribute, or creates a default channel if no channel is specified.
	 * <p>
	 * Channel settings (name, description, sound, vibration, LED) are configured when the channel
	 * is first created and cannot be changed programmatically afterwards. Users can modify these
	 * settings through the system notification settings UI.
	 * <p>
	 * <b>Channel Customization:</b> Override {@link #channelName(String)} and
	 * {@link #channelDescription(String)} to customize channel display names and descriptions.
	 * <p>
	 * <b>Android 7.1 and lower:</b> This method returns a channel ID for compatibility but doesn't
	 * create an actual channel (channels don't exist on these versions).
	 * <br><br>
	 * Example - Basic usage:
	 * <pre>
	 * {@code
	 *   @Override
	 *   public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *       // Create channel (required for Android 8.0+)
	 *       String channelId = addChannel(data);
	 *
	 *       // Use channel ID when building notification
	 *       NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *           getApplicationContext(), channelId)
	 *           .setContentTitle(data.getHeader())
	 *           .setContentText(data.getMessage())
	 *           .setSmallIcon(R.drawable.ic_notification);
	 *
	 *       return builder.build();
	 *   }
	 * }
	 * </pre>
	 * <br>
	 * Example - Multiple channels with custom names:
	 * <pre>
	 * {@code
	 *   public class CustomNotificationFactory extends NotificationFactory {
	 *       @Override
	 *       public String channelName(String channelName) {
	 *           // Map technical IDs to user-friendly names
	 *           if ("promo".equals(channelName)) {
	 *               return "Promotions";
	 *           } else if ("orders".equals(channelName)) {
	 *               return "Order Updates";
	 *           }
	 *           return "Notifications";
	 *       }
	 *
	 *       @Override
	 *       public Notification onGenerateNotification(@NonNull PushMessage data) {
	 *           // Channel created with custom name from channelName() method
	 *           String channelId = addChannel(data);
	 *
	 *           NotificationCompat.Builder builder = new NotificationCompat.Builder(
	 *               getApplicationContext(), channelId)
	 *               .setContentTitle(data.getHeader())
	 *               .setContentText(data.getMessage())
	 *               .setSmallIcon(R.drawable.ic_notification);
	 *
	 *           return builder.build();
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param pushMessage Push message containing channel information in "pw_channel" attribute.
	 *                    If not specified, a default channel will be created.
	 * @return Channel ID to use when building the notification. On Android 7.1 and lower, returns
	 *         a placeholder ID (no actual channel is created).
	 *
	 * @see #channelName(String)
	 * @see #channelDescription(String)
	 * @see NotificationChannelInfoProvider#getChannelName(PushMessage)
	 */
	@SuppressWarnings("WeakerAccess")
	protected String addChannel(PushMessage pushMessage) {
		String pushChannelName = NotificationChannelInfoProvider.getChannelName(pushMessage);
		return notificationChannelManager.addChannel(pushMessage, channelName(pushChannelName), channelDescription(pushChannelName));
	}
}
