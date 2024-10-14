package com.pushwoosh.internal.utils;

public enum RichMediaType {
    DEFAULT ("Default"),
    MODAL ("Modal");

    private final String type;

    RichMediaType(String type) {
        this.type = type;
    }

    public String getStringType() {
        return type;
    }

    public static RichMediaType fromString(String input) {
        for (RichMediaType richMediaType : RichMediaType.values()) {
            if (richMediaType.getStringType().equalsIgnoreCase(input)) {
                return richMediaType;
            }
        }
        throw new IllegalArgumentException("No enum constant for input: " + input);
    }
}
