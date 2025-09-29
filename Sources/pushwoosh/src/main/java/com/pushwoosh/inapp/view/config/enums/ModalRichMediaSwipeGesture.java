package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

public enum ModalRichMediaSwipeGesture {
    UP(0, 1 << 2),
    LEFT(1, 1 << 0),
    RIGHT(2, 1 << 1),
    DOWN(3, 1 << 3),
    NONE(4, 0);

    private final int code;
    private final int bit;

    ModalRichMediaSwipeGesture(int code, int bit) {
        this.code = code;
        this.bit = bit;
    }

    public int getCode() {
        return code;
    }

    public int getBit() {
        return bit;
    }

    public int compare(ModalRichMediaSwipeGesture source){
        return Integer.compare(source.code, code);
    }

    public static ModalRichMediaSwipeGesture getByCode(int code) {
        for (ModalRichMediaSwipeGesture source : ModalRichMediaSwipeGesture.values()) {
            if (source.code == code) {
                return source;
            }
        }

        PWLog.error("Unknown code of source: " + code);
        return null;
    }

    public static ModalRichMediaSwipeGesture fromString(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "up": return UP;
            case "left": return LEFT;
            case "right": return RIGHT;
            case "down": return DOWN;
            case "none": return NONE;
            default:
                return null;
        }
    }
}
