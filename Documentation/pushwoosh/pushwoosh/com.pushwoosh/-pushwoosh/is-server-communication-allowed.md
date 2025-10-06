//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[isServerCommunicationAllowed](is-server-communication-allowed.md)

# isServerCommunicationAllowed

[main]\
open fun [isServerCommunicationAllowed](is-server-communication-allowed.md)(): [Boolean](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin-stdlib/kotlin/-boolean/index.html)

Checks if communication with Pushwoosh server is currently allowed. 

 This method returns the current state of server communication. It returns false if [stopServerCommunication](stop-server-communication.md) was called and true after [startServerCommunication](start-server-communication.md).  Example: 

```kotlin

  if (Pushwoosh.getInstance().isServerCommunicationAllowed()) {
      Log.d("Pushwoosh", "Server communication is enabled");
      Pushwoosh.getInstance().registerForPushNotifications();
  } else {
      Log.d("Pushwoosh", "Server communication is disabled");
  }

```

#### Return

true if communication with Pushwoosh server is allowed, false otherwise

#### See also

| |
|---|
| [startServerCommunication()](start-server-communication.md) |
| [stopServerCommunication()](stop-server-communication.md) |
