# Consumer ProGuard rules for pushwoosh-firebase.

# PushRegistrarHelper.java:29,143  Class.forName(FIREBASE_INITIALIZER_CLASS_NAME)
-keep class com.pushwoosh.firebase.FirebaseInitializer { *; }

# PushRegistrarHelper.java:30,147  Class.forName(FIREBASE_PUSH_REGISTRAR_CLASS_NAME)
-keep class com.pushwoosh.firebase.internal.registrar.FcmRegistrar { *; }
