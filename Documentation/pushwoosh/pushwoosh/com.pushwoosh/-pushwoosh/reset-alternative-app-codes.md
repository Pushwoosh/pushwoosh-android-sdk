//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[resetAlternativeAppCodes](reset-alternative-app-codes.md)

# resetAlternativeAppCodes

[main]\
open fun [resetAlternativeAppCodes](reset-alternative-app-codes.md)()

Removes all alternative application codes previously added via [addAlternativeAppCode](add-alternative-app-code.md). 

 After calling this method, the device will only be registered with the primary application code set via [setAppId](set-app-id.md) or AndroidManifest.xml.  Example: 

```kotlin

  // Clear all alternative app codes
  Pushwoosh.getInstance().resetAlternativeAppCodes();

  // Re-register to update on server
  Pushwoosh.getInstance().registerForPushNotifications();

```

#### See also

| |
|---|
| [addAlternativeAppCode(String)](add-alternative-app-code.md) |
| [setAppId(String)](set-app-id.md) |
