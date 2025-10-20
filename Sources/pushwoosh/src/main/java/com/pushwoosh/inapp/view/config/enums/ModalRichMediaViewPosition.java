package com.pushwoosh.inapp.view.config.enums;

import com.pushwoosh.internal.utils.PWLog;

/**
 * Defines the positioning of modal in-app messages on the screen.
 * <p>
 * The position affects both the visual placement and the user interaction patterns
 * for modal messages. Each position works best for different types of content and
 * user contexts.
 * <p>
 * <b>Usage Example:</b>
 * <pre>
 * {@code
 * // Top position for notifications and alerts
 * ModalRichmediaConfig alertConfig = new ModalRichmediaConfig()
 *     .setViewPosition(ModalRichMediaViewPosition.TOP)
 *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.DROP_DOWN);
 * 
 * // Center position for important messages and confirmations
 * ModalRichmediaConfig confirmConfig = new ModalRichmediaConfig()
 *     .setViewPosition(ModalRichMediaViewPosition.CENTER)
 *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.FADE_IN);
 * 
 * // Bottom position for non-intrusive content
 * ModalRichmediaConfig tipConfig = new ModalRichmediaConfig()
 *     .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
 *     .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP);
 * 
 * // Fullscreen for rich content and videos
 * ModalRichmediaConfig richConfig = new ModalRichmediaConfig()
 *     .setViewPosition(ModalRichMediaViewPosition.FULLSCREEN)
 *     .setStatusBarCovered(true);
 * }
 * </pre>
 *
 * @see ModalRichmediaConfig#setViewPosition(ModalRichMediaViewPosition)
 */
public enum ModalRichMediaViewPosition {
    /** Modal appears at the top of the screen, ideal for notifications and alerts */
    TOP(0),
    
    /** Modal appears in the center of the screen, ideal for important messages and confirmations */
    CENTER(1),
    
    /** Modal appears at the bottom of the screen, ideal for non-intrusive content and tips */
    BOTTOM(2),
    
    /** Modal covers the entire screen, ideal for rich content, videos, and immersive experiences */
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
