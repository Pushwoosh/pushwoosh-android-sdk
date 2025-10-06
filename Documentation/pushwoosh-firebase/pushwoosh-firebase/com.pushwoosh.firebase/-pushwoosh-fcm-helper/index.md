//[pushwoosh-firebase](../../../index.md)/[com.pushwoosh.firebase](../index.md)/[PushwooshFcmHelper](index.md)

# PushwooshFcmHelper

open class [PushwooshFcmHelper](index.md)

Helper class for integrating Pushwoosh with Firebase Cloud Messaging (FCM) in custom FirebaseMessagingService implementations. 

 By default, Pushwoosh SDK automatically handles Firebase Cloud Messaging without any additional code. However, if your app needs a custom FirebaseMessagingService (for example, to handle messages from multiple push providers), use this helper class to forward Firebase callbacks to Pushwoosh. 

**Important:** Use this helper ONLY if you need a custom FirebaseMessagingService. For customizing notifications, use com.pushwoosh.notification.NotificationServiceExtension instead. 

**Critical Integration Steps:**

1. Create your custom FirebaseMessagingService class
2. Override onNewToken and call [onTokenRefresh](on-token-refresh.md)
3. Override onMessageReceived and call [onMessageReceived](on-message-received.md)
4. Register your service in AndroidManifest.xml

**Example - Multiple Push Providers:**

```kotlin

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        // CRITICAL: Forward to Pushwoosh to keep receiving notifications
        PushwooshFcmHelper.onTokenRefresh(token);

        // Forward to other providers if needed
        OtherProvider.setToken(token);
    }

    
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // CRITICAL: Route Pushwoosh messages to Pushwoosh
        if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)) {
            PushwooshFcmHelper.onMessageReceived(this, remoteMessage);
        } else {
            // Handle other providers
            OtherProvider.handleMessage(remoteMessage);
        }
    }
}

```
**AndroidManifest.xml:**```kotlin

<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>

```

#### See also

| |
|---|
| FirebaseMessagingService |
| [onTokenRefresh(String)](on-token-refresh.md) |
| [onMessageReceived(Context, RemoteMessage)](on-message-received.md) |
| [isPushwooshMessage(RemoteMessage)](is-pushwoosh-message.md) |

## Constructors

| | |
|---|---|
| [PushwooshFcmHelper](-pushwoosh-fcm-helper.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [isPushwooshMessage](is-pushwoosh-message.md) | [main]<br>open fun [isPushwooshMessage](is-pushwoosh-message.md)(remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Checks whether a Firebase Cloud Messaging message was sent through Pushwoosh. |
| [messageToBundle](message-to-bundle.md) | [main]<br>open fun [messageToBundle](message-to-bundle.md)(remoteMessage: RemoteMessage): Bundle<br>Converts a Firebase Cloud Messaging RemoteMessage to an Android Bundle. |
| [onMessageReceived](on-message-received.md) | [main]<br>open fun [onMessageReceived](on-message-received.md)(context: Context, remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Processes incoming Firebase Cloud Messaging push notifications for Pushwoosh. |
| [onTokenRefresh](on-token-refresh.md) | [main]<br>open fun [onTokenRefresh](on-token-refresh.md)(token: [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html))<br>Notifies Pushwoosh when Firebase Cloud Messaging token is refreshed. |
