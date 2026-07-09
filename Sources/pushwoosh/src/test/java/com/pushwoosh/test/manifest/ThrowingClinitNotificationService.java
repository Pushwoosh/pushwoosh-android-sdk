package com.pushwoosh.test.manifest;

// Simulates a host-provided class (e.g. a NotificationServiceExtension subclass) whose static
// initializer throws. When AndroidManifestConfig.getClass() resolves this name via the one-arg
// Class.forName (initialize=true), the JVM eagerly runs this <clinit>, wraps the throw in an
// ExceptionInInitializerError, and that Error escapes the catch(ClassNotFoundException |
// NoSuchMethodException) at AndroidManifestConfig.java:252.
public class ThrowingClinitNotificationService {
    static {
        failInStaticInit();
    }

    private static void failInStaticInit() {
        throw new IllegalStateException("static initializer fails on purpose");
    }

    public ThrowingClinitNotificationService() {}
}
