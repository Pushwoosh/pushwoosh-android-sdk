//[pushwoosh-firebase](../../../index.md)/[com.pushwoosh.firebase](../index.md)/[PushwooshFcmHelper](index.md)/[onMessageReceived](on-message-received.md)

# onMessageReceived

[main]\
open fun [onMessageReceived](on-message-received.md)(context: Context, remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Processes incoming Firebase Cloud Messaging push notifications for Pushwoosh. 

**CRITICAL:** Call this method from your custom onMessageReceived callback to let Pushwoosh handle its messages. Without this call, Pushwoosh notifications will NOT be displayed.  Example: 

```kotlin

public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    super.onMessageReceived(remoteMessage);

    // CRITICAL: Route Pushwoosh messages to Pushwoosh
    if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)) {
        PushwooshFcmHelper.onMessageReceived(this, remoteMessage);
    } else {
        // Handle other providers
    }
}

```

#### Return

true if the message was sent via Pushwoosh and was successfully processed; false otherwise

#### Parameters

main

| | |
|---|---|
| context | application or service context |
| remoteMessage | Firebase Cloud Messaging remote message |

#### See also

| |
|---|
| FirebaseMessagingService#onMessageReceived(RemoteMessage) |
| [isPushwooshMessage(RemoteMessage)](is-pushwoosh-message.md) |
