//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[scheduleLocalNotification](schedule-local-notification.md)

# scheduleLocalNotification

[main]\
open fun [scheduleLocalNotification](schedule-local-notification.md)(notification: [LocalNotification](../../com.pushwoosh.notification/-local-notification/index.md)): [LocalNotificationRequest](../../com.pushwoosh.notification/-local-notification-request/index.md)

Schedules local notification.  Example: 

```kotlin

  // Schedule a reminder notification
  private void scheduleWorkoutReminder() {
      LocalNotification workoutReminder = new LocalNotification.Builder()
          .setMessage("Time for your daily workout!")
          .setTitle("Fitness Reminder")
          .setDelay(3600) // 1 hour from now (in seconds)
          .setCustomData(new Bundle()) // Optional custom data
          .build();

      LocalNotificationRequest request = Pushwoosh.getInstance().scheduleLocalNotification(workoutReminder);
      Log.d("App", "Workout reminder scheduled with ID: " + request.getRequestId());
  }

  // Schedule promotional notification
  private void scheduleFlashSaleNotification() {
      Bundle customData = new Bundle();
      customData.putString("screen", "flash_sale");
      customData.putString("sale_id", "flash_2024_01");

      LocalNotification saleNotification = new LocalNotification.Builder()
          .setTitle("Flash Sale Alert!")
          .setMessage("50% off on selected items. Don't miss out!")
          .setDelay(86400) // 24 hours from now
          .setCustomData(customData)
          .build();

      LocalNotificationRequest request = Pushwoosh.getInstance().scheduleLocalNotification(saleNotification);
      // Store request ID to cancel later if needed
      saveNotificationRequestId(request.getRequestId());
  }

  // Schedule cart abandonment reminder
  private void scheduleCartReminder(int itemCount, double cartTotal) {
      LocalNotification cartReminder = new LocalNotification.Builder()
          .setTitle("Your cart is waiting")
          .setMessage("You have " + itemCount + " items worth $" + cartTotal + " in your cart")
          .setDelay(7200) // 2 hours from now
          .build();

      Pushwoosh.getInstance().scheduleLocalNotification(cartReminder);
  }

```

#### Return

[local notification request](../../com.pushwoosh.notification/-local-notification-request/index.md)

#### Parameters

main

| | |
|---|---|
| notification | [notification](../../com.pushwoosh.notification/-local-notification/index.md) to send |
