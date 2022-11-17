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
 * Default Pushwoosh implementation of NotificationFactory
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
	 * @param pushData push notification data
	 * @return Big picture bitmap image for given notification
	 */
	@SuppressWarnings("WeakerAccess")
	protected Bitmap getBigPicture(final PushMessage pushData) {
		return NotificationUtils.tryToGetBitmapFromInternet(pushData.getBigPictureUrl(), -1);
	}

	/**
	 * @param pushData push notification data
	 * @return Large icon bitmap image for given notification
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
