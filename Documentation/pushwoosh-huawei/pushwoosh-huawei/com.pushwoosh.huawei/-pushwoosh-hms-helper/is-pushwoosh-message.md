//[pushwoosh-huawei](../../../index.md)/[com.pushwoosh.huawei](../index.md)/[PushwooshHmsHelper](index.md)/[isPushwooshMessage](is-pushwoosh-message.md)

# isPushwooshMessage

[main]\
open fun [isPushwooshMessage](is-pushwoosh-message.md)(remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks whether a Huawei Mobile Services message was sent through Pushwoosh. 

 Use this method to determine if an incoming HMS message should be handled by Pushwoosh or by another push notification provider.  Example: 

```kotlin

public void onMessageReceived(RemoteMessage remoteMessage) {
    if (PushwooshHmsHelper.isPushwooshMessage(remoteMessage)) {
        PushwooshHmsHelper.onMessageReceived(this, remoteMessage);
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
| remoteMessage | Huawei Mobile Services remote message to check |

#### See also

| |
|---|
| [onMessageReceived(Context, RemoteMessage)](on-message-received.md) |
