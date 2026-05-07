# Consumer ProGuard rules for pushwoosh-calls.

# PushwooshCallPlugin.kt:106  Class.forName for com.pushwoosh.CALL_EVENT_LISTENER meta-data
-keep class * implements com.pushwoosh.calls.listener.CallEventListener { *; }
