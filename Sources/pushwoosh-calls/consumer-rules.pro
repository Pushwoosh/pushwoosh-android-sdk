# Consumer ProGuard rules for pushwoosh-calls.

# PushwooshCallPlugin.kt:106  Class.forName for com.pushwoosh.CALL_EVENT_LISTENER meta-data
-keep class * implements com.pushwoosh.calls.listener.CallEventListener { *; }

# ManifestValidator.java:141  Class.forName lookup of the CallEventListener base interface
# (core module resolves it reflectively because pushwoosh-calls is an optional dependency).
-keep interface com.pushwoosh.calls.listener.CallEventListener { *; }
