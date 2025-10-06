//[pushwoosh-firebase](../../../index.md)/[com.pushwoosh.firebase](../index.md)/[PushwooshFcmHelper](index.md)/[onTokenRefresh](on-token-refresh.md)

# onTokenRefresh

[main]\
open fun [onTokenRefresh](on-token-refresh.md)(token: [String](https://docs.oracle.com/javase/8/docs/api/java/lang/String.html))

Notifies Pushwoosh when Firebase Cloud Messaging token is refreshed. 

**CRITICAL:** Call this method from your custom onNewToken callback to forward the new FCM token to Pushwoosh. Without this call, Pushwoosh will NOT be able to send notifications to the device after token refresh.  Example: 

```kotlin

public void onNewToken(@NonNull String token) {
    super.onNewToken(token);

    // CRITICAL: Forward token to Pushwoosh
    PushwooshFcmHelper.onTokenRefresh(token);
}

```

#### Parameters

main

| | |
|---|---|
| token | new Firebase Cloud Messaging token |

#### See also

| |
|---|
| FirebaseMessagingService#onNewToken(String) |
