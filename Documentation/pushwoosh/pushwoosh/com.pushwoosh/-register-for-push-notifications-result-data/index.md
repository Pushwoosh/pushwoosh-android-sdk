//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[RegisterForPushNotificationsResultData](index.md)

# RegisterForPushNotificationsResultData

open class [RegisterForPushNotificationsResultData](index.md)

Result data returned after successful push notification registration. 

 This class contains the push token and notification permission status received when registering the device for push notifications via [registerForPushNotifications](../-pushwoosh/register-for-push-notifications.md). The data is provided in the success callback and can be used to verify registration status and retrieve the push token for server-side operations. 

**Usage Example:**

```kotlin

Pushwoosh.getInstance().registerForPushNotifications((result) -> {
    if (result.isSuccess()) {
        RegisterForPushNotificationsResultData data = result.getData();

        // Get the push token
        String pushToken = data.getToken();
        Log.d("App", "Push token: " + pushToken);

        // Check if notifications are enabled
        boolean notificationsEnabled = data.isEnabled();
        if (notificationsEnabled) {
            Log.d("App", "User has granted notification permission");
        } else {
            Log.w("App", "Notifications are disabled by user");
            showEnableNotificationsPrompt();
        }

        // Send token to your backend server
        sendTokenToServer(pushToken);
    }
});

```

#### See also

| |
|---|
| [Pushwoosh](../-pushwoosh/register-for-push-notifications-with-tags.md) |

## Constructors

| | |
|---|---|
| [RegisterForPushNotificationsResultData](-register-for-push-notifications-result-data.md) | [main]<br>constructor(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), enabled: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)) |

## Properties

| Name | Summary |
|---|---|
| [token](token.md) | [main]<br>val [token](token.md): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |

## Functions

| Name | Summary |
|---|---|
| [isEnabled](is-enabled.md) | [main]<br>open fun [isEnabled](is-enabled.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Returns whether push notifications are currently enabled for this app. |
