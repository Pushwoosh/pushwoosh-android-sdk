//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getSenderId](get-sender-id.md)

# getSenderId

[main]\
open fun [getSenderId](get-sender-id.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)

Returns the current GCM/FCM sender ID. 

 This method retrieves the sender ID that was set either via [setSenderId](set-sender-id.md) or through the &quot;com.pushwoosh.senderid&quot; metadata in AndroidManifest.xml.  Example: 

```kotlin

  String senderId = Pushwoosh.getInstance().getSenderId();
  Log.d("Pushwoosh", "Sender ID: " + senderId);

```

#### Return

Current GCM/FCM sender id
