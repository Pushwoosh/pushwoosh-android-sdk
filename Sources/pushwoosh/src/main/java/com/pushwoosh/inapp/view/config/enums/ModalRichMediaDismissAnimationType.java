package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

public enum ModalRichMediaDismissAnimationType {
    FADE_OUT(0),
    SLIDE_UP(1),
    SLIDE_RIGHT(2),
    SLIDE_DOWN(3),
    SLIDE_LEFT(4),
    NONE(5);

    private final int code;

    ModalRichMediaDismissAnimationType(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public int compare(ModalRichMediaDismissAnimationType source) {
        return Integer.compare(source.code, code);
    }

    public static ModalRichMediaDismissAnimationType getByCode(int code) {
        for (ModalRichMediaDismissAnimationType source : ModalRichMediaDismissAnimationType.values()) {
            if (source.code == code) {
                return source;
            }
        }

        PWLog.error("Unknown code of source: " + code);
        return null;
    }

    public static ModalRichMediaDismissAnimationType fromString(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "fade_out": return FADE_OUT;
            case "up": return SLIDE_UP;
            case "right": return SLIDE_RIGHT;
            case "down": return SLIDE_DOWN;
            case "left": return SLIDE_LEFT;
            case "none": return NONE;
            default:
                return null;
        }
    }
}
