//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getLanguage](get-language.md)

# getLanguage

[main]\
open fun [getLanguage](get-language.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)

Returns the current language code used for push notification localization. 

 This method returns either the custom language set via [setLanguage](set-language.md) or the device language if no custom language was set.  Example: 

```kotlin

  String language = Pushwoosh.getInstance().getLanguage();
  Log.d("Pushwoosh", "Current language: " + language);

```

#### Return

Current language code in ISO-639-1 format
