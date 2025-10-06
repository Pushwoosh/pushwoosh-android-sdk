//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)

# Pushwoosh

open class [Pushwoosh](index.md)

Main entry point for the Pushwoosh SDK. 

 Pushwoosh is a customer engagement platform that helps you turn user data into high-converting omnichannel campaigns. Build personalized customer journeys using advanced segmentation, behavior tracking, and automated messaging across push notifications, email, SMS, and WhatsApp. 

**Key Features:**

- Push Notifications - Send rich push notifications with images, actions, and deep links
- User Segmentation - Tag users and create targeted campaigns based on behavior and preferences
- Cross-Device Tracking - Track users across multiple devices using User ID
- Local Notifications - Schedule notifications to be shown at specific times
- In-App Messages - Display rich in-app content to engaged users
- Analytics - Track push delivery, opens, and user engagement
- Multichannel Campaigns - Send messages via push, email, SMS, and WhatsApp

**Quick Start:**

```kotlin

  // 1. Get Pushwoosh instance (available after SDK initialization)
  Pushwoosh pushwoosh = Pushwoosh.getInstance();

  // 2. Register for push notifications
  pushwoosh.registerForPushNotifications((result) -> {
      if (result.isSuccess()) {
          Log.d("App", "Push registered: " + result.getData().getToken());
      } else {
          Log.e("App", "Registration failed: " + result.getException());
      }
  });

  // 3. Set user tags for targeting
  TagsBundle tags = new TagsBundle.Builder()
      .putString("Name", "John Doe")
      .putInt("Age", 25)
      .putString("Subscription", "premium")
      .build();
  pushwoosh.setTags(tags);

  // 4. Set user ID for cross-device tracking
  pushwoosh.setUserId("user_12345");

```

**Important Notes:**

- Always call [getInstance](get-instance.md) to access the SDK instance
- On Android 13+, request notification permission before registration using [requestNotificationPermission](request-notification-permission.md)
- Set user tags to enable targeted campaigns and personalization
- Use [setUserId](set-user-id.md) to track users across multiple devices
- Handle push notifications using [getLaunchNotification](get-launch-notification.md) for deep linking

#### See also

| |
|---|
| [registerForPushNotifications()](register-for-push-notifications.md) |
| [setTags(TagsBundle)](set-tags.md) |
| [setUserId(String)](set-user-id.md) |
| [scheduleLocalNotification(LocalNotification)](schedule-local-notification.md) |

## Properties

| Name | Summary |
|---|---|
| [PUSH_HISTORY_CAPACITY](-p-u-s-h_-h-i-s-t-o-r-y_-c-a-p-a-c-i-t-y.md) | [main]<br>val [PUSH_HISTORY_CAPACITY](-p-u-s-h_-h-i-s-t-o-r-y_-c-a-p-a-c-i-t-y.md): [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html) = 16<br>Maximum number of notifications returned by [getPushHistory](get-push-history.md) |
| [PUSH_RECEIVE_EVENT](-p-u-s-h_-r-e-c-e-i-v-e_-e-v-e-n-t.md) | [main]<br>val [PUSH_RECEIVE_EVENT](-p-u-s-h_-r-e-c-e-i-v-e_-e-v-e-n-t.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) = &quot;PUSH_RECEIVE_EVENT&quot;<br>Intent extra key for push notification payload. |

## Functions

