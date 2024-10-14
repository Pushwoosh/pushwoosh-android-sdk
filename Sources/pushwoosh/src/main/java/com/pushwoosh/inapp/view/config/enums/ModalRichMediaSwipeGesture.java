package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

public enum ModalRichMediaSwipeGesture {
    UP(0),
    LEFT(1),
    RIGHT(2),
    DOWN(3),
    NONE(4);

    private final int code;

    ModalRichMediaSwipeGesture(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
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
}
