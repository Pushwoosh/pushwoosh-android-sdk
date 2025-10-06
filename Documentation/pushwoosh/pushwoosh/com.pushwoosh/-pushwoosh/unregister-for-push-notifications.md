//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[unregisterForPushNotifications](unregister-for-push-notifications.md)

# unregisterForPushNotifications

[main]\
open fun [unregisterForPushNotifications](unregister-for-push-notifications.md)()

Unregisters the device from push notifications without a callback. 

 This is a convenience method that calls [unregisterForPushNotifications](unregister-for-push-notifications.md) with a null callback. Use this when you don't need to handle the unregistration result.  Example: 

```kotlin

  // Simple unregistration without callback
  Pushwoosh.getInstance().unregisterForPushNotifications();

```

#### See also

| |
|---|
| [unregisterForPushNotifications(Callback)](unregister-for-push-notifications.md) |

[main]\
open fun [unregisterForPushNotifications](unregister-for-push-notifications.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[String](https://developer.android.com/reference/kotlin/java/lang/String.html), [UnregisterForPushNotificationException](../../com.pushwoosh.exception/-unregister-for-push-notification-exception/index.md)&gt;)

Unregisters the device from push notifications with a callback. 

 This method unregisters the device from Pushwoosh, stopping all push notifications. The device will need to call [registerForPushNotifications](register-for-push-notifications.md) again to receive pushes.  Example: 

```kotlin

  Pushwoosh.getInstance().unregisterForPushNotifications((result) -> {
      if (result.isSuccess()) {
          Log.d("Pushwoosh", "Successfully unregistered");
      } else {
          Exception exception = result.getException();
          Log.e("Pushwoosh", "Failed to unregister: " + exception.getMessage());
      }
  });

```

#### Parameters

main

| | |
|---|---|
| callback | push unregister callback |
