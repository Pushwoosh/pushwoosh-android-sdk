package com.pushwoosh.notification;

import android.app.Notification;
import android.graphics.Bitmap;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.NotificationUtils;
import com.pushwoosh.notification.builder.NotificationBuilder;
import com.pushwoosh.notification.builder.NotificationBuilderManager;

/**
 * Default implementation of {@link NotificationFactory} provided by the Pushwoosh SDK.
 * <p>
 * This class serves as the reference implementation for creating push notifications with standard
 * Pushwoosh features. It demonstrates best practices for building notifications and handling all
 * notification data from the push payload, including rich media, actions, channels, and notification
 * customization options.
 * <p>
 * Use this class as a starting point when creating your own custom notification factory. You can
 * extend it and override specific methods to customize particular aspects while keeping the rest
 * of the default behavior.
 * <p>
 * <b>Features Implemented:</b>
 * <ul>
 * <li>Large icon loading from URLs</li>
 * <li>Big picture style notifications with image loading</li>
 * <li>Notification channels (Android 8.0+)</li>
 * <li>Custom colors, priorities, and visibility settings</li>
 * <li>Notification actions from push payload</li>
 * <li>Sound, vibration, and LED from push payload</li>
 * <li>HTML formatted text support</li>
 * <li>Group notifications support (Android 7.0+)</li>
 * </ul>
 * <p>
 * <b>Quick Start - Use as reference:</b>
 * <pre>
 * {@code
 *   // This is the default factory - no registration needed
 *   // Pushwoosh uses this automatically if no custom factory is specified
 *
 *   // To see how it works, look at onGenerateNotification() method
 * }
 * </pre>
 * <p>
 * <b>Example - Extend for customization:</b>
 * <pre>
 * {@code
 *   public class MyNotificationFactory extends PushwooshNotificationFactory {
 *       @Override
 *       public Notification onGenerateNotification(@NonNull PushMessage data) {
 *           // Call parent to get default notification
 *           Notification notification = super.onGenerateNotification(data);
 *
 *           if (notification != null) {
 *               // Add custom modifications
 *               notification.flags |= Notification.FLAG_INSISTENT; // Keep alerting
 *           }
 *
 *           return notification;
 *       }
 *
 *       @Override
 *       protected Bitmap getLargeIcon(PushMessage pushData) {
 *           // Use custom image loading library
 *           String iconUrl = pushData.getLargeIconUrl();
 *           if (iconUrl != null) {
 *               return MyImageLoader.loadSync(iconUrl);
 *           }
 *           return super.getLargeIcon(pushData);
 *       }
 *   }
 *
 *   // Register in AndroidManifest.xml:
 *   <meta-data
 *       android:name="com.pushwoosh.notification_factory"
 *       android:value=".MyNotificationFactory" />
 * }
 * </pre>
 * <p>
 * <b>Example - Override for complete custom behavior:</b>
 * <pre>
 * {@code
 *   public class MyNotificationFactory extends PushwooshNotificationFactory {
 *       @Override
 *       public Notification onGenerateNotification(@NonNull PushMessage data) {
 *           // Don't call super - build completely custom notification
 *           String channelId = addChannel(data);
 *
 *           NotificationCompat.Builder builder = new NotificationCompat.Builder(
 *               getApplicationContext(), channelId)
 *               .setContentTitle(data.getHeader())
 *               .setContentText(data.getMessage())
 *               .setSmallIcon(R.drawable.custom_icon)
 *               .setColor(0xFF6200EE);
 *
 *           Notification notification = builder.build();
 *           addCancel(notification);
 *
 *           return notification;
 *       }
 *   }
 * }
 * </pre>
 *
 * @see NotificationFactory
 * @see #onGenerateNotification(PushMessage)
 * @see #getBigPicture(PushMessage)
 * @see #getLargeIcon(PushMessage)
 */
public class PushwooshNotificationFactory extends NotificationFactory {

	@Override
	@WorkerThread
	@Nullable
	public Notification onGenerateNotification(@NonNull PushMessage pushData) {

		Bitmap largeIcon = getLargeIcon(pushData);
		Bitmap bigPicture = getBigPicture(pushData);

		final String channelId = addChannel(pushData);
		if (getApplicationContext() == null) {
			return null;
		}

		NotificationBuilder notificationBuilder = NotificationBuilderManager.createNotificationBuilder(getApplicationContext(), channelId);
		notificationBuilder.setContentTitle(getContentFromHtml(pushData.getHeader()))
				.setContentText(getContentFromHtml(pushData.getMessage()))

				.setSmallIcon(pushData.getSmallIcon())
				.setStyle(bigPicture, getContentFromHtml(pushData.getMessage()))
				.setLargeIcon(largeIcon)

				.setColor(pushData.getIconBackgroundColor())

				.setPriority(pushData.getPriority())
				.setVisibility(pushData.getVisibility())

				.setTicker(getContentFromHtml(pushData.getTicker()))
				.setGroup(pushData.getGroupId())
				.setWhen(System.currentTimeMillis());

		for (Action action : pushData.getActions()) {
			NotificationBuilderManager.addAction(getApplicationContext(), notificationBuilder, action);
		}

		// to support summary notifications
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			notificationBuilder.setExtras(pushData.toBundle());
		}

