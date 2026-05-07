# Consumer ProGuard rules for pushwoosh-huawei.

# PushRegistrarHelper.java:31,143  Class.forName(HUAWEI_INITIALIZER_CLASS_NAME)
-keep class com.pushwoosh.huawei.HuaweiInitializer { *; }

# PushRegistrarHelper.java:32,147  Class.forName(HUAWEI_PUSH_REGISTRAR_CLASS_NAME)
-keep class com.pushwoosh.huawei.internal.registrar.HuaweiPushRegistrar { *; }

# HuaweiUtils.java:140 (and others)  optional HMS API (Huawei devices only)
-dontwarn com.huawei.hms.**
-dontwarn com.huawei.agconnect.**
