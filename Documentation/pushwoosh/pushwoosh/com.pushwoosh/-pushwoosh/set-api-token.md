//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[setApiToken](set-api-token.md)

# setApiToken

[main]\
open fun [setApiToken](set-api-token.md)(token: [String](https://developer.android.com/reference/kotlin/java/lang/String.html))

Sets the API access token for Pushwoosh REST API calls. 

 This method configures the API token used for authenticating REST API requests to Pushwoosh. The API token can be found in your Pushwoosh Control Panel under API Access settings.  Example: 

```kotlin

  // Set API token for server-side operations
  Pushwoosh.getInstance().setApiToken("YOUR_API_TOKEN");

```

#### Parameters

main

| | |
|---|---|
| token | Pushwoosh API access token |
