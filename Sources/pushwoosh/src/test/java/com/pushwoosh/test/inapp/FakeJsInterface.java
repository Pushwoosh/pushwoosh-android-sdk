/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction.
 */

package com.pushwoosh.test.inapp;

/**
 * Test fixture used by {@code PushwooshInAppImplTest} to verify the reflection branch of
 * {@code getJavascriptInterfaces()} — must be public with a no-arg constructor so that
 * {@code Class.forName(...).newInstance()} succeeds.
 */
public class FakeJsInterface {
    public FakeJsInterface() {
        // no-op
    }
}
