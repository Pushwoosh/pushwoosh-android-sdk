//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setShowPushnotificationAlert](set-show-pushnotification-alert.md)

# setShowPushnotificationAlert

[main]\
open fun [setShowPushnotificationAlert](set-show-pushnotification-alert.md)(showAlert: [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html))

Controls whether push notifications should be displayed when the app is in foreground. 

 By default, push notifications are shown even when the app is in foreground. Set this to false if you want to suppress notification display when the app is active and handle them programmatically instead. This is useful when you want to show in-app UI instead of system notifications while the app is open.  Example: 

```kotlin

  // Suppress notifications when app is in foreground
  public class MyApplication extends Application {
      
      public void onCreate() {
          super.onCreate();

          // Don't show system notifications when app is active
          Pushwoosh.getInstance().setShowPushnotificationAlert(false);
      }
  }

  // Handle foreground pushes programmatically using NotificationServiceExtension
  public class MyNotificationService extends NotificationServiceExtension {
      
      public boolean onMessageReceived(PushMessage message) {
          if (isAppInForeground()) {
              // Show custom in-app UI instead of notification
              showInAppMessage(message.getMessage());
              return true; // Prevent default notification
          }
          return false; // Show default notification when app is background
      }
  }

  // Re-enable notifications based on user preference
  private void updateNotificationSettings(boolean showNotifications) {
      Pushwoosh.getInstance().setShowPushnotificationAlert(showNotifications);
      Log.d("App", "Foreground notifications " + (showNotifications ? "enabled" : "disabled"));
  }

```

#### Parameters

main

| | |
|---|---|
| showAlert | true to show notifications when app is in foreground (default), false to suppress them |
