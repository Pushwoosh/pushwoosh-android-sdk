package com.pushwoosh.notification;

import android.os.Bundle;

/**
 * The LocalNotification class combines data that is used to schedule local notification {@link com.pushwoosh.Pushwoosh#scheduleLocalNotification(LocalNotification)}
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
	 * LocalNotification Builder.
	 */
	public static class Builder {
		private LocalNotification mLocalNotification;

		public Builder() {
			mLocalNotification = new LocalNotification();
		}

		/**
		 * Sets notification tag that is used to distinguish different notifications. Notifications with different tags will not replace each other.
		 * Notifications with same tag will replace each other if multi notification mode is not set {@link PushwooshNotificationSettings#setMultiNotificationMode(boolean)}
		 *
		 * @param tag notification tag
		 * @return builder
		 */
		public Builder setTag(String tag) {
			PushBundleDataProvider.setTag(mLocalNotification.getExtras(), tag);
			return this;
		}

		/**
		 * Sets notification content.
		 *
		 * @param message notififcation text message
		 * @return builder
		 */
		public Builder setMessage(String message) {
			PushBundleDataProvider.setMessage(mLocalNotification.getExtras(), message);
			return this;
		}

		/**
		 * Sets the delay after which notification will be displayed.
		 *
		 * @param delay delay in seconds
		 * @return builder
		 */
		public Builder setDelay(int delay) {
			mLocalNotification.setDelay(delay);
			return this;
		}

		/**
		 * Sets url link that will be open in browser instead of default launcher activity after clicking on notification.
		 * Deeplink url can be also used as parameter.
		 *
		 * @param url url link
		 * @return builder
		 */
		public Builder setLink(String url) {
			PushBundleDataProvider.setLink(mLocalNotification.getExtras(), url);
			return this;
		}

		/**
		 * Sets image for notification <a href="https://developer.android.com/reference/android/app/Notification.BigPictureStyle.html">BigPictureStyle</a>
		 *
		 * @param url image url link
		 * @return builder
		 */
		public Builder setBanner(String url) {
			PushBundleDataProvider.setBanner(mLocalNotification.getExtras(), url);
			return this;
		}

		/**
		 * Sets small icon image.
		 *
		 * @param name resource name for small icon.
		 * @return builder
		 */
		public Builder setSmallIcon(String name) {
			PushBundleDataProvider.setSmallIcon(mLocalNotification.getExtras(), name);
			return this;
		}

		/**
		 * Sets large icon image.
		 *
		 * @param url image url link.
		 * @return builder
		 */
		public Builder setLargeIcon(String url) {
			PushBundleDataProvider.setLargeIcon(mLocalNotification.getExtras(), url);
			return this;
		}

		/**
		 * Sets custom notification bundle. Warning: this can replace other settings.
		 *
		 * @param extras notification bundle extras
		 * @return builder
		 */
		public Builder setExtras(Bundle extras) {
			if (extras != null) {
				mLocalNotification.getExtras().putAll(extras);
			}
			return this;
		}

		/**
		 * Builds and returns LocalNotification.
		 *
		 * @return local notification
		 */
		public LocalNotification build() {
			return mLocalNotification;
		}
	}
}
