# Consumer ProGuard rules for pushwoosh-amazon.

# orphan: kept until the next major, was PushRegistrarHelper reflection
-keep class com.pushwoosh.amazon.AmazonInitializer { *; }

# orphan: kept until the next major, was PushRegistrarHelper reflection
-keep class com.pushwoosh.amazon.internal.registrar.AdmRegistrar { *; }

# AmazonUtils.java:33, PushAmazonReceiver.java:50  optional ADM API (Fire OS only)
-dontwarn com.amazon.device.messaging.**
