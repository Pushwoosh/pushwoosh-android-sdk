package com.pushwoosh.test.manifest;

// Simulates a host-provided class named in a push payload's pw_actions[].class (e.g. an Activity
// backing an action button) whose static initializer throws. When Action(JSONObject) resolves this
// name via the one-arg Class.forName (initialize=true) at Action.java:45, the JVM eagerly runs this
// <clinit>, wraps the throw in an ExceptionInInitializerError, and that Error escapes the
// catch(ClassNotFoundException) at Action.java:46 (and the catch(JSONException) in
// PushBundleDataProvider.getActions).
//
// Dedicated to the Action.forName repro so it never shares <clinit> state with
// ThrowingClinitNotificationService (the AndroidManifestConfig repro): a poisoned class re-resolved
// later in the same JVM/sandbox throws NoClassDefFoundError instead of ExceptionInInitializerError,
// so each throwing-clinit fixture must have exactly one initialization trigger.
public class ThrowingClinitActionClass {
    static {
        failInStaticInit();
    }

    private static void failInStaticInit() {
        throw new IllegalStateException("action class static initializer fails on purpose");
    }

    public ThrowingClinitActionClass() {}
}
