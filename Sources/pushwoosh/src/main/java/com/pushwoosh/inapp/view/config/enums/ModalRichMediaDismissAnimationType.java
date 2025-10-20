package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

/**
 * Defines animation types for dismissing modal in-app messages.
 * <p>
 * Dismissal animations provide visual feedback and smooth transitions when users close modals.
 * Matching dismiss animations with present animations creates cohesive user experiences.
 *
 * @see ModalRichmediaConfig#setDismissAnimationType(ModalRichMediaDismissAnimationType)
 */
public enum ModalRichMediaDismissAnimationType {
    /** Smooth fade-out animation, matches well with FADE_IN presentation */
    FADE_OUT(0),
    
    /** Slide animation upward off-screen, ideal for center or bottom positioned modals */
    SLIDE_UP(1),
    
    /** Slide animation rightward off-screen */
    SLIDE_RIGHT(2),
    
    /** Slide animation downward off-screen, matches well with DROP_DOWN presentation */
    SLIDE_DOWN(3),
    
    /** Slide animation leftward off-screen */
    SLIDE_LEFT(4),
    
    /** No animation - modal disappears instantly */
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
