package com.pushwoosh.notification;

import android.os.Bundle;

/**
 * Represents a local notification that can be scheduled to be shown at a specific time in the future.
 * <p>
 * Local notifications are displayed by the Android system without requiring server-side push delivery.
 * They are useful for app-initiated reminders, time-based alerts, and user engagement features that
 * don't depend on network connectivity.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Schedule notifications to appear after a specified delay</li>
 * <li>Support all push notification features: images, links, custom data</li>
 * <li>Manage scheduled notifications via {@link LocalNotificationRequest}</li>
 * <li>Compatible with notification settings and multi-notification mode</li>
 * </ul>
 * <p>
 * <b>Quick Start:</b>
 * <pre>
 * {@code
 *   // Schedule a workout reminder for 1 hour from now
 *   LocalNotification reminder = new LocalNotification.Builder()
 *       .setMessage("Time for your daily workout!")
 *       .setDelay(3600) // 1 hour in seconds
 *       .build();
 *
 *   LocalNotificationRequest request = Pushwoosh.getInstance()
 *       .scheduleLocalNotification(reminder);
 *
 *   // Save request ID to cancel later if needed
 *   int requestId = request.getRequestId();
 * }
 * </pre>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>Delay is specified in <b>seconds</b>, not milliseconds</li>
 * <li>Uses the same notification format as remote push notifications</li>
 * <li>Notifications persist across app restarts (stored in database)</li>
 * <li>Use {@link Builder#setTag(String)} to control notification replacement behavior</li>
 * <li>Local notifications are subject to Android system battery optimization</li>
 * </ul>
 *
 * @see Builder
 * @see LocalNotificationRequest
 * @see com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)
 */
public class LocalNotification {
	private int delay;
	private Bundle extras = new Bundle();

	private LocalNotification() {
		PushBundleDataProvider.setLocal(extras, true);
	}

	int getDelay() {
		return delay;
	}

	private void setDelay(int delay) {
		this.delay = delay;
	}

	Bundle getExtras() {
		return extras;
	}

	/**
	 * Builder for creating {@link LocalNotification} instances.
	 * <p>
	 * Provides a fluent API for configuring all aspects of a local notification including
	 * message content, delay, visual elements (icons, images), and behavior (tags, links).
	 * <p>
	 * <b>Common Usage Patterns:</b>
	 * <pre>
	 * {@code
	 *   // Basic reminder
	 *   LocalNotification basic = new LocalNotification.Builder()
	 *       .setMessage("Don't forget to check your cart!")
	 *       .setDelay(7200) // 2 hours
	 *       .build();
	 *
	 *   // Rich notification with image and deep link
	 *   LocalNotification rich = new LocalNotification.Builder()
	 *       .setMessage("50% off flash sale is live!")
	 *       .setTag("flash_sale")
	 *       .setDelay(86400) // 24 hours
	 *       .setBanner("https://example.com/sale-banner.jpg")
	 *       .setLink("myapp://products/sale")
	 *       .build();
	 *
	 *   // Notification with custom data
	 *   Bundle customData = new Bundle();
	 *   customData.putString("screen", "workout_detail");
	 *   customData.putInt("workout_id", 123);
	 *
	 *   LocalNotification withData = new LocalNotification.Builder()
	 *       .setMessage("Ready for your workout?")
	 *       .setDelay(3600) // 1 hour
	 *       .setExtras(customData)
	 *       .build();
	 * }
	 * </pre>
	 *
	 * @see LocalNotification
	 * @see #build()
	 */
	public static class Builder {
		private LocalNotification mLocalNotification;

		public Builder() {
			mLocalNotification = new LocalNotification();
		}

