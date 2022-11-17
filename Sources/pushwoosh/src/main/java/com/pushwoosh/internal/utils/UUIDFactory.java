package com.pushwoosh.internal.utils;

import java.util.UUID;

public class UUIDFactory {
    public String createUUID(){
        return UUID.randomUUID().toString();
    }
}
