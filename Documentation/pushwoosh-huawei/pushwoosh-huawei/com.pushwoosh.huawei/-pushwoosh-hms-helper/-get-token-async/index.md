//[pushwoosh-huawei](../../../../index.md)/[com.pushwoosh.huawei](../../index.md)/[PushwooshHmsHelper](../index.md)/[GetTokenAsync](index.md)

# GetTokenAsync

open class [GetTokenAsync](index.md)

AsyncTask for retrieving Huawei Mobile Services push token asynchronously. 

 Use this class when you need to manually retrieve the HMS push token for advanced scenarios such as manual registration or sending the token to your backend server.  Example: 

```kotlin

new PushwooshHmsHelper.GetTokenAsync(new PushwooshHmsHelper.OnGetTokenAsync() {

    
    public void onGetToken(String token) {
        // Token retrieved successfully
        PushwooshHmsHelper.onTokenRefresh(token);
    }

    
    public void onError(String error) {
        // Token retrieval failed
        Log.e("HMS", "Failed: " + error);
    }
}).execute();

```

#### See also

| |
|---|
| [PushwooshHmsHelper.OnGetTokenAsync](../-on-get-token-async/index.md) |
| [onTokenRefresh(String)](../on-token-refresh.md) |

## Constructors

| | |
|---|---|
| [GetTokenAsync](-get-token-async.md) | [main]<br>constructor(onGetTokenAsync: [PushwooshHmsHelper.OnGetTokenAsync](../-on-get-token-async/index.md)) |
