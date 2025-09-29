package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

public enum ModalRichMediaViewPosition {
    TOP(0),
    CENTER(1),
    BOTTOM(2),
    FULLSCREEN(3);

    private final int code;

    ModalRichMediaViewPosition(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public int compare(ModalRichMediaViewPosition source){
        return Integer.compare(source.code, code);
    }

    public static ModalRichMediaViewPosition getByCode(int code) {
        for (ModalRichMediaViewPosition source : ModalRichMediaViewPosition.values()) {
            if (source.code == code) {
                return source;
            }
        }

        PWLog.error("Unknown code of source: " + code);
        return null;
    }

    public static ModalRichMediaViewPosition fromString(String value) {
        if (value == null) return null;
        switch (value.toLowerCase()) {
            case "top": return TOP;
            case "center": return CENTER;
            case "bottom": return BOTTOM;
            case "fullscreen": return FULLSCREEN;
            default:
                return null;
        }
    }
}
