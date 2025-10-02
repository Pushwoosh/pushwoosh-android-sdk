//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[NotificationFactory](index.md)

# NotificationFactory

abstract class [NotificationFactory](index.md)

Abstract class that is used to customize push notification appearance. All NotificationFactory ancestors must be public and must contain public constructor without parameters. Application will crash on startup if this requirement is not met. Custom NotificationFactory should be registered in AndroidManifest.xml metadata as follows: 

```kotlin

        <meta-data
            android:name="com.pushwoosh.notification_factory"
            android:value="com.your.package.YourNotificationFactory" />
    
```

#### Inheritors

| |
|---|
| [PushwooshNotificationFactory](../-pushwoosh-notification-factory/index.md) |

## Constructors

| | |
|---|---|
| [NotificationFactory](-notification-factory.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [channelDescription](channel-description.md) | [main]<br>open fun [channelDescription](channel-description.md)(channelName: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [channelName](channel-name.md) | [main]<br>open fun [channelName](channel-name.md)(channelName: [String](https://developer.android.com/reference/kotlin/java/lang/String.html)): [String](https://developer.android.com/reference/kotlin/java/lang/String.html) |
| [getNotificationIntent](get-notification-intent.md) | [main]<br>open fun [getNotificationIntent](get-notification-intent.md)(data: [PushMessage](../-push-message/index.md)): Intent |
| [onGenerateNotification](on-generate-notification.md) | [main]<br>abstract fun [onGenerateNotification](on-generate-notification.md)(data: [PushMessage](../-push-message/index.md)): Notification<br>Generates notification using PushMessage data. |
