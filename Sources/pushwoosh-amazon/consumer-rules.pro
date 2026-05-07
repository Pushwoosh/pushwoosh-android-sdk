# Consumer ProGuard rules for pushwoosh-amazon.

# PushRegistrarHelper.java:27,143  Class.forName(AMAZON_INITIALIZER_CLASS_NAME)
-keep class com.pushwoosh.amazon.AmazonInitializer { *; }

# PushRegistrarHelper.java:28,147  Class.forName(AMAZON_PUSH_REGISTRAR_CLASS_NAME)
-keep class com.pushwoosh.amazon.internal.registrar.AdmRegistrar { *; }

# AmazonUtils.java:33, PushAmazonReceiver.java:50  optional ADM API (Fire OS only)
-dontwarn com.amazon.device.messaging.**
