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
 * Abstract class that is used to customize push notification appearance.
 * All NotificationFactory ancestors must be public and must contain public constructor without parameters.
 * Application will crash on startup if this requirement is not met.
 * Custom NotificationFactory should be registered in AndroidManifest.xml metadata as follows:
 * <p>
 * <pre>
 *     {@code
 *         <meta-data
 *             android:name="com.pushwoosh.notification_factory"
 *             android:value="com.your.package.YourNotificationFactory" />
 *     }
 * </pre>
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
	 * Generates notification using PushMessage data.
	 *
	 * @param data notification data
	 * @return Notification to show
	 */
	@WorkerThread
	@Nullable
	public abstract Notification onGenerateNotification(@NonNull PushMessage data);

	/**
	 * @param data notification data
	 * @return Intent to start when user clicks on notification
	 */
	@NonNull
	public Intent getNotificationIntent(@NonNull PushMessage data) {
		Intent intent = new Intent(applicationContext, NotificationOpenActivity.class);
		intent.putExtra(NotificationIntentHelper.EXTRA_NOTIFICATION_BUNDLE, data.toBundle());
		intent.setAction(Long.toString(System.currentTimeMillis()));

		return intent;
	}

	/**
	 * @param channelName name of the channel specified in Android payload as "pw_channel" attribute.
	 * If no attribute was specified, parameter gives default channel name
	 * @return name that you want to assign to the channel on its creation. Note that empty name
	 * will be ignored and default channel name will be assigned to the channel instead
	 */
	public String channelName(String channelName) {
		return channelName;
	}

	/**
	 * @param channelName name of the channel specified in Android payload as "pw_channel" attribute.
	 * If no attribute was specified, parameter gives default channel name
	 * @return description that you want to assign to the channel on its creation
	 */
	@Nullable
	@SuppressWarnings("WeakerAccess")
	public String channelDescription(String channelName) {
		return "";
	}

	/**
	 * Makes notification cancellable
	 *
	 * @param notification push notification
	 */
	protected final void addCancel(@NonNull Notification notification) {
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
	}

	/**
	 * Adds led blinking to notification
	 *
	 * @param notification push notification
	 * @param color        led color
	 * @param ledOnMs      led on duration in ms
	 * @param ledOffMs     led off duration in ms
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
	 * Adds vibration to notification.
	 *
	 * @param notification push notification
	 * @param vibration    vibration setting
	 */
	protected final void addVibration(@NonNull Notification notification, boolean vibration) {
		NotificationPrefs notificationPrefs = RepositoryModule.getNotificationPreferences();
		VibrateType vibrateType = notificationPrefs.vibrateType().get();

		notificationChannelManager.addVibration(notification, vibrateType, vibration);
	}

	/**
	 * Adds sound to notification.
	 *
	 * @param notification push notification
	 * @param sound        resource from res/raw or assets/www/res directory.
	 *                     If parameter is null or does not exists default system sound will be played.
	 *                     If parameter is empty no sound will be played
	 */
	protected final void addSound(@NonNull Notification notification, @Nullable String sound) {
		Uri customSound = NotificationUtils.getSoundUri(sound);
		if (customSound != null) {
			notificationChannelManager.addSound(notification, customSound, sound == null);
		}
	}

	/**
	 * @return Application context.
	 */
	@Nullable
	protected final Context getApplicationContext() {
		return applicationContext;
	}

	/**
	 * Converts string with html formatting to CharSequence.
	 *
	 * @param content push notification message
	 * @return html formatted notification content
	 */
	protected final CharSequence getContentFromHtml(String content) {
		return TextUtils.isEmpty(content) ? content : Html.fromHtml(content);
	}

	/**
	 * Create, if not exist, new notification channel from pushMessage.
	 *
	 * @param pushMessage - if push message doesn't contain "pw_channel" attribute, default channel will be created
	 * @return channel id which connected with channel name. For Api less than 26 it doesn't create anything
	 */
	@SuppressWarnings("WeakerAccess")
	protected String addChannel(PushMessage pushMessage) {
		String pushChannelName = NotificationChannelInfoProvider.getChannelName(pushMessage);
		return notificationChannelManager.addChannel(pushMessage, channelName(pushChannelName), channelDescription(pushChannelName));
	}
}
