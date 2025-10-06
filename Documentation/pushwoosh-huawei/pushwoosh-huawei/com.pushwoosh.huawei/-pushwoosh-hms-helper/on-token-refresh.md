//[pushwoosh-huawei](../../../index.md)/[com.pushwoosh.huawei](../index.md)/[PushwooshHmsHelper](index.md)/[onTokenRefresh](on-token-refresh.md)

# onTokenRefresh

[main]\
open fun [onTokenRefresh](on-token-refresh.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Notifies Pushwoosh when Huawei Mobile Services push token is refreshed. 

**CRITICAL:** Call this method from your custom onNewToken callback to forward the new HMS token to Pushwoosh. Without this call, Pushwoosh will NOT be able to send notifications to the device after token refresh.  Example: 

```kotlin

public void onNewToken(String token) {
    super.onNewToken(token);

    // CRITICAL: Forward token to Pushwoosh
    PushwooshHmsHelper.onTokenRefresh(token);
}

```

#### Parameters

main

| | |
|---|---|
| token | new Huawei Mobile Services push token (can be null) |

#### See also

| |
|---|
| HmsMessageService#onNewToken(String) |
