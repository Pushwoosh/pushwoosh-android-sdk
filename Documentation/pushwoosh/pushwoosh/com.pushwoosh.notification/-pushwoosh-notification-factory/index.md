//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[PushwooshNotificationFactory](index.md)

# PushwooshNotificationFactory

[main]\
open class [PushwooshNotificationFactory](index.md) : [NotificationFactory](../-notification-factory/index.md)

Default Pushwoosh implementation of NotificationFactory

## Constructors

| | |
|---|---|
| [PushwooshNotificationFactory](-pushwoosh-notification-factory.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [channelDescription](../-notification-factory/channel-description.md) | [main]<br>open fun [channelDescription](../-notification-factory/channel-description.md)(channelName: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [channelName](../-notification-factory/channel-name.md) | [main]<br>open fun [channelName](../-notification-factory/channel-name.md)(channelName: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getNotificationIntent](../-notification-factory/get-notification-intent.md) | [main]<br>open fun [getNotificationIntent](../-notification-factory/get-notification-intent.md)(data: [PushMessage](../-push-message/index.md)): Intent |
| [onGenerateNotification](on-generate-notification.md) | [main]<br>open fun [onGenerateNotification](on-generate-notification.md)(pushData: [PushMessage](../-push-message/index.md)): Notification<br>Generates notification using PushMessage data. |
