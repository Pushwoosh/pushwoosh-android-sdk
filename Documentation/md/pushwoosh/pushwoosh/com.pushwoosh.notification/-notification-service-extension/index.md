//[pushwoosh](../../../index.md)/[com.pushwoosh.notification](../index.md)/[NotificationServiceExtension](index.md)

# NotificationServiceExtension

[main]\
open class [NotificationServiceExtension](index.md)

NotificationServiceExtension allows to customize push notification behaviour. All NotificationServiceExtension ancestors must be public and must contain public constructor without parameters. Application will crash on startup if this requirement is not met. Custom NotificationServiceExtension should be registered in AndroidManifest.xml metadata as follows: 

```kotlin

        <meta-data
            android:name="com.pushwoosh.notification_service_extension"
            android:value="com.your.package.YourNotificationServiceExtension" />
    
```

## Constructors

| | |
|---|---|
| [NotificationServiceExtension](-notification-service-extension.md) | [main]<br>constructor() |

## Functions

| Name | Summary |
|---|---|
| [handleMessage](handle-message.md) | [main]<br>fun [handleMessage](handle-message.md)(pushBundle: Bundle)<br>Handles push arrival. |
| [handleNotification](handle-notification.md) | [main]<br>fun [handleNotification](handle-notification.md)(pushBundle: Bundle)<br>Handles notification open. |
| [handleNotificationCanceled](handle-notification-canceled.md) | [main]<br>fun [handleNotificationCanceled](handle-notification-canceled.md)(pushBundle: Bundle)<br>Handles notification cancel. |
| [handleNotificationGroup](handle-notification-group.md) | [main]<br>fun [handleNotificationGroup](handle-notification-group.md)(messages: [List](https://developer.android.com/reference/kotlin/java/util/List.html)&lt;[PushMessage](../-push-message/index.md)&gt;)<br>Handles notifications group open. |
