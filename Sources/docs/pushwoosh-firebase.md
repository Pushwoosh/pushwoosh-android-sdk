# Module pushwoosh-firebase

Integrates Pushwoosh with Firebase Cloud Messaging (FCM) on Android. By default, the SDK handles
FCM automatically and no additional code is required. Add this dependency when you need to deliver
push notifications through FCM and want optional manual control over the messaging service
integration. If your app registers a custom `FirebaseMessagingService` — for example to co-exist
with another push provider — use [PushwooshFcmHelper] to forward FCM callbacks to Pushwoosh.

# Package com.pushwoosh.firebase

[PushwooshFcmHelper] is required only when your app defines a custom `FirebaseMessagingService`.
Call it from `onNewToken` to keep Pushwoosh informed of token refreshes, and from
`onMessageReceived` to route Pushwoosh messages to the SDK while letting other providers handle
their own messages.
