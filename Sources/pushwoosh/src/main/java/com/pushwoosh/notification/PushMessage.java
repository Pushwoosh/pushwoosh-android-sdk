package com.pushwoosh.notification;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import androidx.annotation.NonNull;

import org.json.JSONObject;


import static com.pushwoosh.notification.PushBundleDataProvider.getLedColor;

import com.pushwoosh.internal.utils.HashDecoder;
import com.pushwoosh.internal.utils.JsonUtils;

/**
 * Represents a received push notification with all its payload data and metadata.
 * <p>
 * PushMessage is an immutable data class that encapsulates all information contained in a push notification,
 * including the notification content (title, message), visual customization (icons, colors, LED), behavior
 * settings (sound, vibration, priority), and custom data payloads. It serves as the primary interface for
 * accessing push notification data in callbacks, service extensions, and message handlers.
 * <p>
 * <b>When you receive PushMessage instances:</b>
 * <ul>
 * <li>In {@link NotificationServiceExtension#onMessageReceived(PushMessage)} when push arrives</li>
 * <li>In {@link NotificationServiceExtension#onMessageOpened(PushMessage)} when user taps notification</li>
 * <li>Via {@link com.pushwoosh.Pushwoosh#getLaunchNotification()} to get the notification that launched the app</li>
 * <li>In broadcast receivers handling Pushwoosh notification events</li>
 * </ul>
 * <p>
 * <b>Common use cases:</b>
 * <ul>
 * <li>Extracting custom data from push notifications to route users to specific screens</li>
 * <li>Customizing notification appearance based on payload before display</li>
 * <li>Logging analytics events based on push content or campaign data</li>
 * <li>Handling silent pushes to sync data without showing notifications</li>
 * <li>Implementing custom business logic based on push metadata</li>
 * </ul>
 * <br>
 * <b>Basic usage example - Handling custom data:</b>
 * <pre>
 * {@code
 * public class MyNotificationExtension extends NotificationServiceExtension {
 *     @Override
 *     protected boolean onMessageReceived(PushMessage message) {
 *         // Extract custom data from push payload
 *         String customData = message.getCustomData();
 *         if (customData != null) {
 *             try {
 *                 JSONObject data = new JSONObject(customData);
 *                 String screen = data.optString("target_screen");
 *                 String itemId = data.optString("item_id");
 *
 *                 // Route to specific screen based on custom data
 *                 if ("product_detail".equals(screen)) {
 *                     navigateToProduct(itemId);
 *                 } else if ("cart".equals(screen)) {
 *                     navigateToCart();
 *                 }
 *             } catch (JSONException e) {
 *                 Log.e("App", "Failed to parse custom data", e);
 *             }
 *         }
 *
 *         // Return false to show default notification
 *         return false;
 *     }
 * }
 * }</pre>
 * <br>
 * <b>Silent push example - Background data sync:</b>
 * <pre>
 * {@code
 * @Override
 * protected boolean onMessageReceived(PushMessage message) {
 *     // Check if this is a silent push
 *     if (message.isSilent()) {
 *         // Silent push - no notification shown
 *         String customData = message.getCustomData();
 *         if (customData != null) {
 *             JSONObject data = new JSONObject(customData);
 *             String action = data.optString("action");
 *
 *             if ("sync_data".equals(action)) {
 *                 // Trigger background data sync
 *                 syncUserData();
 *             } else if ("clear_cache".equals(action)) {
 *                 clearAppCache();
 *             }
 *         }
 *         // Return true to indicate we handled the push
 *         return true;
 *     }
 *
 *     // Return false to show default notification
 *     return false;
 * }
 * }</pre>
 * <br>
 * <b>Launch notification example - Deep linking:</b>
 * <pre>
 * {@code
 * @Override
 * protected void onCreate(Bundle savedInstanceState) {
 *     super.onCreate(savedInstanceState);
 *     setContentView(R.layout.activity_main);
 *
 *     // Check if app was opened from a push notification
 *     PushMessage message = Pushwoosh.getInstance().getLaunchNotification();
 *     if (message != null) {
 *         String customData = message.getCustomData();
 *         if (customData != null) {
 *             try {
 *                 JSONObject data = new JSONObject(customData);
 *                 String orderId = data.optString("order_id");
 *
 *                 if (!orderId.isEmpty()) {
 *                     // Navigate to order details screen
 *                     Intent intent = new Intent(this, OrderDetailActivity.class);
 *                     intent.putExtra("order_id", orderId);
 *                     startActivity(intent);
 *                 }
 *             } catch (JSONException e) {
 *                 Log.e("App", "Failed to parse launch notification data", e);
 *             }
 *         }
 *
 *         // Clear launch notification so it's not processed again
 *         Pushwoosh.getInstance().clearLaunchNotification();
 *     }
 * }
 * }</pre>
 * <br>
 * <b>Campaign tracking example - Analytics integration:</b>
 * <pre>
 * {@code
 * @Override
 * protected void onMessageOpened(PushMessage message) {
 *     super.onMessageOpened(message);
 *
 *     // Track push notification opens in your analytics
 *     long campaignId = message.getCampaignId();
 *     long messageId = message.getMessageId();
 *     String messageCode = message.getMessageCode();
 *
 *     // Send event to analytics service
 *     Analytics.logEvent("push_opened", new HashMap<String, Object>() {{
 *         put("campaign_id", campaignId);
 *         put("message_id", messageId);
 *         put("message_code", messageCode);
 *         put("title", message.getHeader());
 *     }});
 *
 *     // Log custom data for segmentation
 *     String customData = message.getCustomData();
 *     if (customData != null) {
 *         try {
 *             JSONObject data = new JSONObject(customData);
 *             String category = data.optString("category", "general");
 *             Analytics.setUserProperty("last_notification_category", category);
 *         } catch (JSONException e) {
 *             Log.e("App", "Failed to parse custom data", e);
 *         }
 *     }
 * }
 * }</pre>
 *
 * @see NotificationServiceExtension
 * @see com.pushwoosh.Pushwoosh#getLaunchNotification()
 * @see LocalNotification
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
	private final String groupId;

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
		groupId = PushBundleDataProvider.getGroupId(extras);

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
	 * Returns the custom JSON data payload attached to the push notification.
	 * <p>
	 * Custom data allows you to send additional information beyond the standard notification content.
	 * This is typically used for deep linking, routing users to specific screens, passing IDs, or
	 * triggering custom business logic. The data is sent as a JSON string and must be parsed by your app.
	 * <p>
	 * <b>Setting custom data in Pushwoosh dashboard:</b><br>
	 * When creating a push notification in the Pushwoosh console, add custom data in the "Additional" tab
	 * under "Custom Data" field as JSON. Example:
	 * <pre>
	 * {"screen": "product", "product_id": "12345", "category": "electronics"}
	 * </pre>
	 * <p>
	 * <b>Example - E-commerce deep linking:</b>
	 * <pre>
	 * {@code
	 * String customData = message.getCustomData();
	 * if (customData != null) {
	 *     try {
	 *         JSONObject data = new JSONObject(customData);
	 *         String screen = data.optString("screen");
	 *         String productId = data.optString("product_id");
	 *
	 *         if ("product".equals(screen) && !productId.isEmpty()) {
	 *             // Navigate to product details
	 *             Intent intent = new Intent(context, ProductDetailActivity.class);
	 *             intent.putExtra("product_id", productId);
	 *             context.startActivity(intent);
	 *         }
	 *     } catch (JSONException e) {
	 *         Log.e("App", "Failed to parse custom data", e);
	 *     }
	 * }
	 * }</pre>
	 * <br>
	 * <b>Example - News app article routing:</b>
	 * <pre>
	 * {@code
	 * String customData = message.getCustomData();
	 * if (customData != null) {
	 *     JSONObject data = new JSONObject(customData);
	 *     String articleId = data.optString("article_id");
	 *     String category = data.optString("category", "general");
	 *
	 *     // Open specific article
	 *     Intent intent = new Intent(context, ArticleActivity.class);
	 *     intent.putExtra("article_id", articleId);
	 *     intent.putExtra("category", category);
	 *     context.startActivity(intent);
	 * }
	 * }</pre>
	 *
	 * @return JSON string containing custom data, or {@code null} if no custom data was included in the push
	 */
	public String getCustomData() {
		return customData;
	}

	/**
	 * @return notification group id
	 */
	public String getGroupId() {
		return groupId;
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

	public String getMessageCode() {
		return HashDecoder.parseMessageHash(pushHash)[1];
	}

	public long getCampaignId() {
		String[] parsedMessageHash = HashDecoder.parseMessageHash(pushHash);
		if (parsedMessageHash[0] != null) {
			return Long.parseLong(parsedMessageHash[2]);
		} else return 0;
	}

	public long getMessageId() {
		String[] parsedMessageHash = HashDecoder.parseMessageHash(pushHash);
		if (parsedMessageHash[0] != null) {
			return Long.parseLong(parsedMessageHash[0]);
		} else return 0;
	}
}