		/**
		 * Sets a unique tag to identify and manage this notification.
		 * <p>
		 * Tags control notification replacement behavior:
		 * <ul>
		 * <li>Notifications with <b>different tags</b> will appear as separate notifications</li>
		 * <li>Notifications with the <b>same tag</b> will replace each other (unless multi-notification mode is enabled)</li>
		 * </ul>
		 * This is useful for updating existing notifications or preventing notification spam.
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // First notification with tag "cart_reminder"
		 *   LocalNotification firstReminder = new LocalNotification.Builder()
		 *       .setTag("cart_reminder")
		 *       .setMessage("You have 3 items in your cart")
		 *       .setDelay(3600)
		 *       .build();
		 *   Pushwoosh.getInstance().scheduleLocalNotification(firstReminder);
		 *
		 *   // Second notification with same tag - will replace the first one
		 *   LocalNotification updatedReminder = new LocalNotification.Builder()
		 *       .setTag("cart_reminder")
		 *       .setMessage("You have 5 items in your cart")
		 *       .setDelay(7200)
		 *       .build();
		 *   Pushwoosh.getInstance().scheduleLocalNotification(updatedReminder);
		 *
		 *   // Different tag - will appear as separate notification
		 *   LocalNotification saleNotification = new LocalNotification.Builder()
		 *       .setTag("flash_sale")
		 *       .setMessage("Flash sale ends soon!")
		 *       .setDelay(1800)
		 *       .build();
		 *   Pushwoosh.getInstance().scheduleLocalNotification(saleNotification);
		 * }
		 * </pre>
		 *
		 * @param tag notification tag (should be unique per notification type)
		 * @return this builder instance for method chaining
		 * @see PushwooshNotificationSettings#setMultiNotificationMode(boolean)
		 */
		public Builder setTag(String tag) {
			PushBundleDataProvider.setTag(mLocalNotification.getExtras(), tag);
			return this;
		}

		/**
		 * Sets the main text content of the notification.
		 * <p>
		 * This is the primary message that will be displayed in the notification. Keep it concise
		 * and actionable to encourage user engagement.
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // E-commerce reminder
		 *   LocalNotification cartReminder = new LocalNotification.Builder()
		 *       .setMessage("Your cart is waiting! Complete your purchase now.")
		 *       .setDelay(7200) // 2 hours
		 *       .build();
		 *
		 *   // Fitness app reminder
		 *   LocalNotification workoutReminder = new LocalNotification.Builder()
		 *       .setMessage("Time for your daily 30-minute workout!")
		 *       .setDelay(3600)
		 *       .build();
		 *
		 *   // News app engagement
		 *   LocalNotification newsAlert = new LocalNotification.Builder()
		 *       .setMessage("5 new articles in your favorite topics")
		 *       .setDelay(86400) // 24 hours
		 *       .build();
		 * }
		 * </pre>
		 *
		 * @param message notification text message
		 * @return this builder instance for method chaining
		 */
		public Builder setMessage(String message) {
			PushBundleDataProvider.setMessage(mLocalNotification.getExtras(), message);
			return this;
		}

		/**
		 * Sets the delay after which the notification will be displayed.
		 * <p>
		 * <b>Important:</b> Delay is specified in <b>seconds</b>, not milliseconds.
		 * The notification will be scheduled using Android's {@link android.app.AlarmManager}
		 * and will be displayed after the specified delay from the moment {@link com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)}
		 * is called.
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // Schedule notification for 5 minutes from now
		 *   LocalNotification fiveMinutes = new LocalNotification.Builder()
		 *       .setMessage("5 minute reminder")
		 *       .setDelay(300) // 5 minutes = 300 seconds
		 *       .build();
		 *
		 *   // Schedule notification for 1 hour from now
		 *   LocalNotification oneHour = new LocalNotification.Builder()
		 *       .setMessage("1 hour reminder")
		 *       .setDelay(3600) // 1 hour = 3600 seconds
		 *       .build();
		 *
		 *   // Schedule notification for 24 hours from now
		 *   LocalNotification oneDay = new LocalNotification.Builder()
		 *       .setMessage("Daily reminder")
		 *       .setDelay(86400) // 24 hours = 86400 seconds
		 *       .build();
		 *
		 *   // Immediate notification (show in 1 second)
		 *   LocalNotification immediate = new LocalNotification.Builder()
		 *       .setMessage("Immediate notification")
		 *       .setDelay(1) // Minimum practical delay
		 *       .build();
		 * }
		 * </pre>
		 * <p>
		 * <b>Note:</b> On Android 6.0+ (API 23+), battery optimization may delay exact timing.
		 * For time-critical notifications, consider using WorkManager or AlarmManager's exact alarm APIs.
		 *
		 * @param delay delay in seconds (must be positive)
		 * @return this builder instance for method chaining
		 */
		public Builder setDelay(int delay) {
			mLocalNotification.setDelay(delay);
			return this;
		}

