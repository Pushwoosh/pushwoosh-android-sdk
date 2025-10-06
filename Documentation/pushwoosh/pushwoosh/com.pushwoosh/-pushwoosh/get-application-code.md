//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getApplicationCode](get-application-code.md)

# getApplicationCode

[main]\
open fun [getApplicationCode](get-application-code.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)

Returns the current Pushwoosh application code. 

 This method retrieves the application code that was set either via [setAppId](set-app-id.md) or through the &quot;com.pushwoosh.appid&quot; metadata in AndroidManifest.xml. The application code uniquely identifies your app in the Pushwoosh system and can be found in the Pushwoosh Control Panel.  Example: 

```kotlin

  // Get current application code for logging or debugging
  String appCode = Pushwoosh.getInstance().getApplicationCode();
  Log.d("App", "Current Pushwoosh App Code: " + appCode);

  // Verify app code matches expected value
  if (!"XXXXX-XXXXX".equals(appCode)) {
      Log.w("App", "Warning: Unexpected app code configured");
  }

```

#### Return

Current Pushwoosh application code, or empty string if not set or SDK not initialized

#### See also

| |
|---|
| [setAppId(String)](set-app-id.md) |
