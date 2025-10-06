//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[registerExistingToken](register-existing-token.md)

# registerExistingToken

[main]\
open fun [registerExistingToken](register-existing-token.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html), callback: [Callback](../../com.pushwoosh.function/-callback/index.md)&lt;[RegisterForPushNotificationsResultData](../-register-for-push-notifications-result-data/index.md), [RegisterForPushNotificationsException](../../com.pushwoosh.exception/-register-for-push-notifications-exception/index.md)&gt;)

Registers the device using an existing FCM/GCM token. 

 This method is useful when you already have a push token obtained from Firebase and want to register it with Pushwoosh without going through the full registration flow.  Example: 

```kotlin

  // Assuming you obtained the token from FirebaseMessaging
  FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
      if (task.isSuccessful()) {
          String token = task.getResult();
          Pushwoosh.getInstance().registerExistingToken(token, (result) -> {
              if (result.isSuccess()) {
                  Log.d("Pushwoosh", "Token registered successfully");
              } else {
                  Log.e("Pushwoosh", "Failed: " + result.getException().getMessage());
              }
          });
      }
  });

```

#### Parameters

main

| | |
|---|---|
| token | FCM/GCM push token |
| callback | registration callback |
