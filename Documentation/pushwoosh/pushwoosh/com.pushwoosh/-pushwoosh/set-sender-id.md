//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setSenderId](set-sender-id.md)

# setSenderId

[main]\
open fun [setSenderId](set-sender-id.md)(senderId: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Sets the FCM/GCM sender ID for push notifications. 

 This method provides a runtime alternative to defining &quot;com.pushwoosh.senderid&quot; metadata in AndroidManifest.xml. The sender ID can be found in your Firebase Console project settings.  Example: 

```kotlin

  Pushwoosh.getInstance().setSenderId("123456789012");

```

#### Parameters

main

| | |
|---|---|
| senderId | GCM/FCM sender id |