		final Notification notification = notificationBuilder.build();

		addLED(notification, pushData.getLed(), pushData.getLedOnMS(), pushData.getLedOffMS());
		addSound(notification, pushData.getSound());
		addVibration(notification, pushData.getVibration());
		addCancel(notification);

		return notification;
	}

	/**
	 * Loads and returns the big picture image for expanded notification style.
	 * <p>
	 * This method downloads the image from the URL specified in the push payload's "pw_big_picture"
	 * attribute. The image is displayed when the user expands the notification. This method runs on
	 * a worker thread, so network operations are safe.
	 * <p>
	 * <b>Image Requirements:</b>
	 * <ul>
	 * <li>Recommended size: 2:1 aspect ratio (e.g., 1024x512)</li>
	 * <li>Supported formats: JPEG, PNG</li>
	 * <li>The image should be optimized for mobile</li>
	 * </ul>
	 * <p>
	 * Override this method to customize image loading, such as using a different image library
	 * (Glide, Picasso), applying transformations, or loading from local storage.
	 * <br><br>
	 * Example - Custom image loading with caching:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected Bitmap getBigPicture(PushMessage pushData) {
	 *       String imageUrl = pushData.getBigPictureUrl();
	 *       if (imageUrl == null) {
	 *           return null;
	 *       }
	 *
	 *       try {
	 *           // Use Glide with caching
	 *           return Glide.with(getApplicationContext())
	 *               .asBitmap()
	 *               .load(imageUrl)
	 *               .submit(1024, 512)
	 *               .get();
	 *       } catch (Exception e) {
	 *           Log.e("NotificationFactory", "Failed to load big picture", e);
	 *           return null;
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param pushData Push notification data containing the big picture URL
	 * @return Bitmap to display in expanded notification, or null if URL is not specified or loading fails
	 *
	 * @see PushMessage#getBigPictureUrl()
	 * @see #getLargeIcon(PushMessage)
	 */
	@SuppressWarnings("WeakerAccess")
	protected Bitmap getBigPicture(final PushMessage pushData) {
		return NotificationUtils.tryToGetBitmapFromInternet(pushData.getBigPictureUrl(), -1);
	}

	/**
	 * Loads and returns the large icon image for the notification.
	 * <p>
	 * The large icon appears on the right side of the notification (or left on some devices) and
	 * provides a visual identifier for your app or the notification content. This method downloads
	 * the image from the URL specified in the push payload's "pw_large_icon" attribute.
	 * <p>
	 * The image is automatically sized to match the system's notification_large_icon_height
	 * dimension (typically 64dp x 64dp). This method runs on a worker thread, so network
	 * operations are safe.
	 * <p>
	 * <b>Image Requirements:</b>
	 * <ul>
	 * <li>Recommended size: 256x256 (will be scaled down)</li>
	 * <li>Should be square or circular</li>
	 * <li>Supported formats: JPEG, PNG</li>
	 * <li>Transparent backgrounds work well</li>
	 * </ul>
	 * <p>
	 * Override this method to customize image loading or apply circular cropping, rounded corners,
	 * or other transformations.
	 * <br><br>
	 * Example - Custom image with circular crop:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected Bitmap getLargeIcon(PushMessage pushData) {
	 *       String iconUrl = pushData.getLargeIconUrl();
	 *       if (iconUrl == null) {
	 *           // Fall back to app icon
	 *           return BitmapFactory.decodeResource(
	 *               getApplicationContext().getResources(),
	 *               R.drawable.ic_launcher
	 *           );
	 *       }
	 *
	 *       try {
	 *           // Load and apply circular transformation
	 *           Bitmap bitmap = Glide.with(getApplicationContext())
	 *               .asBitmap()
	 *               .load(iconUrl)
	 *               .transform(new CircleCrop())
	 *               .submit(256, 256)
	 *               .get();
	 *           return bitmap;
	 *       } catch (Exception e) {
	 *           Log.e("NotificationFactory", "Failed to load large icon", e);
	 *           return null;
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param pushData Push notification data containing the large icon URL
	 * @return Bitmap to display as notification large icon, or null if URL is not specified or loading fails
	 *
	 * @see PushMessage#getLargeIconUrl()
	 * @see #getBigPicture(PushMessage)
	 */
	@SuppressWarnings("WeakerAccess")
	protected Bitmap getLargeIcon(final PushMessage pushData) {
		final int dimension = (int) AndroidPlatformModule.getResourceProvider().getDimension(android.R.dimen.notification_large_icon_height);
		String largeIconUrl = pushData.getLargeIconUrl();
		if (largeIconUrl != null) {
			return NotificationUtils.tryGetBitmap(largeIconUrl, dimension);
		}
		return null;
	}
}
