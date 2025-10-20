package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

/**
 * Defines width behavior for modal in-app messages.
 * <p>
 * The window width affects the visual impact and integration of modals with your app's layout.
 * Choose based on content type and desired user attention level.
 *
 * @see ModalRichmediaConfig#setWindowWidth(ModalRichMediaWindowWidth)
 */
public enum ModalRichMediaWindowWidth {
    FULL_SCREEN(0),
    WRAP_CONTENT(1);

    private final int code;

    ModalRichMediaWindowWidth(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public int compare(ModalRichMediaWindowWidth source) {
        return Integer.compare(source.code, code);
    }

    public static ModalRichMediaWindowWidth getByCode(int code) {
        for (ModalRichMediaWindowWidth source : ModalRichMediaWindowWidth.values()) {
            if (source.code == code) {
                return source;
            }
        }

        PWLog.error("Unknown code of source: " + code);
        return null;
    }
}