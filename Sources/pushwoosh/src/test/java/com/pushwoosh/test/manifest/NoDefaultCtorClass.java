package com.pushwoosh.test.manifest;

public class NoDefaultCtorClass {
    private final int value;

    public NoDefaultCtorClass(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
