//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[enableHuaweiPushNotifications](enable-huawei-push-notifications.md)

# enableHuaweiPushNotifications

[main]\
open fun [enableHuaweiPushNotifications](enable-huawei-push-notifications.md)()

Enables Huawei Push Kit for push notifications on Huawei devices. 

 This method is specifically designed for plugin-based applications (Cordova, React Native, etc.) to enable Huawei Push Kit support on Huawei devices without Google Mobile Services. This method has no effect when called in native Android applications.  Example: 

```kotlin

  // Enable Huawei Push in plugin-based applications
  Pushwoosh.getInstance().enableHuaweiPushNotifications();
  Pushwoosh.getInstance().registerForPushNotifications();

```
