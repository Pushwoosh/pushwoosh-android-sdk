//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getInstance](get-instance.md)

# getInstance

[main]\
open fun [getInstance](get-instance.md)(): [Pushwoosh](index.md)

Returns the shared instance of Pushwoosh SDK. 

 This is the main entry point for all Pushwoosh SDK operations. The instance is created automatically when the SDK is initialized and remains available throughout the application lifecycle.  Example: 

```kotlin

  Pushwoosh pushwoosh = Pushwoosh.getInstance();
  pushwoosh.registerForPushNotifications();

```

#### Return

Pushwoosh shared instance
