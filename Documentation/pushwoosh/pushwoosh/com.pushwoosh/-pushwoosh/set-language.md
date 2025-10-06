//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setLanguage](set-language.md)

# setLanguage

[main]\
open fun [setLanguage](set-language.md)(language: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Sets a custom application language for push notification localization. 

 By default, the SDK uses the device language. This method allows you to override the language for targeting localized push notifications. Set to null to revert to device language.  Example: 

```kotlin

  // Set custom language
  Pushwoosh.getInstance().setLanguage("es");

  // Revert to device language
  Pushwoosh.getInstance().setLanguage(null);

```

#### Parameters

main

| | |
|---|---|
| language | lowercase two-letter code according to ISO-639-1 standard (&quot;en&quot;, &quot;de&quot;, &quot;fr&quot;, etc.) or null (device language). |
