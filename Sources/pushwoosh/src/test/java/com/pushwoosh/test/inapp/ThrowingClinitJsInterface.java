/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction.
 */

package com.pushwoosh.test.inapp;

// Simulates a host-provided JS-interface class registered by name via
// InAppManager.registerJavascriptInterface(className, name) whose static initializer throws. When
// PushwooshInAppImpl.getJavascriptInterfaces() resolves this name via the one-arg Class.forName
// (initialize=true) at PushwooshInAppImpl.java:206, the JVM eagerly runs this <clinit>, wraps the
// throw in an ExceptionInInitializerError, and that Error escapes the catch(Exception) at
// PushwooshInAppImpl.java:212 (an Error is not an Exception).
//
// Dedicated to this repro so it never shares <clinit> state with the Action/Manifest throwing-clinit
// fixtures: a poisoned class re-resolved later in the same JVM/sandbox throws NoClassDefFoundError
// instead of ExceptionInInitializerError, so each throwing-clinit fixture must have exactly one
// initialization trigger. The throw lives in a helper method because the JLS requires a static {}
// block to be able to complete normally — a bare throw in the block is a compile error.
public class ThrowingClinitJsInterface {
    static {
        failInStaticInit();
    }

    private static void failInStaticInit() {
        throw new IllegalStateException("js interface static initializer fails on purpose");
    }

    public ThrowingClinitJsInterface() {}
}
