//[pushwoosh-huawei](../../../../index.md)/[com.pushwoosh.huawei](../../index.md)/[PushwooshHmsHelper](../index.md)/[OnGetTokenAsync](index.md)

# OnGetTokenAsync

interface [OnGetTokenAsync](index.md)

Callback interface for asynchronous HMS token retrieval. 

 Implement this interface to receive callbacks when the HMS token is successfully retrieved or when an error occurs during token retrieval.

#### See also

| |
|---|
| [PushwooshHmsHelper.GetTokenAsync](../-get-token-async/index.md) |

## Functions

| Name | Summary |
|---|---|
| [onError](on-error.md) | [main]<br>abstract fun [onError](on-error.md)(error: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Called when HMS push token retrieval fails. |
| [onGetToken](on-get-token.md) | [main]<br>abstract fun [onGetToken](on-get-token.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))<br>Called when HMS push token is successfully retrieved. |
