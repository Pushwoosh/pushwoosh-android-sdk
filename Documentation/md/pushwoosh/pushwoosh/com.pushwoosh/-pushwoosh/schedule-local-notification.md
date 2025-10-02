//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[scheduleLocalNotification](schedule-local-notification.md)

# scheduleLocalNotification

[main]\
open fun [scheduleLocalNotification](schedule-local-notification.md)(notification: [LocalNotification](../../com.pushwoosh.notification/-local-notification/index.md)): [LocalNotificationRequest](../../com.pushwoosh.notification/-local-notification-request/index.md)

Schedules local notification.  Example: 

```kotlin

  LocalNotification notification = new LocalNotification.Builder().setMessage("Local notification content")
			  .setDelay(seconds)
			  .build();
  LocalNotificationRequest request = Pushwoosh.getInstance().scheduleLocalNotification(notification);

```

#### Return

[local notification request](../../com.pushwoosh.notification/-local-notification-request/index.md)

#### Parameters

main

| | |
|---|---|
| notification | [notification](../../com.pushwoosh.notification/-local-notification/index.md) to send |
