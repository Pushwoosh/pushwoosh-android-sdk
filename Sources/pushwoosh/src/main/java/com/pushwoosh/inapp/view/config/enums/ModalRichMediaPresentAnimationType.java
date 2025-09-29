package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

public enum ModalRichMediaPresentAnimationType {
    FADE_IN(0),
    DROP_DOWN(1),
    SLIDE_FROM_LEFT(2),
    SLIDE_UP(3),
    SLIDE_FROM_RIGHT(4),
    NONE(5);

    private final int code;

    ModalRichMediaPresentAnimationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public int compare(ModalRichMediaPresentAnimationType source) {
        return Integer.compare(source.code, code);
    }

    public static ModalRichMediaPresentAnimationType getByCode(int code) {
        for (ModalRichMediaPresentAnimationType source : ModalRichMediaPresentAnimationType.values()) {
            if (source.code == code) {
                return source;
            }
        }

        PWLog.error("Unknown code of source: " + code);
        return null;
    }

    public static ModalRichMediaPresentAnimationType fromString(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "fade_in": return FADE_IN;
            case "down": return DROP_DOWN;
            case "left": return SLIDE_FROM_LEFT;
            case "up": return SLIDE_UP;
            case "right": return SLIDE_FROM_RIGHT;
            case "none": return NONE;
            default:
                return null;
        }
    }
}
