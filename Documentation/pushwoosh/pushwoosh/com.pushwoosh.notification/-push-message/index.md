//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[PushMessage](index.md)

# PushMessage

open class [PushMessage](index.md)

Represents a received push notification with all its payload data and metadata. 

 PushMessage is an immutable data class that encapsulates all information contained in a push notification, including the notification content (title, message), visual customization (icons, colors, LED), behavior settings (sound, vibration, priority), and custom data payloads. It serves as the primary interface for accessing push notification data in callbacks, service extensions, and message handlers. 

**When you receive PushMessage instances:**

- In onMessageReceived when push arrives
- In onMessageOpened when user taps notification
- Via [getLaunchNotification](../../com.pushwoosh/-pushwoosh/get-launch-notification.md) to get the notification that launched the app
- In broadcast receivers handling Pushwoosh notification events

**Common use cases:**

- Extracting custom data from push notifications to route users to specific screens
- Customizing notification appearance based on payload before display
- Logging analytics events based on push content or campaign data
- Handling silent pushes to sync data without showing notifications
- Implementing custom business logic based on push metadata

**Basic usage example - Handling custom data:**```kotlin

public class MyNotificationExtension extends NotificationServiceExtension {
    
    protected boolean onMessageReceived(PushMessage message) {
        // Extract custom data from push payload
        String customData = message.getCustomData();
        if (customData != null) {
            try {
                JSONObject data = new JSONObject(customData);
                String screen = data.optString("target_screen");
                String itemId = data.optString("item_id");

                // Route to specific screen based on custom data
                if ("product_detail".equals(screen)) {
                    navigateToProduct(itemId);
                } else if ("cart".equals(screen)) {
                    navigateToCart();
                }
            } catch (JSONException e) {
                Log.e("App", "Failed to parse custom data", e);
            }
        }

        // Return false to show default notification
        return false;
    }
}

```
**Silent push example - Background data sync:**```kotlin

protected boolean onMessageReceived(PushMessage message) {
    // Check if this is a silent push
    if (message.isSilent()) {
        // Silent push - no notification shown
        String customData = message.getCustomData();
        if (customData != null) {
            JSONObject data = new JSONObject(customData);
            String action = data.optString("action");

            if ("sync_data".equals(action)) {
                // Trigger background data sync
                syncUserData();
            } else if ("clear_cache".equals(action)) {
                clearAppCache();
            }
        }
        // Return true to indicate we handled the push
        return true;
    }

    // Return false to show default notification
    return false;
}

```
**Launch notification example - Deep linking:**```kotlin

protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    // Check if app was opened from a push notification
    PushMessage message = Pushwoosh.getInstance().getLaunchNotification();
    if (message != null) {
        String customData = message.getCustomData();
        if (customData != null) {
            try {
                JSONObject data = new JSONObject(customData);
                String orderId = data.optString("order_id");

                if (!orderId.isEmpty()) {
                    // Navigate to order details screen
                    Intent intent = new Intent(this, OrderDetailActivity.class);
                    intent.putExtra("order_id", orderId);
                    startActivity(intent);
                }
            } catch (JSONException e) {
                Log.e("App", "Failed to parse launch notification data", e);
            }
        }

        // Clear launch notification so it's not processed again
        Pushwoosh.getInstance().clearLaunchNotification();
    }
}

```
**Campaign tracking example - Analytics integration:**```kotlin

protected void onMessageOpened(PushMessage message) {
    super.onMessageOpened(message);

    // Track push notification opens in your analytics
    long campaignId = message.getCampaignId();
    long messageId = message.getMessageId();
    String messageCode = message.getMessageCode();

    // Send event to analytics service
    Analytics.logEvent("push_opened", new HashMap<String, Object>() {{
        put("campaign_id", campaignId);
        put("message_id", messageId);
        put("message_code", messageCode);
        put("title", message.getHeader());
    }});

    // Log custom data for segmentation
    String customData = message.getCustomData();
    if (customData != null) {
        try {
            JSONObject data = new JSONObject(customData);
            String category = data.optString("category", "general");
            Analytics.setUserProperty("last_notification_category", category);
        } catch (JSONException e) {
            Log.e("App", "Failed to parse custom data", e);
        }
    }
}

```

