//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[sendAppOpen](send-app-open.md)

# sendAppOpen

[main]\
open fun [sendAppOpen](send-app-open.md)()

Sends an application open event to Pushwoosh. 

 This method is usually called automatically by the SDK when the application launches. However, in some custom integration scenarios, you may need to call it manually to ensure proper tracking of app opens and session analytics.  Example: 

```kotlin

  // Manually send app open event (usually not needed)
  Pushwoosh.getInstance().sendAppOpen();

```
