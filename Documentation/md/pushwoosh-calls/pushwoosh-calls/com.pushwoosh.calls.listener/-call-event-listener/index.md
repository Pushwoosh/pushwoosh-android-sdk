//[pushwoosh-calls](../../../index.md)/[com.pushwoosh.calls.listener](../index.md)/[CallEventListener](index.md)

# CallEventListener

[androidJvm]\
interface [CallEventListener](index.md)

## Functions

| Name | Summary |
|---|---|
| [onAnswer](on-answer.md) | [androidJvm]<br>abstract fun [onAnswer](on-answer.md)(voIPMessage: [PushwooshVoIPMessage](../../com.pushwoosh.calls/-pushwoosh-vo-i-p-message/index.md), videoState: [Int](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-int/index.html)) |
| [onCallAdded](on-call-added.md) | [androidJvm]<br>abstract fun [onCallAdded](on-call-added.md)(voIPMessage: [PushwooshVoIPMessage](../../com.pushwoosh.calls/-pushwoosh-vo-i-p-message/index.md)) |
| [onCallRemoved](on-call-removed.md) | [androidJvm]<br>abstract fun [onCallRemoved](on-call-removed.md)(voIPMessage: [PushwooshVoIPMessage](../../com.pushwoosh.calls/-pushwoosh-vo-i-p-message/index.md)) |
| [onCreateIncomingConnection](on-create-incoming-connection.md) | [androidJvm]<br>abstract fun [onCreateIncomingConnection](on-create-incoming-connection.md)(payload: [Bundle](https://developer.android.com/reference/kotlin/android/os/Bundle.html)?) |
| [onDisconnect](on-disconnect.md) | [androidJvm]<br>abstract fun [onDisconnect](on-disconnect.md)(voIPMessage: [PushwooshVoIPMessage](../../com.pushwoosh.calls/-pushwoosh-vo-i-p-message/index.md)) |
| [onReject](on-reject.md) | [androidJvm]<br>abstract fun [onReject](on-reject.md)(voIPMessage: [PushwooshVoIPMessage](../../com.pushwoosh.calls/-pushwoosh-vo-i-p-message/index.md)) |
