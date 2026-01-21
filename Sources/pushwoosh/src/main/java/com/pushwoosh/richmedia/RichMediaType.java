package com.pushwoosh.richmedia;

import androidx.annotation.NonNull;

public enum RichMediaType {
    MODAL,
    DEFAULT;

    @NonNull
    public static RichMediaType fromString(String input) {
        for (RichMediaType type : values()) {
            if (type.name().equalsIgnoreCase(input)) {
                return type;
            }
        }
        return DEFAULT;
    }
}