		/**
		 * Sets a URL or deep link that will be opened when the user taps the notification.
		 * <p>
		 * This allows you to direct users to specific screens in your app or open external links in a browser.
		 * <ul>
		 * <li><b>HTTP/HTTPS URLs:</b> Open in the default browser</li>
		 * <li><b>Deep links:</b> Navigate to specific app screens (e.g., "myapp://product/123")</li>
		 * <li><b>If not set:</b> Opens the default launcher activity</li>
		 * </ul>
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // Open a web page
		 *   LocalNotification webLink = new LocalNotification.Builder()
		 *       .setMessage("Check out our new collection!")
		 *       .setLink("https://example.com/new-collection")
		 *       .setDelay(3600)
		 *       .build();
		 *
		 *   // Navigate to specific product screen
		 *   LocalNotification productLink = new LocalNotification.Builder()
		 *       .setMessage("Your favorite item is back in stock!")
		 *       .setLink("myapp://products/12345")
		 *       .setDelay(7200)
		 *       .build();
		 *
		 *   // Navigate to cart screen
		 *   LocalNotification cartLink = new LocalNotification.Builder()
		 *       .setMessage("Complete your purchase - items in cart!")
		 *       .setLink("myapp://cart")
		 *       .setDelay(86400)
		 *       .build();
		 *
		 *   // Navigate to workout detail
		 *   LocalNotification workoutLink = new LocalNotification.Builder()
		 *       .setMessage("Time for today's workout")
		 *       .setLink("fitnessapp://workout/daily-routine")
		 *       .setDelay(3600)
		 *       .build();
		 * }
		 * </pre>
		 * <p>
		 * <b>Note:</b> Make sure your app's deep link scheme is properly configured in AndroidManifest.xml
		 * to handle custom URL schemes.
		 *
		 * @param url URL or deep link (e.g., "https://example.com" or "myapp://screen/id")
		 * @return this builder instance for method chaining
		 * @see com.pushwoosh.Pushwoosh#getLaunchNotification()
		 */
		public Builder setLink(String url) {
			PushBundleDataProvider.setLink(mLocalNotification.getExtras(), url);
			return this;
		}

		/**
		 * Sets a large image to be displayed in the notification using Android's BigPictureStyle.
		 * <p>
		 * The banner image is displayed when the notification is expanded and provides a rich visual
		 * experience. The image is downloaded and cached when the notification is displayed.
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // E-commerce flash sale with product image
		 *   LocalNotification saleNotification = new LocalNotification.Builder()
		 *       .setMessage("50% off on selected items!")
		 *       .setBanner("https://cdn.example.com/flash-sale-banner.jpg")
		 *       .setLink("myapp://sale")
		 *       .setDelay(3600)
		 *       .build();
		 *
		 *   // News article with feature image
		 *   LocalNotification newsAlert = new LocalNotification.Builder()
		 *       .setMessage("Breaking: Major event update")
		 *       .setBanner("https://news.example.com/article-image.jpg")
		 *       .setLink("newsapp://article/12345")
		 *       .setDelay(1800)
		 *       .build();
		 *
		 *   // Fitness achievement with motivational image
		 *   LocalNotification achievement = new LocalNotification.Builder()
		 *       .setMessage("Congratulations! You reached your goal!")
		 *       .setBanner("https://cdn.fitnessapp.com/achievement-badge.png")
		 *       .setDelay(7200)
		 *       .build();
		 * }
		 * </pre>
		 * <p>
		 * <b>Best Practices:</b>
		 * <ul>
		 * <li>Use high-resolution images (at least 450x450 pixels)</li>
		 * <li>Keep file size reasonable (under 1MB) for faster loading</li>
		 * <li>Use JPEG or PNG formats</li>
		 * <li>Images are downloaded when notification is displayed, so ensure URLs are accessible</li>
		 * </ul>
		 *
		 * @param url image URL (must be publicly accessible)
		 * @return this builder instance for method chaining
		 * @see <a href="https://developer.android.com/reference/android/app/Notification.BigPictureStyle.html">Android BigPictureStyle</a>
		 */
		public Builder setBanner(String url) {
			PushBundleDataProvider.setBanner(mLocalNotification.getExtras(), url);
			return this;
		}

