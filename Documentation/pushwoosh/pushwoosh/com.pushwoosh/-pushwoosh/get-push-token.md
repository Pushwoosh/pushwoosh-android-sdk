//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[getPushToken](get-push-token.md)

# getPushToken

[main]\
open fun [getPushToken](get-push-token.md)(): [String](https://developer.android.com/reference/kotlin/java/lang/String.html)

Returns the current push notification token. 

 This method returns the FCM/GCM token obtained after successful registration. The token may be null if the device hasn't been registered yet or if registration is in progress.  Example: 

```kotlin

  String pushToken = Pushwoosh.getInstance().getPushToken();
  if (pushToken != null && !pushToken.isEmpty()) {
      Log.d("Pushwoosh", "Push token: " + pushToken);
  } else {
      Log.d("Pushwoosh", "Device not registered yet");
  }

```

#### Return

Push notification token or null if device is not registered yet.
