//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[addAlternativeAppCode](add-alternative-app-code.md)

# addAlternativeAppCode

[main]\
open fun [addAlternativeAppCode](add-alternative-app-code.md)(appCode: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Adds an alternative Pushwoosh application code for device registration. 

 This method allows registering the device with multiple Pushwoosh applications simultaneously. This is useful for white-label apps, multi-brand applications, or when you need to send pushes from different Pushwoosh applications to the same device.  Example: 

```kotlin

  // Primary app code is set in AndroidManifest.xml or via setAppId()
  Pushwoosh.getInstance().setAppId("XXXXX-XXXXX");

  // Add alternative app codes for white-label brands
  Pushwoosh.getInstance().addAlternativeAppCode("BRAND1-APPID");
  Pushwoosh.getInstance().addAlternativeAppCode("BRAND2-APPID");

  // Device will now receive pushes from all three applications
  Pushwoosh.getInstance().registerForPushNotifications();

```

#### Parameters

main

| | |
|---|---|
| appCode | Alternative Pushwoosh application code to add |

#### See also

| |
|---|
| [setAppId(String)](set-app-id.md) |
| [resetAlternativeAppCodes()](reset-alternative-app-codes.md) |
