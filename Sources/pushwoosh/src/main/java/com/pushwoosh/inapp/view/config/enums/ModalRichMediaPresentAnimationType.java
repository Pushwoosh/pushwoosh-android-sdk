package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

/**
 * Defines animation types for presenting modal in-app messages.
 * <p>
 * Presentation animations create visual continuity and draw attention when modals appear.
 * Different animations work better with different modal positions and content types.
 *
 * @see ModalRichmediaConfig#setPresentAnimationType(ModalRichMediaPresentAnimationType)
 */
public enum ModalRichMediaPresentAnimationType {
    /** Smooth fade-in animation, ideal for professional and subtle presentations */
    FADE_IN(0),
    
    /** Drop-down animation from top, ideal for notifications and alerts */
    DROP_DOWN(1),
    
    /** Slide animation from the left side of the screen */
    SLIDE_FROM_LEFT(2),
    
    /** Slide animation upward from the bottom, ideal for bottom-positioned modals */
    SLIDE_UP(3),
    
    /** Slide animation from the right side of the screen */
    SLIDE_FROM_RIGHT(4),
    
    /** No animation - modal appears instantly */
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
