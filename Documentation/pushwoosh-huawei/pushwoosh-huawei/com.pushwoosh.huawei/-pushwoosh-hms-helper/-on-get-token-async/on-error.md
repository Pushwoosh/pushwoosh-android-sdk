//[pushwoosh-huawei](../../../../index.md)/[com.pushwoosh.huawei](../../index.md)/[PushwooshHmsHelper](../index.md)/[OnGetTokenAsync](index.md)/[onError](on-error.md)

# onError

[main]\
abstract fun [onError](on-error.md)(error: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Called when HMS push token retrieval fails. 

 This callback is executed on the main thread if token retrieval encounters an error. Common errors include missing agconnect-services.json configuration or HMS API failures. 

**Common error messages:**

- &quot;Missing client/app_id key in agconnect-services.json&quot; - Configuration error
- &quot;Status code: XXXX&quot; - HMS API error (see Huawei documentation for error codes)
- &quot;Context is null&quot; - Application context not available

#### Parameters

main

| | |
|---|---|
| error | error message describing what went wrong |
