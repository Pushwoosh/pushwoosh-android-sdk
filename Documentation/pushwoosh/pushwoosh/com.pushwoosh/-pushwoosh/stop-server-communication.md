//[pushwoosh](../../../index.md)/[com.pushwoosh](../index.md)/[Pushwoosh](index.md)/[stopServerCommunication](stop-server-communication.md)

# stopServerCommunication

[main]\
open fun [stopServerCommunication](stop-server-communication.md)()

Stops communication with the Pushwoosh server. 

 This method disables all communication with Pushwoosh servers. The SDK will not send any data or register for push notifications until [startServerCommunication](start-server-communication.md) is called. Use this to implement GDPR compliance or user privacy preferences.  Example: 

```kotlin

  // User opts out of notifications/tracking
  if (userOptsOut()) {
      Pushwoosh.getInstance().stopServerCommunication();
  }

  // GDPR compliance: stop communication until user consents
  if (!userHasGivenConsent()) {
      Pushwoosh.getInstance().stopServerCommunication();
  }

```

#### See also

| |
|---|
| [startServerCommunication()](start-server-communication.md) |
| [isServerCommunicationAllowed()](is-server-communication-allowed.md) |
