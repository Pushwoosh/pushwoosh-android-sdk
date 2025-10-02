//[pushwoosh-huawei](../../../index.md)/[com.pushwoosh.huawei](../index.md)/[PushwooshHmsHelper](index.md)

# PushwooshHmsHelper

[main]\
open class [PushwooshHmsHelper](index.md)

## Constructors

| | |
|---|---|
| [PushwooshHmsHelper](-pushwoosh-hms-helper.md) | [main]<br>constructor() |

## Types

| Name | Summary |
|---|---|
| [GetTokenAsync](-get-token-async/index.md) | [main]<br>open class [GetTokenAsync](-get-token-async/index.md) |
| [OnGetTokenAsync](-on-get-token-async/index.md) | [main]<br>interface [OnGetTokenAsync](-on-get-token-async/index.md) |

## Functions

| Name | Summary |
|---|---|
| [isPushwooshMessage](is-pushwoosh-message.md) | [main]<br>open fun [isPushwooshMessage](is-pushwoosh-message.md)(remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>Check if the remoteMessage was sent via Pushwoosh |
| [onMessageReceived](on-message-received.md) | [main]<br>open fun [onMessageReceived](on-message-received.md)(context: Context, remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)<br>if you use custom HmsMessageService call this method when onMessageReceived is invoked |
| [onTokenRefresh](on-token-refresh.md) | [main]<br>open fun [onTokenRefresh](on-token-refresh.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>if you use custom HmsMessageService call this method when onNewToken is invoked |
