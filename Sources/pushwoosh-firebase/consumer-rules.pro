# Consumer ProGuard rules for pushwoosh-firebase.

# PushwooshInitializer.initFirebaseInXamarinPlugin  Class.forName("com.pushwoosh.firebase.FirebaseInitializer")
-keep class com.pushwoosh.firebase.FirebaseInitializer { *; }

# orphan: kept until the next major, was PushRegistrarHelper reflection
-keep class com.pushwoosh.firebase.internal.registrar.FcmRegistrar { *; }
