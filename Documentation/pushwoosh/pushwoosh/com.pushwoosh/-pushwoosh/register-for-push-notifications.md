//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[registerForPushNotifications](register-for-push-notifications.md)

# registerForPushNotifications

[main]\
open fun [registerForPushNotifications](register-for-push-notifications.md)()

Registers the device for push notifications without a callback. 

 This is a convenience method that calls [registerForPushNotifications](register-for-push-notifications.md) with a null callback. Use this method when you don't need to handle registration results.  Example: 

```kotlin

  // Simple registration without callback
  Pushwoosh.getInstance().registerForPushNotifications();

```

#### See also

| |
|---|
| [registerForPushNotifications(Callback)](register-for-push-notifications.md) |

[main]\
open fun [registerForPushNotifications](register-for-push-notifications.md)(callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;)

Registers the device for push notifications with a callback. 

 This method initiates the registration process with FCM/GCM and registers the device with Pushwoosh. The callback provides information about the registration result including the push token and notification permission status.  Example: 

```kotlin

  // Register for push notifications in Application onCreate or MainActivity
  
  protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);

      // Request notification permission (Android 13+) before registration
      Pushwoosh.getInstance().requestNotificationPermission();

      // Register with callback to handle success/error
      Pushwoosh.getInstance().registerForPushNotifications((result) -> {
          if (result.isSuccess()) {
              String pushToken = result.getData().getToken();
              boolean notificationsEnabled = result.getData().isEnabled();
              Log.d("App", "Push registration successful. Token: " + pushToken);

              // Optionally store registration status
              SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
              prefs.edit().putBoolean("push_registered", true).apply();
          } else {
              Exception exception = result.getException();
              Log.e("App", "Push registration failed: " + exception.getMessage());
              // Show user-friendly error message
              Toast.makeText(this, "Unable to enable notifications", Toast.LENGTH_SHORT).show();
          }
      });
  }

```

#### Parameters

main

| | |
|---|---|
| callback | push registration callback |