#### See also

| |
|---|
| [NotificationServiceExtension](../-notification-service-extension/index.md) |
| [Pushwoosh](../../com.pushwoosh/-pushwoosh/get-launch-notification.md) |
| [LocalNotification](../-local-notification/index.md) |

## Constructors

| | |
|---|---|
| [PushMessage](-push-message.md) | [main]<br>constructor(extras: Bundle) |

## Properties

| Name | Summary |
|---|---|
| [actions](actions.md) | [main]<br>val [actions](actions.md): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;Action&gt; |
| [badges](badges.md) | [main]<br>val [badges](badges.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [bigPictureUrl](big-picture-url.md) | [main]<br>val [bigPictureUrl](big-picture-url.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [customData](custom-data.md) | [main]<br>val [customData](custom-data.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [groupId](group-id.md) | [main]<br>val [groupId](group-id.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [header](header.md) | [main]<br>val [header](header.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [iconBackgroundColor](icon-background-color.md) | [main]<br>val [iconBackgroundColor](icon-background-color.md): [Integer](https://developer.android.com/reference/kotlin/java/lang/Integer.html) |
| [largeIconUrl](large-icon-url.md) | [main]<br>val [largeIconUrl](large-icon-url.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [led](led.md) | [main]<br>val [led](led.md): [Integer](https://developer.android.com/reference/kotlin/java/lang/Integer.html) |
| [ledOffMS](led-off-m-s.md) | [main]<br>val [ledOffMS](led-off-m-s.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [ledOnMS](led-on-m-s.md) | [main]<br>val [ledOnMS](led-on-m-s.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [message](message.md) | [main]<br>val [message](message.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [priority](priority.md) | [main]<br>val [priority](priority.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [pushHash](push-hash.md) | [main]<br>val [pushHash](push-hash.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [smallIcon](small-icon.md) | [main]<br>val [smallIcon](small-icon.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |
| [sound](sound.md) | [main]<br>val [sound](sound.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [ticker](ticker.md) | [main]<br>val [ticker](ticker.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [vibration](vibration.md) | [main]<br>val [vibration](vibration.md): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [visibility](visibility.md) | [main]<br>val [visibility](visibility.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) |

## Functions

| Name | Summary |
|---|---|
| [getCampaignId](get-campaign-id.md) | [main]<br>open fun [getCampaignId](get-campaign-id.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getMessageCode](get-message-code.md) | [main]<br>open fun [getMessageCode](get-message-code.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getMessageId](get-message-id.md) | [main]<br>open fun [getMessageId](get-message-id.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getPushMetaData](get-push-meta-data.md) | [main]<br>open fun [getPushMetaData](get-push-meta-data.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getPushwooshNotificationId](get-pushwoosh-notification-id.md) | [main]<br>open fun [getPushwooshNotificationId](get-pushwoosh-notification-id.md)(): [Long](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-long/index.html) |
| [getTag](get-tag.md) | [main]<br>open fun [getTag](get-tag.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [isBadgesAdditive](is-badges-additive.md) | [main]<br>open fun [isBadgesAdditive](is-badges-additive.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isLocal](is-local.md) | [main]<br>open fun [isLocal](is-local.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isLockScreen](is-lock-screen.md) | [main]<br>open fun [isLockScreen](is-lock-screen.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [isSilent](is-silent.md) | [main]<br>open fun [isSilent](is-silent.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html) |
| [toBundle](to-bundle.md) | [main]<br>open fun [toBundle](to-bundle.md)(): Bundle |
| [toJson](to-json.md) | [main]<br>open fun [toJson](to-json.md)(): JSONObject |
