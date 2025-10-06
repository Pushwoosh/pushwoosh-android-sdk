//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[requestNotificationPermission](request-notification-permission.md)

# requestNotificationPermission

[main]\
open fun [requestNotificationPermission](request-notification-permission.md)()

Requests notification permission from the user. 

 On Android 13 (API level 33) and above, this method prompts the user to grant notification permission. On earlier Android versions, this method has no effect as notification permission is granted by default.  Example: 

```kotlin

  // Request permission before registering for push notifications
  Pushwoosh.getInstance().requestNotificationPermission();

```
