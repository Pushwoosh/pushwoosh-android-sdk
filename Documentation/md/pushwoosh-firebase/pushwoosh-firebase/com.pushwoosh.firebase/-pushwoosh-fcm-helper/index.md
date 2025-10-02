//[pushwoosh-firebase](../../../index.md)/[com.pushwoosh.firebase](../index.md)/[PushwooshFcmHelper](index.md)

# PushwooshFcmHelper

[main]\
open class [PushwooshFcmHelper](index.md)

## Constructors

| | |
|---|---|
| [PushwooshFcmHelper](-pushwoosh-fcm-helper.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [isPushwooshMessage](is-pushwoosh-message.md) | [main]<br>open fun [isPushwooshMessage](is-pushwoosh-message.md)(remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Check if the remoteMessage was sent via Pushwoosh |
| [messageToBundle](message-to-bundle.md) | [main]<br>open fun [messageToBundle](message-to-bundle.md)(remoteMessage: RemoteMessage): Bundle<br>Convert RemoteMessage to Bundle object |
| [onMessageReceived](on-message-received.md) | [main]<br>open fun [onMessageReceived](on-message-received.md)(context: Context, remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>if you use custom com.google.firebase.messaging. |
| [onTokenRefresh](on-token-refresh.md) | [main]<br>open fun [onTokenRefresh](on-token-refresh.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>if you use custom FirebaseMessagingService call this method when onNewToken is invoked |
