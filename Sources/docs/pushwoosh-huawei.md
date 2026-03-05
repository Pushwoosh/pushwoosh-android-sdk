# Module pushwoosh-huawei

Integrates Pushwoosh with Huawei Mobile Services (HMS) Push Kit on Android. By default, the SDK
handles HMS automatically and no additional code is required. Add this dependency when your app
targets Huawei devices and needs to deliver push notifications through HMS Push Kit. If your app
registers a custom `HmsMessageService` — for example to co-exist with another push provider — use
[PushwooshHmsHelper] to forward HMS callbacks to Pushwoosh.

# Package com.pushwoosh.huawei

[PushwooshHmsHelper] is required only when your app defines a custom `HmsMessageService`.
Call it from `onNewToken` to keep Pushwoosh informed of token refreshes, and from
`onMessageReceived` to route Pushwoosh messages to the SDK while letting other providers handle
their own messages.
