//[pushwoosh-huawei](../../../index.md)/[com.pushwoosh.huawei](../index.md)/[PushwooshHmsHelper](index.md)/[onMessageReceived](on-message-received.md)

# onMessageReceived

[main]\
open fun [onMessageReceived](on-message-received.md)(context: Context, remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Processes incoming Huawei Mobile Services push notifications for Pushwoosh. 

**CRITICAL:** Call this method from your custom onMessageReceived callback to let Pushwoosh handle its messages. Without this call, Pushwoosh notifications will NOT be displayed.  Example: 

```kotlin

public void onMessageReceived(RemoteMessage remoteMessage) {
    super.onMessageReceived(remoteMessage);

    // CRITICAL: Route Pushwoosh messages to Pushwoosh
    if (PushwooshHmsHelper.isPushwooshMessage(remoteMessage)) {
        PushwooshHmsHelper.onMessageReceived(this, remoteMessage);
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
| remoteMessage | Huawei Mobile Services remote message |

#### See also

| |
|---|
| HmsMessageService#onMessageReceived(RemoteMessage) |
| [isPushwooshMessage(RemoteMessage)](is-pushwoosh-message.md) |
