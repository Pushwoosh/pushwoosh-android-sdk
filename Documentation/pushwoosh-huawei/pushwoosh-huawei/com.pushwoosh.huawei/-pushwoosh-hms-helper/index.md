//[pushwoosh-huawei](../../../index.md)/[com.pushwoosh.huawei](../index.md)/[PushwooshHmsHelper](index.md)

# PushwooshHmsHelper

open class [PushwooshHmsHelper](index.md)

Helper class for integrating Pushwoosh with Huawei Mobile Services (HMS) Push Kit in custom HmsMessageService implementations. 

 By default, Pushwoosh SDK automatically handles Huawei Mobile Services push notifications without any additional code. However, if your app needs a custom HmsMessageService (for example, to handle messages from multiple push providers), use this helper class to forward HMS callbacks to Pushwoosh. 

**Important:** Use this helper ONLY if you need a custom HmsMessageService. For customizing notifications, use com.pushwoosh.notification.NotificationServiceExtension instead. 

**Critical Integration Steps:**

1. Add HMS Core SDK dependencies and configure agconnect-services.json
2. Create your custom HmsMessageService class
3. Override onNewToken and call [onTokenRefresh](on-token-refresh.md)
4. Override onMessageReceived and call [onMessageReceived](on-message-received.md)
5. Register your service in AndroidManifest.xml

**Example - Multiple Push Providers:**

```kotlin

public class MyHmsMessageService extends HmsMessageService {

    
    public void onNewToken(String token) {
        super.onNewToken(token);

        // CRITICAL: Forward to Pushwoosh to keep receiving notifications
        PushwooshHmsHelper.onTokenRefresh(token);

        // Forward to other providers if needed
        OtherProvider.setToken(token);
    }

    
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // CRITICAL: Route Pushwoosh messages to Pushwoosh
        if (PushwooshHmsHelper.isPushwooshMessage(remoteMessage)) {
            PushwooshHmsHelper.onMessageReceived(this, remoteMessage);
        } else {
            // Handle other providers
            OtherProvider.handleMessage(remoteMessage);
        }
    }
}

```
**AndroidManifest.xml:**```kotlin

<service
    android:name=".MyHmsMessageService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.huawei.push.action.MESSAGING_EVENT" />
    </intent-filter>
</service>

```

#### See also

| |
|---|
| HmsMessageService |
| [onTokenRefresh(String)](on-token-refresh.md) |
| [onMessageReceived(Context, RemoteMessage)](on-message-received.md) |
| [isPushwooshMessage(RemoteMessage)](is-pushwoosh-message.md) |
| [PushwooshHmsHelper.GetTokenAsync](-get-token-async/index.md) |

## Constructors

| | |
|---|---|
| [PushwooshHmsHelper](-pushwoosh-hms-helper.md) | [main]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [GetTokenAsync](-get-token-async/index.md) | [main]<br>open class [GetTokenAsync](-get-token-async/index.md)<br>AsyncTask for retrieving Huawei Mobile Services push token asynchronously. |
| [OnGetTokenAsync](-on-get-token-async/index.md) | [main]<br>interface [OnGetTokenAsync](-on-get-token-async/index.md)<br>Callback interface for asynchronous HMS token retrieval. |

## Functions

| Name | Summary |
|---|---|
| [isPushwooshMessage](is-pushwoosh-message.md) | [main]<br>open fun [isPushwooshMessage](is-pushwoosh-message.md)(remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks whether a Huawei Mobile Services message was sent through Pushwoosh. |
| [onMessageReceived](on-message-received.md) | [main]<br>open fun [onMessageReceived](on-message-received.md)(context: Context, remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Processes incoming Huawei Mobile Services push notifications for Pushwoosh. |
| [onTokenRefresh](on-token-refresh.md) | [main]<br>open fun [onTokenRefresh](on-token-refresh.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Notifies Pushwoosh when Huawei Mobile Services push token is refreshed. |
