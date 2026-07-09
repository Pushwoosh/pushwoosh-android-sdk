/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction.
 */

package com.pushwoosh.calls.test;

// Simulates a host-provided CallEventListener subclass named in AndroidManifest.xml via the
// com.pushwoosh.CALL_EVENT_LISTENER meta-data key, whose static initializer throws. When
// PushwooshCallPlugin.resolveCallEventListener() resolves this name with the one-arg Class.forName
// (initialize=true) at PushwooshCallPlugin.kt:106, the JVM eagerly runs this <clinit>, wraps the
// throw in an ExceptionInInitializerError, and that Error escapes the catch(Exception) at
// PushwooshCallPlugin.kt:112 (an Error is not an Exception).
//
// It intentionally does NOT implement CallEventListener: the <clinit> throws at Class.forName (:106),
// before newInstance() (:107) and before the `as? CallEventListener` cast (:107) are ever reached, so
// the interface is immaterial to the crash — the load-time failure is what escapes.
//
// Dedicated to this repro (not shared with any other throwing-clinit fixture) so it never shares
// <clinit> state in one JVM/sandbox: an ExceptionInInitializerError fires only on the first init
// attempt per classloader; a poisoned class re-resolved later throws NoClassDefFoundError instead.
// The throw lives in a helper method because the JLS requires a static {} block to be able to
// complete normally — a bare throw in the block is a compile error.
public class ThrowingClinitCallEventListener {
    static {
        failInStaticInit();
    }

    private static void failInStaticInit() {
        throw new IllegalStateException("call event listener static initializer fails on purpose");
    }

    public ThrowingClinitCallEventListener() {}
}