		/**
		 * Sets the small icon displayed in the notification status bar and notification content.
		 * <p>
		 * The small icon appears in the device's status bar and as a small icon in the notification itself.
		 * This should be a simple, monochromatic icon from your app's drawable resources.
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // Use custom notification icon
		 *   LocalNotification withIcon = new LocalNotification.Builder()
		 *       .setMessage("New message received")
		 *       .setSmallIcon("ic_notification") // Refers to res/drawable/ic_notification.png
		 *       .setDelay(300)
		 *       .build();
		 *
		 *   // Use different icons for different notification types
		 *   LocalNotification cartIcon = new LocalNotification.Builder()
		 *       .setMessage("Items in your cart")
		 *       .setSmallIcon("ic_cart")
		 *       .setDelay(3600)
		 *       .build();
		 *
		 *   LocalNotification saleIcon = new LocalNotification.Builder()
		 *       .setMessage("Flash sale alert!")
		 *       .setSmallIcon("ic_sale")
		 *       .setDelay(7200)
		 *       .build();
		 * }
		 * </pre>
		 * <p>
		 * <b>Best Practices:</b>
		 * <ul>
		 * <li>Use white icon with transparent background for Android 5.0+ (API 21+)</li>
		 * <li>Provide multiple densities (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)</li>
		 * <li>Recommended size: 24x24 dp</li>
		 * <li>Keep design simple - complex icons don't work well at small sizes</li>
		 * <li>If not set, SDK uses the app's default notification icon</li>
		 * </ul>
		 *
		 * @param name drawable resource name (without extension, e.g., "ic_notification")
		 * @return this builder instance for method chaining
		 */
		public Builder setSmallIcon(String name) {
			PushBundleDataProvider.setSmallIcon(mLocalNotification.getExtras(), name);
			return this;
		}

		/**
		 * Sets the large icon displayed on the left side of the notification.
		 * <p>
		 * The large icon is a circular image displayed prominently in the notification. It's typically
		 * used for user avatars, app branding, or context-specific imagery. The image is downloaded
		 * from the provided URL when the notification is displayed.
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // E-commerce notification with product thumbnail
		 *   LocalNotification productNotification = new LocalNotification.Builder()
		 *       .setMessage("Your favorite item is back in stock!")
		 *       .setLargeIcon("https://cdn.example.com/products/item-thumb.jpg")
		 *       .setLink("myapp://products/12345")
		 *       .setDelay(3600)
		 *       .build();
		 *
		 *   // News app with article thumbnail
		 *   LocalNotification newsNotification = new LocalNotification.Builder()
		 *       .setMessage("Breaking news update")
		 *       .setLargeIcon("https://news.example.com/article-thumb.jpg")
		 *       .setLink("newsapp://article/67890")
		 *       .setDelay(1800)
		 *       .build();
		 *
		 *   // Fitness app with achievement badge
		 *   LocalNotification achievementNotification = new LocalNotification.Builder()
		 *       .setMessage("You earned a new badge!")
		 *       .setLargeIcon("https://cdn.fitnessapp.com/badges/runner-gold.png")
		 *       .setDelay(7200)
		 *       .build();
		 * }
		 * </pre>
		 * <p>
		 * <b>Best Practices:</b>
		 * <ul>
		 * <li>Use square images (Android will automatically crop to circular)</li>
		 * <li>Recommended size: 256x256 pixels or larger</li>
		 * <li>Keep file size reasonable (under 500KB) for faster loading</li>
		 * <li>Image is displayed in a circle on Android 5.0+ (API 21+)</li>
		 * <li>URL must be publicly accessible when notification is displayed</li>
		 * </ul>
		 *
		 * @param url image URL (must be publicly accessible)
		 * @return this builder instance for method chaining
		 */
		public Builder setLargeIcon(String url) {
			PushBundleDataProvider.setLargeIcon(mLocalNotification.getExtras(), url);
			return this;
		}

