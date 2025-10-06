//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[clearLaunchNotification](clear-launch-notification.md)

# clearLaunchNotification

[main]\
open fun [clearLaunchNotification](clear-launch-notification.md)()

Clears the launch notification data. 

 After calling this method, [getLaunchNotification](get-launch-notification.md) will return null until the app is launched from another push notification. This is useful to prevent processing the same launch notification multiple times.  Example: 

```kotlin

  PushMessage launchNotification = Pushwoosh.getInstance().getLaunchNotification();
  if (launchNotification != null) {
      // Process the launch notification
      handlePushMessage(launchNotification);
      // Clear it to prevent reprocessing
      Pushwoosh.getInstance().clearLaunchNotification();
  }

```
