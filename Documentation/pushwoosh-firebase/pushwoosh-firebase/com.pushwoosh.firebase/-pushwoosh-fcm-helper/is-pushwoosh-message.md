//[pushwoosh-firebase](../../../index.md)/[com.pushwoosh.firebase](../index.md)/[PushwooshFcmHelper](index.md)/[isPushwooshMessage](is-pushwoosh-message.md)

# isPushwooshMessage

[main]\
open fun [isPushwooshMessage](is-pushwoosh-message.md)(remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks whether a Firebase Cloud Messaging message was sent through Pushwoosh. 

 Use this method to determine if an incoming FCM message should be handled by Pushwoosh or by another push notification provider.  Example: 

```kotlin

public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
    if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)) {
        PushwooshFcmHelper.onMessageReceived(this, remoteMessage);
    } else {
        // Handle other providers
    }
}

```

#### Return

true if the message was sent via Pushwoosh; false otherwise

#### Parameters

main

| | |
|---|---|
| remoteMessage | Firebase Cloud Messaging remote message to check |

#### See also

| |
|---|
| [onMessageReceived(Context, RemoteMessage)](on-message-received.md) |
