//[pushwoosh-firebase](../../../index.md)/[com.pushwoosh.firebase](../index.md)/[PushwooshFcmHelper](index.md)/[onMessageReceived](on-message-received.md)

# onMessageReceived

[main]\
open fun [onMessageReceived](on-message-received.md)(context: Context, remoteMessage: RemoteMessage): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

if you use custom com.google.firebase.messaging.FirebaseMessagingService call this method when onMessageReceived is invoked

#### Return

true if the remoteMessage was sent via Pushwoosh and was successfully processed; otherwise false
