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
 * Represents a push notification message received from Pushwoosh.
 * <p>
 * PushMessage is a data container that provides access to all information about a received push notification,
 * including content (title, message, images), appearance settings (icons, colors, sounds), behavior flags
 * (silent, local), and metadata. This class is used throughout the SDK to pass push notification data
 * between components.
 * <p>
 * <b>Key Features:</b>
 * <ul>
 * <li>Content Access - Retrieve notification title, message, and images</li>
 * <li>Appearance Settings - Access icon URLs, colors, LED, sound, and vibration settings</li>
 * <li>Metadata - Get campaign ID, message ID, push hash, and custom data</li>
 * <li>Behavior Flags - Check if notification is silent or local</li>
 * <li>Notification Actions - Access action buttons configured in the push</li>
 * <li>Data Conversion - Convert to Bundle or JSON for processing</li>
 * </ul>
 * <p>
 * <b>Common Use Cases:</b>
 * <pre>
 * {@code
 *   // 1. Handling push notification in NotificationServiceExtension
 *   public class MyNotificationExtension extends NotificationServiceExtension {
 *       @Override
 *       protected boolean onMessageReceived(PushMessage message) {
 *           // Access notification content
 *           String title = message.getHeader();
 *           String text = message.getMessage();
 *           String customData = message.getCustomData();
 *
 *           // Parse custom data for e-commerce app
 *           if (customData != null) {
 *               try {
 *                   JSONObject data = new JSONObject(customData);
 *                   String productId = data.optString("product_id");
 *                   double discount = data.optDouble("discount", 0);
 *
 *                   // Update local database with product discount
 *                   updateProductDiscount(productId, discount);
 *
 *                   Log.d("App", "Push for product: " + productId);
 *               } catch (JSONException e) {
 *                   Log.e("App", "Failed to parse custom data", e);
 *               }
 *           }
 *
 *           // Don't show notification if app is in foreground
 *           if (isAppOnForeground()) {
 *               showInAppAlert(title, text);
 *               return true; // Suppress notification
 *           }
 *
 *           return false; // Show notification
 *       }
 *
 *       @Override
 *       protected void onMessageOpened(PushMessage message) {
 *           // Track campaign analytics
 *           long campaignId = message.getCampaignId();
 *           long messageId = message.getMessageId();
 *
 *           // Navigate based on custom data
 *           String customData = message.getCustomData();
 *           if (customData != null) {
 *               try {
 *                   JSONObject data = new JSONObject(customData);
 *                   String screen = data.optString("screen");
 *
 *                   if ("product_details".equals(screen)) {
 *                       String productId = data.optString("product_id");
 *                       openProductScreen(productId);
 *                   }
 *               } catch (JSONException e) {
 *                   Log.e("App", "Failed to parse custom data", e);
 *               }
 *           }
 *       }
 *   }
 *
 *   // 2. Checking notification appearance settings
 *   protected void logNotificationSettings(PushMessage message) {
 *       // Check if notification is silent (data-only)
 *       if (message.isSilent()) {
 *           Log.d("App", "Silent push received");
 *           processDataOnly(message.getCustomData());
 *           return;
 *       }
 *
 *       // Check if notification is local
 *       if (message.isLocal()) {
 *           Log.d("App", "Local notification triggered");
 *       }
 *
 *       // Get rich media URLs
 *       String largeIcon = message.getLargeIconUrl();
 *       String bigPicture = message.getBigPictureUrl();
 *
 *       if (bigPicture != null) {
 *           Log.d("App", "Notification has big picture: " + bigPicture);
 *       }
 *   }
 *
 *   // 3. Working with notification actions
 *   protected void handleNotificationActions(PushMessage message) {
 *       List<Action> actions = message.getActions();
 *
 *       if (!actions.isEmpty()) {
 *           Log.d("App", "Notification has " + actions.size() + " action buttons");
 *
 *           for (Action action : actions) {
 *               Log.d("App", "Action: " + action.getTitle() +
 *                            ", Type: " + action.getType());
 *           }
 *       }
 *   }
 * }
 * </pre>
 * <p>
 * <b>Important Notes:</b>
 * <ul>
 * <li>PushMessage objects are created automatically by the SDK when processing push notifications</li>
 * <li>To receive PushMessage instances, extend {@link NotificationServiceExtension} and override callback methods</li>
 * <li>Custom data can be sent from Pushwoosh Control Panel in JSON format and accessed via {@link #getCustomData()}</li>
 * <li>Silent pushes ({@link #isSilent()} returns true) do not display notifications but can trigger background processing</li>
 * <li>Use {@link #toBundle()} or {@link #toJson()} to access the raw push payload if needed</li>
 * <li>Campaign tracking IDs ({@link #getCampaignId()}, {@link #getMessageId()}) are useful for analytics</li>
 * </ul>
 *
 * @see NotificationServiceExtension
 * @see Action
 * @see PushwooshNotificationFactory
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

	/**
	 * Creates a PushMessage from a push notification payload Bundle.
	 * <p>
	 * This constructor is used internally by the SDK to create PushMessage instances from
	 * FCM/HMS push payloads. You typically don't need to call this constructor directly -
	 * PushMessage instances are automatically created and passed to your
	 * {@link NotificationServiceExtension} callback methods.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   // Advanced use case: Creating PushMessage from saved Bundle
	 *   public void reprocessSavedNotification() {
	 *       SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
	 *       String bundleString = prefs.getString("saved_notification", null);
	 *
	 *       if (bundleString != null) {
	 *           // Reconstruct bundle and create PushMessage
	 *           Bundle bundle = bundleFromString(bundleString);
	 *           PushMessage message = new PushMessage(bundle);
	 *
	 *           // Process the saved notification
	 *           processNotification(message);
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @param extras Bundle containing the push notification payload from FCM/HMS
	 * @see NotificationServiceExtension#onMessageReceived(PushMessage)
	 */
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
	 * Gets the notification title.
	 * <p>
	 * Returns the title text configured for this push notification in the Pushwoosh Control Panel.
	 * This is displayed as the prominent heading in the notification UI. If no title was specified,
	 * returns the app name by default.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       String title = message.getHeader();
	 *       String text = message.getMessage();
	 *
	 *       // Show custom in-app notification for foreground
	 *       if (isAppOnForeground()) {
	 *           showCustomDialog(title, text);
	 *           return true; // Suppress system notification
	 *       }
	 *
	 *       return false; // Show system notification
	 *   }
	 * }
	 * </pre>
	 *
	 * @return notification title text, or null if not specified
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setContentTitle(java.lang.CharSequence)">Notification.Builder.setContentTitle</a>
	 * @see #getMessage()
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * Gets the notification message body.
	 * <p>
	 * Returns the main text content of the push notification configured in the Pushwoosh Control Panel.
	 * This is displayed as the notification body text below the title.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected void onMessageOpened(PushMessage message) {
	 *       String title = message.getHeader();
	 *       String body = message.getMessage();
	 *
	 *       // Log notification details
	 *       Log.d("App", "User opened: " + title + " - " + body);
	 *
	 *       // Track in analytics
	 *       analytics.trackEvent("notification_opened", new HashMap<String, String>() {{
	 *           put("title", title);
	 *           put("message", body);
	 *           put("campaign_id", String.valueOf(message.getCampaignId()));
	 *       }});
	 *   }
	 * }
	 * </pre>
	 *
	 * @return notification message body text, or null if not specified
	 * @see <a href="https://developer.android.com/reference/android/app/Notification.Builder.html#setContentText(java.lang.CharSequence)">Notification.Builder.setContentText</a>
	 * @see #getHeader()
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
	 * Gets the Pushwoosh internal notification ID.
	 * <p>
	 * Returns a unique identifier assigned by Pushwoosh to this notification. This ID is different
	 * from the message ID and campaign ID, and is used internally by the SDK. It can be useful
	 * for advanced tracking or when communicating with Pushwoosh support about specific notifications.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       long notificationId = message.getPushwooshNotificationId();
	 *       long messageId = message.getMessageId();
	 *       long campaignId = message.getCampaignId();
	 *
	 *       // Log all IDs for comprehensive tracking
	 *       Log.d("Analytics", String.format(
	 *           "Notification - PW ID: %d, Message ID: %d, Campaign ID: %d",
	 *           notificationId, messageId, campaignId
	 *       ));
	 *
	 *       // Store for support/debugging
	 *       if (notificationId != -1) {
	 *           saveDiagnosticInfo(notificationId, message.toJson().toString());
	 *       }
	 *
	 *       return false;
	 *   }
	 * }
	 * </pre>
	 *
	 * @return Pushwoosh notification ID, or -1 if not available
	 * @see #getMessageId()
	 * @see #getCampaignId()
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
	 * Checks if this is a silent (data-only) push notification.
	 * <p>
	 * Silent pushes do not display notifications to the user but can trigger background processing
	 * in your app. Use this to deliver data updates, sync content, or perform background tasks
	 * without interrupting the user.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       if (message.isSilent()) {
	 *           // Handle silent push - update data without notification
	 *           String customData = message.getCustomData();
	 *
	 *           try {
	 *               JSONObject data = new JSONObject(customData);
	 *               String action = data.optString("action");
	 *
	 *               if ("sync_messages".equals(action)) {
	 *                   // Sync messages in background
	 *                   syncMessagesFromServer();
	 *               } else if ("update_config".equals(action)) {
	 *                   // Update app configuration
	 *                   updateAppConfig(data);
	 *               }
	 *
	 *               Log.d("App", "Silent push processed: " + action);
	 *           } catch (JSONException e) {
	 *               Log.e("App", "Failed to parse silent push data", e);
	 *           }
	 *
	 *           return true; // Already handled, don't show notification
	 *       }
	 *
	 *       return false; // Regular push, show notification
	 *   }
	 * }
	 * </pre>
	 *
	 * @return true if this is a silent push that should not display a notification
	 * @see #getCustomData()
	 */
	public boolean isSilent() {
		return silent;
	}

	/**
	 * Checks if this is a local notification.
	 * <p>
	 * Local notifications are scheduled by the app itself using {@link LocalNotification} rather than
	 * being sent from Pushwoosh servers. This method helps distinguish between remote and local notifications.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       if (message.isLocal()) {
	 *           Log.d("App", "Local notification triggered");
	 *
	 *           // Handle local notification
	 *           String customData = message.getCustomData();
	 *           // Process reminder or scheduled task
	 *
	 *       } else {
	 *           Log.d("App", "Remote notification from Pushwoosh");
	 *
	 *           // Track delivery for remote notifications
	 *           long campaignId = message.getCampaignId();
	 *           analytics.trackNotificationReceived(campaignId);
	 *       }
	 *
	 *       return false; // Show notification
	 *   }
	 * }
	 * </pre>
	 *
	 * @return true if this is a local notification scheduled by the app
	 * @see LocalNotification
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
	 * Gets the list of action buttons for this notification.
	 * <p>
	 * Action buttons allow users to interact with notifications directly from the notification tray
	 * without opening the app. Each action can trigger an Activity, Service, or Broadcast and can
	 * include custom data. Actions are configured in the Pushwoosh Control Panel when creating a campaign.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       List<Action> actions = message.getActions();
	 *
	 *       if (!actions.isEmpty()) {
	 *           Log.d("App", "Notification has " + actions.size() + " action buttons");
	 *
	 *           // Log action details
	 *           for (Action action : actions) {
	 *               Log.d("App", String.format(
	 *                   "Action: %s, Type: %s, URL: %s",
	 *                   action.getTitle(),
	 *                   action.getType(),
	 *                   action.getUrl()
	 *               ));
	 *
	 *               // Check for specific actions
	 *               if ("View Product".equals(action.getTitle())) {
	 *                   // Prepare product URL for quick access
	 *                   String productUrl = action.getUrl();
	 *                   cacheProductData(productUrl);
	 *               }
	 *           }
	 *       }
	 *
	 *       return false;
	 *   }
	 *
	 *   // Handling action button clicks in your activity
	 *   public class ActionReceiver extends BroadcastReceiver {
	 *       @Override
	 *       public void onReceive(Context context, Intent intent) {
	 *           // Get action data from intent
	 *           String actionTitle = intent.getStringExtra("action_title");
	 *
	 *           // Handle different actions
	 *           if ("Add to Cart".equals(actionTitle)) {
	 *               String productId = intent.getStringExtra("product_id");
	 *               addToCart(productId);
	 *           } else if ("Buy Now".equals(actionTitle)) {
	 *               String productId = intent.getStringExtra("product_id");
	 *               startCheckout(productId);
	 *           }
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @return list of notification actions, or empty list if no actions configured
	 * @see Action
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
	 * Gets custom data attached to this push notification.
	 * <p>
	 * Custom data is a JSON string that can be set in the Pushwoosh Control Panel when creating
	 * a push campaign. Use this to pass additional information to your app such as product IDs,
	 * deep link parameters, user-specific data, or any other information needed to handle the notification.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected void onMessageOpened(PushMessage message) {
	 *       String customData = message.getCustomData();
	 *
	 *       if (customData != null) {
	 *           try {
	 *               JSONObject data = new JSONObject(customData);
	 *
	 *               // E-commerce example: navigate to product
	 *               String screen = data.optString("screen");
	 *               if ("product_details".equals(screen)) {
	 *                   String productId = data.optString("product_id");
	 *                   String category = data.optString("category");
	 *
	 *                   // Open product details screen
	 *                   Intent intent = new Intent(this, ProductActivity.class);
	 *                   intent.putExtra("product_id", productId);
	 *                   intent.putExtra("category", category);
	 *                   startActivity(intent);
	 *               }
	 *               // News app example: open article
	 *               else if ("article".equals(screen)) {
	 *                   String articleId = data.optString("article_id");
	 *                   openArticle(articleId);
	 *               }
	 *
	 *           } catch (JSONException e) {
	 *               Log.e("App", "Failed to parse custom data", e);
	 *           }
	 *       }
	 *   }
	 *
	 *   // Processing custom data in background (silent push)
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       if (message.isSilent()) {
	 *           String customData = message.getCustomData();
	 *
	 *           try {
	 *               JSONObject data = new JSONObject(customData);
	 *               String action = data.optString("action");
	 *
	 *               if ("update_inventory".equals(action)) {
	 *                   int productId = data.optInt("product_id");
	 *                   int stock = data.optInt("stock");
	 *
	 *                   // Update local database
	 *                   updateProductStock(productId, stock);
	 *               }
	 *
	 *           } catch (JSONException e) {
	 *               Log.e("App", "Failed to parse custom data", e);
	 *           }
	 *
	 *           return true; // Suppress notification
	 *       }
	 *       return false;
	 *   }
	 * }
	 * </pre>
	 *
	 * @return custom data JSON string, or null if no custom data was provided
	 * @see #isSilent()
	 * @see #toJson()
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
	 * Converts this push message to a Bundle.
	 * <p>
	 * Returns the raw push payload as an Android Bundle containing all the notification data
	 * received from FCM/HMS. This is useful when you need to access fields that don't have
	 * dedicated getter methods or when passing the push data to other components.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected void onMessageOpened(PushMessage message) {
	 *       // Get raw bundle for advanced processing
	 *       Bundle bundle = message.toBundle();
	 *
	 *       // Access all bundle keys
	 *       for (String key : bundle.keySet()) {
	 *           Log.d("App", "Key: " + key + " = " + bundle.get(key));
	 *       }
	 *
	 *       // Pass to analytics service
	 *       analytics.trackNotificationOpened(bundle);
	 *
	 *       // Store for later processing
	 *       SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
	 *       prefs.edit()
	 *           .putString("last_notification", bundle.toString())
	 *           .apply();
	 *   }
	 * }
	 * </pre>
	 *
	 * @return Bundle containing the complete push notification payload
	 * @see #toJson()
	 */
	public Bundle toBundle() {
		return extras;
	}

	/**
	 * Converts this push message to JSON.
	 * <p>
	 * Returns the raw push payload as a JSONObject containing all the notification data
	 * received from FCM/HMS. This is useful for logging, debugging, sending data to analytics,
	 * or when you need to access the complete push payload in JSON format.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       // Log complete push payload for debugging
	 *       JSONObject json = message.toJson();
	 *       Log.d("App", "Push received: " + json.toString());
	 *
	 *       // Send to analytics
	 *       analytics.logEvent("push_received", new Bundle() {{
	 *           putString("payload", json.toString());
	 *           putLong("campaign_id", message.getCampaignId());
	 *       }});
	 *
	 *       // Check for specific custom fields
	 *       if (json.has("priority_delivery")) {
	 *           boolean isPriority = json.optBoolean("priority_delivery");
	 *           if (isPriority) {
	 *               // Handle high-priority notification
	 *               showImmediateNotification(message);
	 *               return true;
	 *           }
	 *       }
	 *
	 *       return false;
	 *   }
	 *
	 *   // Storing notification history
	 *   protected void saveNotificationHistory(PushMessage message) {
	 *       JSONObject json = message.toJson();
	 *
	 *       try {
	 *           // Add timestamp
	 *           json.put("received_at", System.currentTimeMillis());
	 *
	 *           // Save to database or file
	 *           saveToDatabase(json.toString());
	 *
	 *       } catch (JSONException e) {
	 *           Log.e("App", "Failed to save notification", e);
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @return JSONObject containing the complete push notification payload
	 * @see #toBundle()
	 * @see #getCustomData()
	 */
	public JSONObject toJson() {
		return PushBundleDataProvider.asJson(extras);
	}

	/**
	 * Gets the message code for this push notification.
	 * <p>
	 * The message code is a unique identifier extracted from the push hash. It can be used
	 * for tracking and identifying specific messages in your analytics or logging systems.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected void onMessageOpened(PushMessage message) {
	 *       String messageCode = message.getMessageCode();
	 *       long campaignId = message.getCampaignId();
	 *
	 *       // Track in analytics with detailed identifiers
	 *       analytics.trackEvent("notification_opened", new HashMap<String, Object>() {{
	 *           put("message_code", messageCode);
	 *           put("campaign_id", campaignId);
	 *           put("title", message.getHeader());
	 *       }});
	 *
	 *       Log.d("App", "Opened message: " + messageCode);
	 *   }
	 * }
	 * </pre>
	 *
	 * @return message code string, or null if not available
	 * @see #getCampaignId()
	 * @see #getMessageId()
	 * @see #getPushHash()
	 */
	public String getMessageCode() {
		return HashDecoder.parseMessageHash(pushHash)[1];
	}

	/**
	 * Gets the campaign ID for this push notification.
	 * <p>
	 * The campaign ID uniquely identifies the push campaign that sent this notification.
	 * Use this for analytics, A/B testing tracking, campaign performance measurement,
	 * and correlating user actions with specific marketing campaigns.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected void onMessageOpened(PushMessage message) {
	 *       long campaignId = message.getCampaignId();
	 *       long messageId = message.getMessageId();
	 *
	 *       // Track campaign conversion
	 *       analytics.trackCampaignConversion(campaignId);
	 *
	 *       // Log detailed campaign metrics
	 *       Log.d("Analytics", String.format(
	 *           "Campaign: %d, Message: %d opened at %s",
	 *           campaignId, messageId, new Date()
	 *       ));
	 *
	 *       // Check if this is from a special campaign
	 *       if (isBlackFridayCampaign(campaignId)) {
	 *           // Show special Black Friday offers
	 *           showBlackFridayDeals();
	 *       }
	 *   }
	 *
	 *   // Track campaign performance in e-commerce
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       if (!message.isSilent()) {
	 *           long campaignId = message.getCampaignId();
	 *
	 *           // Store campaign info for attribution
	 *           SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
	 *           prefs.edit()
	 *               .putLong("last_campaign_id", campaignId)
	 *               .putLong("campaign_received_time", System.currentTimeMillis())
	 *               .apply();
	 *       }
	 *       return false;
	 *   }
	 * }
	 * </pre>
	 *
	 * @return campaign ID as a long value, or 0 if not available
	 * @see #getMessageId()
	 * @see #getMessageCode()
	 */
	public long getCampaignId() {
		String[] parsedMessageHash = HashDecoder.parseMessageHash(pushHash);
		if (parsedMessageHash[0] != null) {
			return Long.parseLong(parsedMessageHash[2]);
		} else return 0;
	}

	/**
	 * Gets the unique message ID for this push notification.
	 * <p>
	 * The message ID uniquely identifies this specific push message instance. Use this for
	 * detailed tracking, deduplication, message history, and correlating user interactions
	 * with specific message deliveries.
	 * <br><br>
	 * Example:
	 * <pre>
	 * {@code
	 *   @Override
	 *   protected boolean onMessageReceived(PushMessage message) {
	 *       long messageId = message.getMessageId();
	 *       long campaignId = message.getCampaignId();
	 *
	 *       // Deduplicate messages
	 *       if (isMessageAlreadyProcessed(messageId)) {
	 *           Log.d("App", "Message " + messageId + " already processed");
	 *           return true; // Suppress duplicate
	 *       }
	 *
	 *       // Store in message history
	 *       saveMessageHistory(messageId, campaignId, message.toJson().toString());
	 *
	 *       // Track delivery in analytics
	 *       analytics.trackEvent("push_delivered", new HashMap<String, Object>() {{
	 *           put("message_id", messageId);
	 *           put("campaign_id", campaignId);
	 *           put("is_silent", message.isSilent());
	 *       }});
	 *
	 *       return false;
	 *   }
	 *
	 *   // Track user journey from notification to conversion
	 *   protected void trackPurchaseAttribution(String orderId, double amount) {
	 *       SharedPreferences prefs = getSharedPreferences("app", MODE_PRIVATE);
	 *       long lastMessageId = prefs.getLong("last_opened_message_id", 0);
	 *       long lastCampaignId = prefs.getLong("last_opened_campaign_id", 0);
	 *
	 *       if (lastMessageId != 0) {
	 *           // Attribute purchase to the notification
	 *           analytics.trackPurchase(orderId, amount, lastCampaignId, lastMessageId);
	 *           Log.d("Analytics", "Purchase attributed to message: " + lastMessageId);
	 *       }
	 *   }
	 * }
	 * </pre>
	 *
	 * @return message ID as a long value, or 0 if not available
	 * @see #getCampaignId()
	 * @see #getMessageCode()
	 * @see #getPushwooshNotificationId()
	 */
	public long getMessageId() {
		String[] parsedMessageHash = HashDecoder.parseMessageHash(pushHash);
		if (parsedMessageHash[0] != null) {
			return Long.parseLong(parsedMessageHash[0]);
		} else return 0;
	}
}
