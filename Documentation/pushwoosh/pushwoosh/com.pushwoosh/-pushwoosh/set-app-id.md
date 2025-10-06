//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setAppId](set-app-id.md)

# setAppId

[main]\
open fun [setAppId](set-app-id.md)(appId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Associates current application with the given Pushwoosh application code. 

 This method provides a runtime alternative to defining &quot;com.pushwoosh.appid&quot; metadata in AndroidManifest.xml. The application code can be found in your Pushwoosh Control Panel.  Example: 

```kotlin

  Pushwoosh.getInstance().setAppId("XXXXX-XXXXX");

```

#### Parameters

main

| | |
|---|---|
| appId | Pushwoosh application code |
