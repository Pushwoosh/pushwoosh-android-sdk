//[pushwoosh-huawei](../../../../index.md)/[com.pushwoosh.huawei](../../index.md)/[PushwooshHmsHelper](../index.md)/[OnGetTokenAsync](index.md)/[onGetToken](on-get-token.md)

# onGetToken

[main]\
abstract fun [onGetToken](on-get-token.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Called when HMS push token is successfully retrieved. 

 This callback is executed on the main thread after the token has been retrieved from Huawei Mobile Services. Use this to store the token, send it to your server, or forward it to Pushwoosh.

#### Parameters

main

| | |
|---|---|
| token | the retrieved HMS push token |
