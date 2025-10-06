//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[startServerCommunication](start-server-communication.md)

# startServerCommunication

[main]\
open fun [startServerCommunication](start-server-communication.md)()

Starts communication with the Pushwoosh server. 

 This method enables communication with Pushwoosh servers, allowing the SDK to send and receive data. Use this in conjunction with [stopServerCommunication](stop-server-communication.md) to implement GDPR compliance or user privacy preferences.  Example: 

```kotlin

  // User accepts privacy policy
  if (userAcceptsPrivacyPolicy()) {
      Pushwoosh.getInstance().startServerCommunication();
      Pushwoosh.getInstance().registerForPushNotifications();
  }

```

#### See also

| |
|---|
| [stopServerCommunication()](stop-server-communication.md) |
| [isServerCommunicationAllowed()](is-server-communication-allowed.md) |