| Name | Summary |
|---|---|
| [addAlternativeAppCode](add-alternative-app-code.md) | [main]<br>open fun [addAlternativeAppCode](add-alternative-app-code.md)(appCode: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Adds an alternative Pushwoosh application code for device registration. |
| [clearLaunchNotification](clear-launch-notification.md) | [main]<br>open fun [clearLaunchNotification](clear-launch-notification.md)()<br>Clears the launch notification data. |
| [clearPushHistory](clear-push-history.md) | [main]<br>open fun [clearPushHistory](clear-push-history.md)()<br>Clears the push notification history. |
| [enableHuaweiPushNotifications](enable-huawei-push-notifications.md) | [main]<br>open fun [enableHuaweiPushNotifications](enable-huawei-push-notifications.md)()<br>Enables Huawei Push Kit for push notifications on Huawei devices. |
| [getApplicationCode](get-application-code.md) | [main]<br>open fun [getApplicationCode](get-application-code.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Returns the current Pushwoosh application code. |
| [getHwid](get-hwid.md) | [main]<br>open fun [getHwid](get-hwid.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Returns the Pushwoosh Hardware ID (HWID) associated with the current device. |
| [getInstance](get-instance.md) | [main]<br>open fun [getInstance](get-instance.md)(): [Pushwoosh](index.md)<br>Returns the shared instance of Pushwoosh SDK. |
| [getLanguage](get-language.md) | [main]<br>open fun [getLanguage](get-language.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Returns the current language code used for push notification localization. |
| [getLaunchNotification](get-launch-notification.md) | [main]<br>open fun [getLaunchNotification](get-launch-notification.md)(): [PushMessage](../../com.pushwoosh.notification/-push-message/index.md)<br>Returns the push notification that launched the application. |
| [getPushHistory](get-push-history.md) | [main]<br>open fun [getPushHistory](get-push-history.md)(): [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[PushMessage](../../com.pushwoosh.notification/-push-message/index.md)&gt;<br>Returns the push notification history. |
| [getPushToken](get-push-token.md) | [main]<br>open fun [getPushToken](get-push-token.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Returns the current push notification token. |
| [getSenderId](get-sender-id.md) | [main]<br>open fun [getSenderId](get-sender-id.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Returns the current GCM/FCM sender ID. |
| [getTags](get-tags.md) | [main]<br>open fun [getTags](get-tags.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), [GetTagsException](../../com.pushwoosh.exception/-get-tags-exception/index.md)&gt;)<br>Gets tags associated with current device  Example: ```kotlin<br>  // Retrieve and use user profile tags to personalize UI   private void loadUserProfile() {       Pushwoosh.getInstance().getTags((result) -> {           if (result.isSuccess()) {               TagsBundle tags = result.getData();<br>              // Read user profile data               String userName = tags.getString("Name", "Guest");               int userAge = tags.getInt("Age", 0);               String subscriptionTier = tags.getString("Subscription_Tier", "free");               boolean isPremium = "premium".equals(subscriptionTier);<br>              // Update UI based on tags               updateWelcomeMessage("Welcome back, " + userName + "!");               if (isPremium) {                   showPremiumFeatures();               } else {                   showUpgradePrompt();               }<br>              // Check user preferences               List<String> interests = tags.getStringList("Interests");               if (interests != null && !interests.isEmpty()) {                   showPersonalizedContent(interests);               }<br>              Log.d("App", "User profile loaded: " + userName + ", tier: " + subscriptionTier);           } else {               Log.e("App", "Failed to retrieve tags: " + result.getException().getMessage());               // Show default UI               showDefaultContent();           }       });   }<br>``` |
| [getUserId](get-user-id.md) | [main]<br>open fun [getUserId](get-user-id.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)<br>Returns the current user identifier. |
| [isServerCommunicationAllowed](is-server-communication-allowed.md) | [main]<br>open fun [isServerCommunicationAllowed](is-server-communication-allowed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks if communication with Pushwoosh server is currently allowed. |
| [mergeUserId](merge-user-id.md) | [main]<br>open fun [mergeUserId](merge-user-id.md)(oldUserId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), newUserId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), doMerge: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), [MergeUserException](../../com.pushwoosh.exception/-merge-user-exception/index.md)&gt;)<br>Merges or removes event statistics for a user identifier. |
| [registerExistingToken](register-existing-token.md) | [main]<br>open fun [registerExistingToken](register-existing-token.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;)<br>Registers the device using an existing FCM/GCM token. |
| [registerForPushNotifications](register-for-push-notifications.md) | [main]<br>open fun [registerForPushNotifications](register-for-push-notifications.md)()<br>Registers the device for push notifications without a callback.<br>[main]<br>open fun [registerForPushNotifications](register-for-push-notifications.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;)<br>Registers the device for push notifications with a callback. |
| [registerForPushNotificationsWithoutPermission](register-for-push-notifications-without-permission.md) | [main]<br>open fun [registerForPushNotificationsWithoutPermission](register-for-push-notifications-without-permission.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;)<br>Registers the device for push notifications without requesting notification permission. |
| [registerForPushNotificationsWithTags](register-for-push-notifications-with-tags.md) | [main]<br>open fun [registerForPushNotificationsWithTags](register-for-push-notifications-with-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))<br>[main]<br>open fun [registerForPushNotificationsWithTags](register-for-push-notifications-with-tags.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;, tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))<br>Registers the device for push notifications and sets tags in a single request. |
| [registerForPushNotificationsWithTagsWithoutPermission](register-for-push-notifications-with-tags-without-permission.md) | [main]<br>open fun [registerForPushNotificationsWithTagsWithoutPermission](register-for-push-notifications-with-tags-without-permission.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;, tagsBundle: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))<br>Registers the device for push notifications with tags without requesting notification permission. |
| [registerSMSNumber](register-s-m-s-number.md) | [main]<br>open fun [registerSMSNumber](register-s-m-s-number.md)(number: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Registers an SMS number for the current user. |
| [registerWhatsappNumber](register-whatsapp-number.md) | [main]<br>open fun [registerWhatsappNumber](register-whatsapp-number.md)(number: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Registers a WhatsApp number for the current user. |
| [requestNotificationPermission](request-notification-permission.md) | [main]<br>open fun [requestNotificationPermission](request-notification-permission.md)()<br>Requests notification permission from the user. |
| [resetAlternativeAppCodes](reset-alternative-app-codes.md) | [main]<br>open fun [resetAlternativeAppCodes](reset-alternative-app-codes.md)()<br>Removes all alternative application codes previously added via [addAlternativeAppCode](add-alternative-app-code.md). |
| [scheduleLocalNotification](schedule-local-notification.md) | [main]<br>open fun [scheduleLocalNotification](schedule-local-notification.md)(notification: [LocalNotification](../../com.pushwoosh.notification/-local-notification/index.md)): [LocalNotificationRequest](../../com.pushwoosh.notification/-local-notification-request/index.md)<br>Schedules local notification. |
| [sendAppOpen](send-app-open.md) | [main]<br>open fun [sendAppOpen](send-app-open.md)()<br>Sends an application open event to Pushwoosh. |
| [sendInappPurchase](send-inapp-purchase.md) | [main]<br>open fun [sendInappPurchase](send-inapp-purchase.md)(sku: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), price: [BigDecimal](https://developer.android.com/reference/kotlin/java/math/BigDecimal.html), currency: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Sends in-app purchase statistics to Pushwoosh. |
| [setAllowedExternalHosts](set-allowed-external-hosts.md) | [main]<br>open fun [setAllowedExternalHosts](set-allowed-external-hosts.md)(allowedExternalHosts: [ArrayList](https://developer.android.com/reference/kotlin/java/util/ArrayList.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;)<br>Sets the list of allowed external hosts for secure push content. |
| [setApiToken](set-api-token.md) | [main]<br>open fun [setApiToken](set-api-token.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Sets the API access token for Pushwoosh REST API calls. |
| [setAppId](set-app-id.md) | [main]<br>open fun [setAppId](set-app-id.md)(appId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Associates current application with the given Pushwoosh application code. |
| [setEmail](set-email.md) | [main]<br>open fun [setEmail](set-email.md)(email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Registers a single email address for the current user without a callback.<br>[main]<br>open fun [setEmail](set-email.md)(emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;)<br>Registers a list of email addresses for the current user without a callback.<br>[main]<br>open fun [setEmail](set-email.md)(email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), [SetEmailException](../../com.pushwoosh.exception/-set-email-exception/index.md)&gt;)<br>Registers a single email address for the current user with a callback.<br>[main]<br>open fun [setEmail](set-email.md)(emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), [SetEmailException](../../com.pushwoosh.exception/-set-email-exception/index.md)&gt;)<br>Registers a list of email addresses for the current user with a callback. |
| [setEmailTags](set-email-tags.md) | [main]<br>open fun [setEmailTags](set-email-tags.md)(emailTags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>open fun [setEmailTags](set-email-tags.md)(emailTags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), email: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)&gt;)<br>Associates device with given email tags. |
| [setLanguage](set-language.md) | [main]<br>open fun [setLanguage](set-language.md)(language: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Sets a custom application language for push notification localization. |
| [setSenderId](set-sender-id.md) | [main]<br>open fun [setSenderId](set-sender-id.md)(senderId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Sets the FCM/GCM sender ID for push notifications. |
| [setShowPushnotificationAlert](set-show-pushnotification-alert.md) | [main]<br>open fun [setShowPushnotificationAlert](set-show-pushnotification-alert.md)(showAlert: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))<br>Controls whether push notifications should be displayed when the app is in foreground. |
| [setTags](set-tags.md) | [main]<br>open fun [setTags](set-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md))<br>open fun [setTags](set-tags.md)(tags: [TagsBundle](../../com.pushwoosh.tags/-tags-bundle/index.md), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Void](https://developer.android.com/reference/kotlin/java/lang/Void.html), [PushwooshException](../../com.pushwoosh.exception/-pushwoosh-exception/index.md)&gt;)<br>Associates device with given tags. |
| [setUser](set-user.md) | [main]<br>open fun [setUser](set-user.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;)<br>Sets the user identifier and registers associated email addresses without a callback.<br>[main]<br>open fun [setUser](set-user.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), emails: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html)&gt;, callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), [SetUserException](../../com.pushwoosh.exception/-set-user-exception/index.md)&gt;)<br>Sets the user identifier and registers associated email addresses with a callback. |
| [setUserId](set-user-id.md) | [main]<br>open fun [setUserId](set-user-id.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Sets the user identifier without a callback.<br>[main]<br>open fun [setUserId](set-user-id.md)(userId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[Boolean](https://developer.android.com/reference/kotlin/java/lang/Boolean.html), [SetUserIdException](../../com.pushwoosh.exception/-set-user-id-exception/index.md)&gt;)<br>Sets the user identifier with a callback. |
| [startServerCommunication](start-server-communication.md) | [main]<br>open fun [startServerCommunication](start-server-communication.md)()<br>Starts communication with the Pushwoosh server. |
| [stopServerCommunication](stop-server-communication.md) | [main]<br>open fun [stopServerCommunication](stop-server-communication.md)()<br>Stops communication with the Pushwoosh server. |
| [unregisterForPushNotifications](unregister-for-push-notifications.md) | [main]<br>open fun [unregisterForPushNotifications](unregister-for-push-notifications.md)()<br>Unregisters the device from push notifications without a callback.<br>[main]<br>open fun [unregisterForPushNotifications](unregister-for-push-notifications.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html), [UnregisterForPushNotificationException](../../com.pushwoosh.exception/-unregister-for-push-notification-exception/index.md)&gt;)<br>Unregisters the device from push notifications with a callback. |
