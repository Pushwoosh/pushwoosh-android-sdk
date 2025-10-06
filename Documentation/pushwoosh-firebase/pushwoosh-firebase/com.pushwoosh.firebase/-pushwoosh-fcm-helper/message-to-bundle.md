//[pushwoosh-firebase](../../../index.md)/[com.pushwoosh.firebase](../index.md)/[PushwooshFcmHelper](index.md)/[messageToBundle](message-to-bundle.md)

# messageToBundle

[main]\
open fun [messageToBundle](message-to-bundle.md)(remoteMessage: RemoteMessage): Bundle

Converts a Firebase Cloud Messaging RemoteMessage to an Android Bundle. 

 Use this utility method when you need to pass message data to other Android components or implement custom message processing logic.

#### Return

Bundle containing all data from the remote message

#### Parameters

main

| | |
|---|---|
| remoteMessage | Firebase Cloud Messaging remote message to convert |

#### See also

| |
|---|
| RemoteMessage |
| [onMessageReceived(Context, RemoteMessage)](on-message-received.md) |
