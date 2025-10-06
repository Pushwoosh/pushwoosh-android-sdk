//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[registerForPushNotificationsWithoutPermission](register-for-push-notifications-without-permission.md)

# registerForPushNotificationsWithoutPermission

[main]\
open fun [registerForPushNotificationsWithoutPermission](register-for-push-notifications-without-permission.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;)

Registers the device for push notifications without requesting notification permission. 

 This method is useful when you want to handle notification permission request yourself or when you've already requested the permission separately. On Android 13+ (API 33+), if notification permission is not granted, the registration will still succeed but notifications won't be displayed to the user.  Example: 

```kotlin

  // Custom permission flow - register after user grants permission
  private void onNotificationPermissionGranted() {
      // Permission already handled by custom flow
      Pushwoosh.getInstance().registerForPushNotificationsWithoutPermission((result) -> {
          if (result.isSuccess()) {
              String token = result.getData().getToken();
              Log.d("App", "Registered with token: " + token);
              updateSubscriptionStatus(true);
          } else {
              Log.e("App", "Registration failed: " + result.getException().getMessage());
          }
      });
  }

  // Silent registration for background services
  private void registerSilently() {
      // Register without showing permission dialog
      Pushwoosh.getInstance().registerForPushNotificationsWithoutPermission((result) -> {
          if (result.isSuccess()) {
              // Device registered, can receive data pushes
              Log.d("App", "Background registration complete");
          }
      });
  }

```

#### Parameters

main

| | |
|---|---|
| callback | push registration callback |

#### See also

| |
|---|
| [registerForPushNotifications(Callback)](register-for-push-notifications.md) |
| [requestNotificationPermission()](request-notification-permission.md) |
