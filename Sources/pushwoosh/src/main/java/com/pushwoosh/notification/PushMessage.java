package com.pushwoosh.notification;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import androidx.annotation.NonNull;

import org.json.JSONObject;


import static com.pushwoosh.notification.PushBundleDataProvider.getLedColor;

import com.pushwoosh.internal.utils.JsonUtils;

/**
 * Push message data class.
 */
public class PushMessage {
	private final Bundle extras;
	private final String header;
	private final String message;
	private final String pushHash;
	private final String metaData;
	private final boolean silent;
	private final boolean local;
	private final Integer iconBackgroundColor;
	private final Integer led;
	private final String sound;
	private final boolean vibration;
	private final String ticker;
	private final String largeIconUrl;
	private final String bigPictureUrl;
	private final int smallIcon;
	private final int priority;
	private final int badges;
	private final boolean badgesAdditive;
	private final int visibility;
	private final int ledOnMS;
	private final int ledOffMS;
	private final List<Action> actions = new ArrayList<>();
	private final String msgTag;
	private final boolean lockScreen;
	private final String customData;

	public PushMessage(@NonNull Bundle extras) {
		this.extras = extras;

		pushHash = PushBundleDataProvider.getPushHash(extras);
		metaData = PushBundleDataProvider.getPushMetadata(extras);
		silent = PushBundleDataProvider.isSilent(extras);
		local = PushBundleDataProvider.isLocal(extras);
		iconBackgroundColor = PushBundleDataProvider.getIconBackgroundColor(extras);

		led = getLedColor(extras);
		sound = PushBundleDataProvider.getSound(extras);
		vibration = PushBundleDataProvider.getVibration(extras);
		message = PushBundleDataProvider.getMessage(extras);
		header = PushBundleDataProvider.getHeader(extras);
		ticker = message;
		priority = PushBundleDataProvider.getPriority(extras);
		visibility = PushBundleDataProvider.getVisibility(extras);
		badges = PushBundleDataProvider.getBadges(extras);
		badgesAdditive = PushBundleDataProvider.isBadgesAdditive(extras);
		customData = PushBundleDataProvider.getCustomData(extras);

		bigPictureUrl = PushBundleDataProvider.getBigPicture(extras);
		largeIconUrl = PushBundleDataProvider.getLargeIcon(extras);
		smallIcon = PushBundleDataProvider.getSmallIcon(extras);

		ledOnMS = PushBundleDataProvider.getLedOnMs(extras);
		ledOffMS = PushBundleDataProvider.getLedOffMs(extras);
		msgTag = PushBundleDataProvider.getMessageTag(extras);

		lockScreen = PushBundleDataProvider.isLockScreen(extras);

		actions.addAll(PushBundleDataProvider.getActions(extras));
	}

	/**
	 * @return Notification large icon url.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setLargeIcon(android.graphics.Bitmap)">Notification.Builder.setLargeIcon</a>
	 */
	public String getLargeIconUrl() {
		return largeIconUrl;
	}

	/**
	 * @return Notification big picture url.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.BigPictureStyle.html#bigPicture(android.graphics.Bitmap)">Notification.BigPictureStyle.bigPicture</a>
	 */
	public String getBigPictureUrl() {
		return bigPictureUrl;
	}

	/**
	 * @return Notification title.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setContentTitle(java.lang.CharSequence)">Notification.Builder.setContentTitle</a>
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * @return Notification message.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setContentText(java.lang.CharSequence)">Notification.Builder.setContentText</a>
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @return Pushmessage hash. Pushes triggered using remote API may not have hash.
	 */
	public String getPushHash() {
		return pushHash;
	}

	/**
	 * @return Pushmessage metadata.
	 */
	public String getPushMetaData() { return metaData; }

	/**
	 *
	 * @return Pushwoosh Notification ID
	 */


	public long getPushwooshNotificationId() {
		if (metaData != null) {
			Bundle metaDataBundle = JsonUtils.jsonStringToBundle(metaData, true);
			return metaDataBundle.getLong("uid", -1);
		} else {
			return -1;
		}
	}

	/**
	 * @return true if push message is "silent" and will not present notification.
	 */
	public boolean isSilent() {
		return silent;
	}

	/**
	 * @return true if push notification is local.
	 */
	public boolean isLocal() {
		return local;
	}

	/**
	 * @return notification icon background color.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setColor(int)">Notification.Builder.setColor</a>
	 */
	public Integer getIconBackgroundColor() {
		return iconBackgroundColor;
	}

	/**
	 * @return Led color for current push message.
	 */
	public Integer getLed() {
		return led;
	}

	/**
	 * @return sound uri for current push message.
	 */
	public String getSound() {
		return sound;
	}

	/**
	 * @return true if device should vibrate in response to notification.
	 */
	public boolean getVibration() {
		return vibration;
	}

	/**
	 * @return Ticker.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setTicker(java.lang.CharSequence)">Notification.Builder.setTicker</a>
	 */
	public String getTicker() {
		return ticker;
	}

	/**
	 * @return Notification small icon.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setSmallIcon(int)">Notification.Builder.setSmallIcon</a>
	 */
	public int getSmallIcon() {
		return smallIcon;
	}

	/**
	 * @return Notification priority.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setPriority(int)">Notification.Builder.setPriority</a>
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * @return Application icon badge number.
	 */
	public int getBadges() {
		return badges;
	}

	/**
	 * @return True if there is a sign '+' or '-' at the beginning of the badge number.
	 */
	public boolean isBadgesAdditive() {
		return badgesAdditive;
	}


	/**
	 * @return Notification visibility.
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setVisibility(int)">Notification.Builder.setVisibility</a>
	 */
	public int getVisibility() {
		return visibility;
	}

	/**
	 * @return LED on duration in ms
	 */
	public int getLedOnMS() {
		return ledOnMS;
	}

	/**
	 * @return LED off duration in ms
	 */
	public int getLedOffMS() {
		return ledOffMS;
	}

	/**
	 * @return Notification actions
	 */
	public List<Action> getActions() {
		return this.actions;
	}

	/**
	 * @return Notification tag. Notifications with different tags will not replace each other.
	 * Notifications with same tag will replace each other if multinotification mode is on {@link com.pushwoosh.notification.PushwooshNotificationSettings#setMultiNotificationMode(boolean)}
	 */
	public String getTag() {
		return msgTag;
	}

	/**
	 * @return true if notification presents Rich Media on lock screen.
	 */
	public boolean isLockScreen() {
		return lockScreen;
	}

	/**
	 * @return custom push data attached to incoming push message
	 */
	public String getCustomData() {
		return customData;
	}

	/**
	 * @return Bundle representation of push payload
	 */
	public Bundle toBundle() {
		return extras;
	}

	/**
	 * @return JSON representation of push payload
	 */
	public JSONObject toJson() {
		return PushBundleDataProvider.asJson(extras);
	}
}