		/**
		 * Adds custom data to the notification that can be retrieved when the notification is opened.
		 * <p>
		 * This allows you to attach arbitrary key-value data to the notification, which can be used
		 * for navigation, analytics, or any custom logic when the user taps the notification.
		 * <p>
		 * <b>Warning:</b> This method merges the provided Bundle with existing notification data.
		 * If you provide keys that conflict with Pushwoosh's internal keys (like "title", "l", "b", etc.),
		 * they may override notification settings. Use custom keys to avoid conflicts.
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // E-commerce: Attach product information
		 *   Bundle productData = new Bundle();
		 *   productData.putString("screen", "product_detail");
		 *   productData.putInt("product_id", 12345);
		 *   productData.putString("category", "electronics");
		 *
		 *   LocalNotification productReminder = new LocalNotification.Builder()
		 *       .setMessage("Your saved item is on sale!")
		 *       .setExtras(productData)
		 *       .setDelay(3600)
		 *       .build();
		 *
		 *   // Retrieve in your Activity:
		 *   // PushMessage pushMessage = Pushwoosh.getInstance().getLaunchNotification();
		 *   // String screen = pushMessage.getCustomData().getString("screen");
		 *   // int productId = pushMessage.getCustomData().getInt("product_id");
		 *
		 *   // Fitness app: Attach workout details
		 *   Bundle workoutData = new Bundle();
		 *   workoutData.putString("workout_type", "cardio");
		 *   workoutData.putInt("duration_minutes", 30);
		 *   workoutData.putString("difficulty", "intermediate");
		 *
		 *   LocalNotification workoutReminder = new LocalNotification.Builder()
		 *       .setMessage("Time for your 30-minute cardio session!")
		 *       .setExtras(workoutData)
		 *       .setDelay(7200)
		 *       .build();
		 *
		 *   // News app: Attach article metadata
		 *   Bundle articleData = new Bundle();
		 *   articleData.putString("article_id", "news_2024_001");
		 *   articleData.putString("category", "technology");
		 *   articleData.putLong("published_timestamp", System.currentTimeMillis());
		 *
		 *   LocalNotification newsAlert = new LocalNotification.Builder()
		 *       .setMessage("Breaking: Major tech announcement")
		 *       .setExtras(articleData)
		 *       .setDelay(1800)
		 *       .build();
		 * }
		 * </pre>
		 * <p>
		 * <b>Important Notes:</b>
		 * <ul>
		 * <li>Custom data is accessible via {@link com.pushwoosh.Pushwoosh#getLaunchNotification()}</li>
		 * <li>Avoid using Pushwoosh internal keys: "title", "l", "b", "ci", "i", "pw_msg_tag", etc.</li>
		 * <li>Keep data size reasonable - large bundles may impact performance</li>
		 * <li>Use primitive types and Strings for best compatibility</li>
		 * </ul>
		 *
		 * @param extras Bundle containing custom key-value data
		 * @return this builder instance for method chaining
		 * @see com.pushwoosh.Pushwoosh#getLaunchNotification()
		 * @see PushMessage#getCustomData()
		 */
		public Builder setExtras(Bundle extras) {
			if (extras != null) {
				mLocalNotification.getExtras().putAll(extras);
			}
			return this;
		}

		/**
		 * Builds and returns the configured {@link LocalNotification} instance.
		 * <p>
		 * After calling this method, the LocalNotification is ready to be scheduled using
		 * {@link com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)}.
		 * <br><br>
		 * Example:
		 * <pre>
		 * {@code
		 *   // Build a complete notification
		 *   LocalNotification notification = new LocalNotification.Builder()
		 *       .setMessage("Don't forget to check your cart!")
		 *       .setTag("cart_reminder")
		 *       .setDelay(3600)
		 *       .setLink("myapp://cart")
		 *       .setBanner("https://cdn.example.com/cart-banner.jpg")
		 *       .setSmallIcon("ic_cart")
		 *       .build();
		 *
		 *   // Schedule the notification
		 *   LocalNotificationRequest request = Pushwoosh.getInstance()
		 *       .scheduleLocalNotification(notification);
		 *
		 *   // Save request ID for later cancellation
		 *   SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
		 *   prefs.edit().putInt("cart_reminder_id", request.getRequestId()).apply();
		 * }
		 * </pre>
		 *
		 * @return configured LocalNotification instance ready to be scheduled
		 * @see com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)
		 * @see LocalNotificationRequest
		 */
		public LocalNotification build() {
			return mLocalNotification;
		}
	}
}
