# Module pushwoosh-calls

Adds VoIP call support to the Pushwoosh SDK. When Pushwoosh delivers a VoIP push notification,
this module intercepts it, registers the call with the Android Telecom Framework,
and presents the system incoming-call UI. Add this dependency when you need to deliver VoIP calls
via push and integrate them with a calling solution such as WebRTC, Twilio, or Agora. The primary
configuration entry point is [PushwooshCallSettings].

# Package com.pushwoosh.calls

Core classes for VoIP call configuration and data.

Use [PushwooshCallSettings] to request the required `READ_PHONE_NUMBERS` permission,
customize notification channel names and ringtone, and check permission status.

[PushwooshVoIPMessage] carries parsed call data (caller name, call ID, video flag)
delivered to your [CallEventListener] callbacks.

[CallPermissionsCallback] receives the result of a permission request initiated by
[PushwooshCallSettings].

# Package com.pushwoosh.calls.listener

Call lifecycle event callbacks. Implement [CallEventListener] and register it in
`AndroidManifest.xml` via the `com.pushwoosh.CALL_EVENT_LISTENER` meta-data key to receive
events when the user answers, rejects, or disconnects a call, and when the remote party cancels
before the call is answered.
